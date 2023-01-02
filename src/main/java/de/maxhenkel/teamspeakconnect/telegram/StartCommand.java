package de.maxhenkel.teamspeakconnect.telegram;

import de.maxhenkel.teamspeakconnect.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class StartCommand extends BotCommand {

    public static final Logger LOGGER = LogManager.getLogger("TelegramStartCommand");

    public StartCommand() {
        super("start", "Starts the bot");
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        try {
            if (!chat.isUserChat()) {
                absSender.execute(new SendMessage(chat.getId().toString(), "This command can only be used in private chats"));
                return;
            }

            de.maxhenkel.teamspeakconnect.database.User databaseUser = Main.DATABASE.getUserByTelegramId(user.getId());

            if (databaseUser != null) {
                absSender.execute(new SendMessage(chat.getId().toString(), "You are already connected to a TeamSpeak user"));
                return;
            }

            SendMessage sendMessage = new SendMessage(
                    chat.getId().toString(),
                    "Please visit the TeamSpeak channel <b>%s</b> and send <code>!connect</code> in the chat\nOnce you have the code, enter <code>/connect &lt;your-code&gt;</code> here.".formatted(Main.TEAMSPEAK_BOT.getBotChannelName())
            );
            sendMessage.enableHtml(true);
            absSender.execute(sendMessage);
            LOGGER.info("User '{}' started the bot", user.getUserName());
        } catch (TelegramApiException e) {
            LOGGER.error("Failed to process start command for '{}'", user.getUserName(), e);
        }
    }

}
