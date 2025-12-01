import java.util.List;

public class Question {

    private int index;
    private String text;
    private List<AnswerOption> options;
    private String pollId;

    public Question(int index, String text, List<AnswerOption> options) {
        this.index = index;
        this.text = text;
        this.options = options;
    }

    public int getIndex() { return index; }
    public String getText() { return text; }
    public List<AnswerOption> getOptions() { return options; }

    public String getPollId() { return pollId; }
    public void setPollId(String pollId) { this.pollId = pollId; }
}
