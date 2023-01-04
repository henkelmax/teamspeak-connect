package de.maxhenkel.teamspeakconnect.telegram;

import de.maxhenkel.teamspeakconnect.Main;
import de.maxhenkel.teamspeakconnect.database.User;
import de.maxhenkel.teamspeakconnect.discord.TelegramDiscordConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.facilities.filedownloader.TelegramFileDownloader;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.List;

public class TelegramBot extends TelegramLongPollingCommandBot {

    public static final Logger LOGGER = LogManager.getLogger("TelegramBot");

    private final String botToken;
    private TelegramBotsApi bot;

    private TelegramFileDownloader telegramFileDownloader;

    public TelegramBot(String botToken) {
        this.botToken = botToken;
    }

    public void connect() throws TelegramApiException {
        bot = new TelegramBotsApi(DefaultBotSession.class);
        bot.registerBot(this);
        register(new StartCommand());
        register(new ConnectCommand());
        telegramFileDownloader = new TelegramFileDownloader(this::getBotToken);
    }

    @Override
    public String getBotUsername() {
        return "TeamspeakConnectBot";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void processNonCommandUpdate(Update update) {
        try {
            if (!update.hasMessage()) {
                return;
            }
            Message message = update.getMessage();
            Long sender = message.getFrom().getId();

            User user = Main.DATABASE.getUserByTelegramId(sender);
            if (user == null) {
                SendMessage sendMessage = new SendMessage(sender.toString(), "You are not connected to TeamSpeak!\nUse <code>/connect id</code> to connect to TeamSpeak");
                sendMessage.enableHtml(true);
                execute(sendMessage);
                return;
            }

            List<User> clients = Main.TEAMSPEAK_BOT.getClientsInChannel(user.getTeamSpeakId());

            if (clients == null) {
                execute(new SendMessage(sender.toString(), "You are not in a TeamSpeak channel!"));
                return;
            }

            ForwardMessage.ForwardMessageBuilder builder = ForwardMessage.builder();
            builder.fromChatId(message.getChatId());
            builder.messageId(message.getMessageId());
            for (User client : clients) {
                if (client.getTelegramId() != null && client.getTelegramId().equals(sender)) {
                    continue;
                }
                if (client.getTelegramId() != null) {
                    builder.chatId(client.getTelegramId());
                    execute(builder.build());
                }
                //TODO Handle properly
                if (client.getDiscordId() != null && Main.DISCORD_BOT != null && Main.DISCORD_BOT.getApi() != null) {
                    TelegramDiscordConverter.sendDiscord(update, user, client.getDiscordId());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process message", e);
        }
    }

    public TelegramFileDownloader getTelegramFileDownloader() {
        return telegramFileDownloader;
    }

}
