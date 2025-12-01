import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);

            SurveyManager manager = new SurveyManager();
            SurveyBot bot = new SurveyBot(manager);

            api.registerBot(bot);

            System.out.println("Bot started!");

            // פתיחת ה-GUI של יצירת סקרים
            javax.swing.SwingUtilities.invokeLater(() -> {
                // כאן לשים את ה-id שלך ל-ChatGPT API בשרת app.seker.live
                String gptId = "code";
                SurveyCreatorFrame frame = new SurveyCreatorFrame(manager, bot, gptId);
                frame.setVisible(true);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
