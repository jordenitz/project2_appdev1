import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SurveyCreatorFrame extends JFrame {

    private final SurveyManager surveyManager;
    private final SurveyBot bot;
    private final GPTSurveyGenerator gpt;

    // רדיו-בטן לבחירה בין ידני ל-GPT
    private final JRadioButton manualRadio = new JRadioButton("יצירה ידנית", true);
    private final JRadioButton gptRadio = new JRadioButton("יצירה בעזרת GPT");

    // שדות לשאלות ידניות (3 שאלות × 4 תשובות)
    private final JTextField[][] manualOptions = new JTextField[3][4];
    private final JTextField[] manualQuestions = new JTextField[3];

    // שדה לנושא GPT
    private final JTextField topicField = new JTextField();

    // עיכוב בדקות
    private final JTextField delayField = new JTextField("0");

    // אזור תצוגת תוצאות
    private final JTextArea resultsArea = new JTextArea();

    public SurveyCreatorFrame(SurveyManager surveyManager, SurveyBot bot, String gptId) {
        this.surveyManager = surveyManager;
        this.bot = bot;
        this.gpt = new GPTSurveyGenerator(gptId);

        setTitle("מנהל סקרים");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        initUi();
    }

    private void initUi() {
        JPanel main = new JPanel(new BorderLayout());
        setContentPane(main);

        // בחירת מצב: ידני / GPT
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        ButtonGroup group = new ButtonGroup();
        group.add(manualRadio);
        group.add(gptRadio);
        modePanel.add(manualRadio);
        modePanel.add(gptRadio);

        main.add(modePanel, BorderLayout.NORTH);

        // מרכז: טאב-פאנל – ידני / GPT
        JTabbedPane tabs = new JTabbedPane();

        tabs.add("ידני", createManualPanel());
        tabs.add("GPT", createGptPanel());

        // סינכרון בין הרדיו לטאבים
        manualRadio.addActionListener(e -> tabs.setSelectedIndex(0));
        gptRadio.addActionListener(e -> tabs.setSelectedIndex(1));
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 0) {
                manualRadio.setSelected(true);
            } else {
                gptRadio.setSelected(true);
            }
        });

        main.add(tabs, BorderLayout.CENTER);

        // תחתון: עיכוב + כפתורים + תוצאות
        JPanel bottom = new JPanel(new BorderLayout());

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.add(new JLabel("עיכוב (בדקות):"));
        delayField.setColumns(5);
        controlPanel.add(delayField);

        JButton sendButton = new JButton("צור ושלח סקר");
        JButton showResultsButton = new JButton("הצג תוצאות אחרונות");
        controlPanel.add(sendButton);
        controlPanel.add(showResultsButton);

        bottom.add(controlPanel, BorderLayout.NORTH);

        resultsArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(resultsArea);
        bottom.add(scroll, BorderLayout.CENTER);

        main.add(bottom, BorderLayout.SOUTH);

        // לוגיקה לכפתורים
        sendButton.addActionListener(e -> onCreateAndSendSurvey());
        showResultsButton.addActionListener(e -> onShowResults());
    }

    private JPanel createManualPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1));
        for (int i = 0; i < 3; i++) {
            JPanel qPanel = new JPanel();
            qPanel.setBorder(BorderFactory.createTitledBorder("שאלה " + (i + 1)));
            qPanel.setLayout(new BoxLayout(qPanel, BoxLayout.Y_AXIS));

            manualQuestions[i] = new JTextField();
            qPanel.add(new JLabel("טקסט השאלה:"));
            qPanel.add(manualQuestions[i]);

            JPanel optsPanel = new JPanel(new GridLayout(2, 2));
            for (int j = 0; j < 4; j++) {
                manualOptions[i][j] = new JTextField();
                optsPanel.add(manualOptions[i][j]);
            }
            qPanel.add(new JLabel("תשובות (2–4 לא ריקות):"));
            qPanel.add(optsPanel);

            panel.add(qPanel);
        }
        return panel;
    }

    private JPanel createGptPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel inner = new JPanel(new BorderLayout());
        inner.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inner.add(new JLabel("נושא כללי לסקר (למשל: העדפות טכנולוגיות בקרב מהנדסי תוכנה):"), BorderLayout.NORTH);
        inner.add(topicField, BorderLayout.CENTER);
        panel.add(inner, BorderLayout.NORTH);
        return panel;
    }

    private void onCreateAndSendSurvey() {
        try {
            int delayMinutes = Integer.parseInt(delayField.getText().trim());
            if (delayMinutes < 0) delayMinutes = 0;

            if (surveyManager.getCommunitySize() < 3) {
                JOptionPane.showMessageDialog(this,
                        "יש פחות מ-3 חברים בקהילה – אי אפשר ליצור סקר.",
                        "שגיאה",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            List<Question> questions;
            if (manualRadio.isSelected()) {
                questions = buildManualQuestions();
            } else {
                questions = buildGptQuestions();
            }

            if (questions.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "לא נמצאו שאלות חוקיות (1–3 שאלות, לכל שאלה 2–4 תשובות).",
                        "שגיאה",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            Survey survey = new Survey(0L, questions); // ownerId – אפשר לשים 0 או ת״ז הטלגרם של היוזם אם יש לך

            boolean ok = surveyManager.startSurvey(survey, delayMinutes, bot);
            if (!ok) {
                JOptionPane.showMessageDialog(this,
                        "יש כבר סקר פעיל או שהקהילה קטנה מדי.",
                        "שגיאה",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "הסקר יישלח " + (delayMinutes == 0 ? "מיידית" : "בעוד " + delayMinutes + " דקות") + ".",
                        "הצלחה",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "עיכוב בדקות חייב להיות מספר שלם.",
                    "שגיאה",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "אירעה שגיאה ביצירת הסקר: " + ex.getMessage(),
                    "שגיאה",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<Question> buildManualQuestions() {
        List<Question> questions = new ArrayList<>();
        int qIndex = 0;

        for (int i = 0; i < 3; i++) {
            String qText = manualQuestions[i].getText().trim();
            if (qText.isEmpty()) continue;

            List<AnswerOption> opts = new ArrayList<>();
            for (int j = 0; j < 4; j++) {
                String optText = manualOptions[i][j].getText().trim();
                if (!optText.isEmpty()) {
                    opts.add(new AnswerOption(optText));
                }
            }
            if (opts.size() >= 2 && opts.size() <= 4) {
                questions.add(new Question(qIndex++, qText, opts));
            }
        }

        return questions;
    }

    private List<Question> buildGptQuestions() throws Exception {
        String topic = topicField.getText().trim();
        if (topic.isEmpty()) {
            throw new IllegalArgumentException("חייבים למלא נושא כללי עבור GPT.");
        }
        return gpt.generateSurveyQuestions(topic);
    }

    private void onShowResults() {
        Survey last = surveyManager.getLastClosedSurvey();
        if (last == null) {
            resultsArea.setText("לא קיים סקר שנסגר עדיין.");
            return;
        }

        Map<Integer, Map<Integer, Double>> percents = last.calculateResultsPercentages();
        StringBuilder sb = new StringBuilder();

        for (Question q : last.getQuestions()) {
            int qIndex = q.getIndex();
            sb.append("שאלה ").append(qIndex + 1).append(": ").append(q.getText()).append("\n");

            Map<Integer, Double> qPercents = percents.get(qIndex);
            if (qPercents == null) {
                sb.append("  אין מספיק תשובות.\n\n");
                continue;
            }

            // מיון תשובות לפי אחוזים (מהגבוה לנמוך)
            List<Integer> indices = new ArrayList<>(qPercents.keySet());
            indices.sort((a, b) -> Double.compare(qPercents.get(b), qPercents.get(a)));

            List<AnswerOption> options = q.getOptions();
            for (Integer ansIdx : indices) {
                double p = qPercents.get(ansIdx);
                String optText = (ansIdx >= 0 && ansIdx < options.size()) ? options.get(ansIdx).getText() : ("תשובה #" + ansIdx);
                sb.append("  - ").append(optText)
                        .append(" : ")
                        .append(String.format("%.1f%%", p))
                        .append("\n");
            }
            sb.append("\n");
        }

        resultsArea.setText(sb.toString());
    }
}
