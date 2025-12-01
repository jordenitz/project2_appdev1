import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Survey {

    private Long ownerId;
    private List<Question> questions;
    private Map<Long, UserSurveyAnswer> answers = new HashMap<>();
    private boolean active = false;

    public Survey(Long ownerId, List<Question> questions) {
        this.ownerId = ownerId;
        this.questions = questions;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public Map<Long, UserSurveyAnswer> getAnswers() {
        return answers;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean allMembersAnswered(Set<Long> memberIds) {
        return answers.size() == memberIds.size();
    }

    public void registerAnswer(Long userId, int questionIndex, Integer answerIndex) {
        // אם הסקר לא פעיל – מתעלמים
        if (!active) {
            return;
        }

        // לוקחים את האובייקט של תשובות המשתמש, או יוצרים חדש אם אין
        UserSurveyAnswer userAnswers = answers.get(userId);
        if (userAnswers == null) {
            userAnswers = new UserSurveyAnswer(userId);
            answers.put(userId, userAnswers);
        }

        // רישום הבחירה לשאלה הספציפית
        // UserSurveyAnswer כבר דואג שלא ניתן לענות על אותה שאלה פעמיים
        userAnswers.select(questionIndex, answerIndex);
    }
    /**
     * מחזיר מפת תוצאות:
     * key1 = אינדקס שאלה (questionIndex)
     * value1 = Map: key2 = אינדקס תשובה, value2 = אחוזים (0-100)
     */
    public Map<Integer, Map<Integer, Double>> calculateResultsPercentages() {
        Map<Integer, Map<Integer, Integer>> counts = new HashMap<>();

        // מעבר על כל המשתמשים שענו
        for (UserSurveyAnswer userAnswer : answers.values()) {
            Map<Integer, Integer> perQuestion = userAnswer.getSelectedOptionByQuestionIndex();
            for (Map.Entry<Integer, Integer> e : perQuestion.entrySet()) {
                int qIndex = e.getKey();
                int answerIdx = e.getValue();

                counts.putIfAbsent(qIndex, new HashMap<>());
                Map<Integer, Integer> questionCounts = counts.get(qIndex);
                questionCounts.put(answerIdx, questionCounts.getOrDefault(answerIdx, 0) + 1);
            }
        }

        // המרה לאחוזים
        Map<Integer, Map<Integer, Double>> percentages = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, Integer>> entry : counts.entrySet()) {
            int qIndex = entry.getKey();
            Map<Integer, Integer> questionCounts = entry.getValue();

            int total = 0;
            for (int c : questionCounts.values()) {
                total += c;
            }
            Map<Integer, Double> qPercents = new HashMap<>();
            for (Map.Entry<Integer, Integer> e : questionCounts.entrySet()) {
                int ansIdx = e.getKey();
                int c = e.getValue();
                double percent = total == 0 ? 0.0 : (c * 100.0) / total;
                qPercents.put(ansIdx, percent);
            }
            percentages.put(qIndex, qPercents);
        }

        return percentages;
    }

}
