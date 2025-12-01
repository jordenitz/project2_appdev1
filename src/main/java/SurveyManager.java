import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class SurveyManager {

    private final Map<Long, String> members = new HashMap<>();

    private Survey activeSurvey = null;
    private long endTimeMillis;

    // סקר אחרון שנסגר – בשביל ה-GUI של Swing
    private Survey lastClosedSurvey;

    public boolean addMember(Long userId, String name) {
        if (members.containsKey(userId)) return false;
        members.put(userId, name);
        return true;
    }

    public int getCommunitySize() {
        return members.size();
    }

    public void broadcastNewMember(String name, Long newUserId, SurveyBot bot) {
        String msg = name + " הצטרף לקהילה! יש כעת " + members.size() + " חברים.";

        for (Long uid : members.keySet()) {
            if (!uid.equals(newUserId)) {
                bot.sendText(uid, msg);
            }
        }
    }

    public boolean hasActiveSurvey() {
        return activeSurvey != null && activeSurvey.isActive();
    }

    private void sendSurvey(SurveyBot bot) {
        activeSurvey.setActive(true);
        endTimeMillis = System.currentTimeMillis() + (5 * 60_000);

        for (Long userId : members.keySet()) {
            int qIndex = 0;
            for (Question q : activeSurvey.getQuestions()) {
                sendPoll(userId, q, qIndex, bot);
                qIndex++;
            }
        }

        // סגירת הסקר אחרי 5 דקות
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                closeSurvey();
            }
        }, 5 * 60_000L);
    }

    private void sendPoll(Long userId, Question q, int qIndex, SurveyBot bot) {
        SendPoll poll = new SendPoll();
        poll.setChatId(userId.toString());
        poll.setQuestion(q.getText());

        List<String> options = q.getOptions()
                .stream()
                .map(AnswerOption::getText)
                .toList();

        poll.setOptions(options);

        poll.setIsAnonymous(false);
        // כאן כנראה התכוונת: poll.setAllowMultipleAnswers(false);
        // כרגע השורה poll.getAllowMultipleAnswers(); לא עושה כלום

        try {
            Message message = bot.execute(poll);
            q.setPollId(message.getPoll().getId());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void registerAnswer(PollAnswer pollAnswer, SurveyBot bot) {

        if (activeSurvey == null || !activeSurvey.isActive())
            return;

        Long userId = pollAnswer.getUser().getId();

        for (Question q : activeSurvey.getQuestions()) {
            if (q.getPollId().equals(pollAnswer.getPollId())) {
                activeSurvey.registerAnswer(
                        userId,
                        q.getIndex(),
                        pollAnswer.getOptionIds().get(0)
                );
            }
        }

        if (activeSurvey.allMembersAnswered(members.keySet()))
            closeSurvey();
    }

    // *** גרסה אחת ויחידה של closeSurvey ***
    private void closeSurvey() {
        if (activeSurvey == null || !activeSurvey.isActive()) return;

        activeSurvey.setActive(false);
        lastClosedSurvey = activeSurvey; // כדי שה-GUI יוכל להציג תוצאות
        System.out.println("Survey closed automatically.");
    }

    /**
     * מתחיל סקר חדש אם:
     *  - אין סקר פעיל
     *  - יש לפחות 3 חברים בקהילה
     * @param survey הסקר שנבנה (ידני / GPT)
     * @param delayMinutes כמה דקות לחכות לפני שליחת הסקר (0 = מיידי)
     * @param bot הבוט של טלגרם
     * @return true אם התחיל, false אם לא עמד בתנאים
     */
    public boolean startSurvey(Survey survey, int delayMinutes, SurveyBot bot) {
        if (hasActiveSurvey()) {
            System.out.println("יש כבר סקר פעיל, אי אפשר להתחיל חדש.");
            return false;
        }

        if (members.size() < 3) {
            System.out.println("פחות משלושה חברים בקהילה – אי אפשר ליצור סקר.");
            return false;
        }

        this.activeSurvey = survey;

        if (delayMinutes <= 0) {
            sendSurvey(bot);
        } else {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    sendSurvey(bot);
                }
            }, delayMinutes * 60_000L);
        }

        return true;
    }

    public Survey getLastClosedSurvey() {
        return lastClosedSurvey;
    }
}
