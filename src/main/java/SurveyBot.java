import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;

import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


public class SurveyBot extends TelegramLongPollingBot {

    private final SurveyManager surveyManager;

    public SurveyBot(SurveyManager surveyManager) {
        this.surveyManager = surveyManager;
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println("Update received: " + update);

        // קיבלנו הודעה רגילה
        if (update.hasMessage() && update.getMessage().hasText()) {

            Long userId = update.getMessage().getFrom().getId();
            String name = update.getMessage().getFrom().getFirstName();

            // אם המשתמש חדש — נוסיף אותו לקהילה
            if (surveyManager.addMember(userId, name)) {
                surveyManager.broadcastNewMember(name, userId, this);
                System.out.println("User added: " + name + " (" + userId + ")");
            }
        }
        // קיבלנו תשובה לסקר
        if (update.hasPollAnswer()) {
            handlePollAnswer(update.getPollAnswer());
        }
    }

    private void handleMessage(Message message) {

        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        String name = message.getFrom().getFirstName();
        String text = message.getText().trim();

        if (text.equalsIgnoreCase("/start") ||
                text.equals("היי") ||
                text.equalsIgnoreCase("hi")) {

            boolean isNew = surveyManager.addMember(userId, name);

            if (isNew) {
                sendText(chatId, "ברוך הבא לקהילה!");
                surveyManager.broadcastNewMember(name, userId, this);
            } else {
                sendText(chatId, "אתה כבר בקהילה 👍");
            }

        } else {
            sendText(chatId, "כדי להצטרף לקהילה שלח /start או היי/Hi");
        }
    }

    private void handlePollAnswer(PollAnswer pollAnswer) {
        surveyManager.registerAnswer(pollAnswer, this);
    }

    public void sendText(Long chatId, String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText(text);

        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "yarD7437_bot"; // ← להחליף בשם האמיתי
    }

    @Override
    public String getBotToken() {
        return "8234846959:AAEYYdGtpOafUcNV4TJtc21ypEwy_7ZCbII"; // ← לשים את ה־Token מבוטפאדהר
    }
}

