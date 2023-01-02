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

public class ConnectCommand extends BotCommand {

    public static final Logger LOGGER = LogManager.getLogger("TelegramConnectCommand");

    public ConnectCommand() {
        super("connect", "Connects the bot to a TeamSpeak user");
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] args) {
        try {
            if (!chat.isUserChat()) {
                absSender.execute(new SendMessage(chat.getId().toString(), "This command can only be used in private chats"));
                return;
            }

            de.maxhenkel.teamspeakconnect.database.User databaseUser = Main.DATABASE.getUserByTelegramId(user.getId());

            if (databaseUser != null) {
                absSender.execute(new SendMessage(chat.getId().toString(), "You are already connected to a TeamSpeak user"));
                //TODO Add disconnect command
                SendMessage sendMessage = new SendMessage(chat.getId().toString(), "use <code>/disconnect</code> to disconnect");
                sendMessage.enableHtml(true);
                absSender.execute(sendMessage);
                return;
            }

            if (args.length <= 0) {
                absSender.execute(new SendMessage(chat.getId().toString(), "Please enter your code as the first argument"));
                return;
            }

            int code;
            try {
                code = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                absSender.execute(new SendMessage(chat.getId().toString(), "Invalid code"));
                return;
            }

            de.maxhenkel.teamspeakconnect.database.User dbUser = Main.DATABASE.getUserByAuthCode(code);

            if (dbUser == null) {
                absSender.execute(new SendMessage(chat.getId().toString(), "Unknown code"));
                //TODO Ban system
                return;
            }

            Main.DATABASE.setTelegramId(dbUser.getId(), user.getId());
            Main.DATABASE.removeAuthCode(dbUser.getId());
            absSender.execute(new SendMessage(chat.getId().toString(), "You have successfully linked your TeamSpeak account with Telegram!"));

            LOGGER.info("User {} ({}) linked account", user.getUserName(), dbUser.getId().toHexString());
        } catch (TelegramApiException e) {
            LOGGER.error("Failed to process connect command for '{}'", user.getUserName(), e);
        }
    }

}
