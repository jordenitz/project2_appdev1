import java.util.HashMap;
import java.util.Map;

public class UserSurveyAnswer {

    private Long userId;
    private Map<Integer, Integer> selectedOptionByQuestionIndex = new HashMap<>();

    public UserSurveyAnswer(Long userId) {
        this.userId = userId;
    }

    public void select(int questionIndex, int answerIndex) {
        // אם המשתמש כבר ענה – לא משנים
        if (!selectedOptionByQuestionIndex.containsKey(questionIndex)) {
            selectedOptionByQuestionIndex.put(questionIndex, answerIndex);
        }
    }

    public Map<Integer, Integer> getSelectedOptionByQuestionIndex() {
        return selectedOptionByQuestionIndex;
    }
}
