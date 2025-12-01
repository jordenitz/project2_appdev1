import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GPTSurveyGenerator {

    private static final String BASE_URL = "https://app.seker.live/fm1/";
    private final String id; // תעודת זהות כפי שנרשמה בשרת

    public GPTSurveyGenerator(String id) {
        this.id = id;
    }

    public void clearHistory() throws IOException {
        JSONObject body = new JSONObject();
        body.put("id", id);
        String resp = sendPost("clear-history", body.toString());
        checkForErrors(resp, "clear-history");
    }

    public int checkBalance() throws IOException {
        JSONObject body = new JSONObject();
        body.put("id", id);
        String resp = sendPost("check-balance", body.toString());
        checkForErrors(resp, "check-balance");

        // אם השרת מחזיר {"balance": מספר} – זה יתפוס
        try {
            JSONObject json = new JSONObject(resp);
            return json.optInt("balance", -1);
        } catch (Exception e) {
            System.out.println("Failed to parse balance JSON: " + e.getMessage());
            return -1;
        }
    }

    /**
     * מייצר רשימת שאלות + תשובות בעזרת GPT, לפי נושא כללי.
     * מחזיר List<Question> שאפשר להשתמש בו ליצירת Survey.
     */
    public List<Question> generateSurveyQuestions(String topic) throws IOException {
        clearHistory();

        String prompt =
                "Create 1-3 survey questions with 2-4 answer options each. " +
                        "Topic: " + topic + ". " +
                        "Return ONLY JSON in this exact format: " +
                        "{ \"questions\": [ { \"q\": \"question text\", \"options\": [\"opt1\", \"opt2\"] } ] }";

        JSONObject body = new JSONObject();
        body.put("id", id);
        body.put("text", prompt);

        String resp = sendPost("send-message", body.toString());
        checkForErrors(resp, "send-message");

        // בשלב ראשון נדפיס את מה שקיבלנו כדי שתוכל לראות בקונסול
        System.out.println("=== Raw response from send-message ===");
        System.out.println(resp);
        System.out.println("======================================");

        // ייתכן שהשרת מחזיר:
        // 1. את ה-JSON של הסקר כמו שהוא
        // 2. אובייקט אחר שמכיל את הטקסט בשדה כלשהו (למשל "text")
        // ננסה לכסות את שני המצבים בצורה זהירה:

        String textPart = resp;

        // קודם ננסה לראות אם resp הוא JSON עם שדה "text"
        try {
            JSONObject wrapper = new JSONObject(resp);
            if (wrapper.has("text")) {
                textPart = wrapper.getString("text");
            }
        } catch (Exception ignore) {
            // אם resp לא JSON – נמשיך עם הטקסט כמו שהוא
        }

        // עכשיו ננסה למצוא בפנים את החלק שהוא אובייקט JSON שמתחיל ב-{ ומסתיים ב-}
        String jsonString = extractJsonObject(textPart);

        if (jsonString == null) {
            throw new IOException("לא הצלחתי למצוא JSON חוקי בתגובה מ-GPT. תגובה הייתה:\n" + textPart);
        }

        System.out.println("=== Extracted JSON for survey ===");
        System.out.println(jsonString);
        System.out.println("=================================");

        JSONObject json = new JSONObject(jsonString);
        JSONArray arr = json.getJSONArray("questions");

        List<Question> questions = new ArrayList<>();

        int qIndex = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject qObj = arr.getJSONObject(i);
            String qText = qObj.getString("q");
            JSONArray optsArray = qObj.getJSONArray("options");

            List<AnswerOption> options = new ArrayList<>();
            for (int j = 0; j < optsArray.length(); j++) {
                options.add(new AnswerOption(optsArray.getString(j)));
            }

            questions.add(new Question(qIndex++, qText, options));
        }

        return questions;
    }

    /**
     * פונקציה פנימית לשליחת POST לנתיב בשרת
     */
    private String sendPost(String path, String jsonBody) throws IOException {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(bytes);
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }

        String response = sb.toString();
        System.out.println("[" + path + "] HTTP code=" + code + ", body=" + response);

        return response;
    }

    /**
     * בדיקת קודי שגיאה לוגיים של השרת (3000–3005)
     */
    private void checkForErrors(String resp, String fromWhere) throws IOException {
        if (resp == null || resp.isEmpty()) return;

        // אם התגובה מספרית (למשל "3000")
        try {
            int code = Integer.parseInt(resp.trim());
            handleErrorCode(code, fromWhere);
            return;
        } catch (NumberFormatException ignore) {
        }

        // אם זה JSON עם קוד
        try {
            JSONObject json = new JSONObject(resp);
            if (json.has("code")) {
                int code = json.getInt("code");
                handleErrorCode(code, fromWhere);
            }
        } catch (Exception ignore) {
            // לא JSON – נתעלם
        }
    }

    private void handleErrorCode(int code, String fromWhere) throws IOException {
        if (code >= 3000 && code <= 3005) {
            String msg;
            switch (code) {
                case 3000:
                    msg = "לא נשלחה תעודת זהות (id).";
                    break;
                case 3001:
                    msg = "תעודת הזהות לא קיימת במאגר.";
                    break;
                case 3002:
                    msg = "נגמרה מכסת הבקשות לתעודת זהות זו.";
                    break;
                case 3003:
                    msg = "לא נשלח טקסט להודעה.";
                    break;
                case 3005:
                default:
                    msg = "שגיאה כללית מהשרת.";
                    break;
            }
            throw new IOException("שגיאה מהשרת ב-" + fromWhere + ": code=" + code + " (" + msg + ")");
        }
    }

    /**
     * מנסה לחלץ אובייקט JSON מתוך מחרוזת טקסט (החל מה-{ הראשון עד ה-} האחרון)
     */
    private String extractJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start == -1 || end == -1 || end <= start) {
            return null;
        }
        return text.substring(start, end + 1).trim();
    }
}
