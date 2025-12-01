import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.JsonNode;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GPTSurveyGenerator {

    private static final String BASE_URL = "https://app.seker.live/fm1/";
    private final String id;

    public GPTSurveyGenerator(String id) {
        this.id = id;
    }

    // מנקה היסטוריה בשרת
    public void clearHistory() {
        try {
            Unirest.post(BASE_URL + "clear-history")
                    .header("Content-Type", "application/json")
                    .body("{\"id\":\"" + id + "\"}")
                    .asJson();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // יוצרת שאלות סקר לפי נושא
    public List<Question> generateSurveyQuestions(String topic) {

        clearHistory();

        String text = "Create a survey question about: " + topic +
                ". Return a question + 2-4 answer options.";

        HttpResponse<JsonNode> response;

        try {
            // פנייה אמיתית ל־API שעובד
            response = Unirest.get(BASE_URL + "send-message")
                    .queryString("id", id)
                    .queryString("text", text)
                    .asJson();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        JSONObject obj = response.getBody().getObject();

        String questionText = null;
        List<String> opts = new ArrayList<>();

        // ניסיון 1 — JSON מלא מהשרת
        if (obj.has("question")) {
            questionText = obj.optString("question", null);
        }

        if (obj.has("options")) {
            JSONArray arr = obj.optJSONArray("options");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    opts.add(arr.optString(i));
                }
            }
        }

        // fallback — לפעמים השרת מחזיר טקסט ארוך בתוך "extra"
        if ((questionText == null || opts.isEmpty()) && obj.has("extra")) {

            String extra = obj.optString("extra", "");
            String[] lines = extra.split("\\r?\\n");

            // שורה שמתחילה עם "###" או "סקר"
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("###") || line.startsWith("סקר") || line.startsWith("**")) {
                    questionText = line.replaceAll("[#*]", "").trim();
                    break;
                }
            }

            if (questionText == null && lines.length > 0)
                questionText = lines[0].replaceAll("[#*]", "").trim();

            // חיפוש אופציות (1., 2., -, –)
            for (String line : lines) {
                line = line.trim();
                if (line.matches("^[0-9]+[.)].*")) {
                    opts.add(line.replaceFirst("^[0-9]+[.)]\\s*", ""));
                } else if (line.startsWith("-") || line.startsWith("–")) {
                    opts.add(line.substring(1).trim());
                }
            }
        }

        // fallback — אם אין שאלה
        if (questionText == null || questionText.trim().isEmpty()) {
            questionText = "הסקר מהצ'אט אינו עובד, מה דעתך בנושא כוסברה?";
        }

// fallback — אם אין אופציות
        if (opts.isEmpty()) {
            opts.add("כן");
            opts.add("לא");
            opts.add("אולי");
        }

// לא יותר מ־4 תשובות
        if (opts.size() > 4) {
            opts = opts.subList(0, 4);
        }

        // יצירת רשימת שאלות (תמיד שאלה 1)
        List<Question> list = new ArrayList<>();

        List<AnswerOption> answerOptions = new ArrayList<>();
        for (String s : opts) answerOptions.add(new AnswerOption(s));

        list.add(new Question(0, questionText, answerOptions));

        return list;
    }
}
