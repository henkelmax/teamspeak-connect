package de.maxhenkel.teamspeakconnect.telegram;

import de.maxhenkel.teamspeakconnect.Main;
import de.maxhenkel.teamspeakconnect.database.User;
import de.maxhenkel.teamspeakconnect.discord.ExceptionHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.event.message.MessageCreateEvent;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class DiscordTelegramConverter {

    public static final Logger LOGGER = LogManager.getLogger("DiscordTelegramConverter");

    public static void sendTelegram(MessageCreateEvent event, User sender, Long chatId) {
        Message message = event.getMessage();
        try {
            List<MessageAttachment> attachments = message.getAttachments();
            if (!attachments.isEmpty()) {
                handleAttachments(message, attachments, sender, chatId);
            }

            if (!message.getContent().isEmpty()) {
                SendMessage sendMessage = new SendMessage(chatId.toString(), "<a href=\"%s\">%s</a>\n%s".formatted(getURL(sender), message.getAuthor().getName(), htmlEscape(message.getContent())));
                sendMessage.enableHtml(true);
                Main.TELEGRAM_BOT.execute(sendMessage);
            }
        } catch (TelegramApiException e) {
            message.reply("Failed to send message to Telegram").exceptionally(new ExceptionHandler<>());
            LOGGER.error("Failed to send message to Telegram", e);
        }
    }

    private static void handleAttachments(Message message, List<MessageAttachment> attachments, User sender, Long chatId) throws TelegramApiException {
        for (MessageAttachment attachment : attachments) {
            if (attachment.getSize() > 6 * 1024 * 1024) {
                SendMessage sendMessage = new SendMessage(
                        chatId.toString(),
                        "<a href=\"%s\">%s</a>\n%s".formatted(
                                getURL(sender),
                                message.getAuthor().getName(),
                                "File <code>%s</code> is too large to be sent to Telegram".formatted(htmlEscape(attachment.getFileName()))
                        ));
                sendMessage.enableHtml(true);
                Main.TELEGRAM_BOT.execute(sendMessage);
                return;
            }
            SendDocument sendDocument = new SendDocument();
            sendDocument.setChatId(chatId);
            InputFile file = new InputFile();
            try {
                file.setMedia(attachment.getUrl().openStream(), attachment.getFileName());
            } catch (IOException e) {
                //TODO Proper error handling
                LOGGER.error("Failed to open stream", e);
                continue;
            }
            sendDocument.setDocument(file);
            String caption = "By %s".formatted(message.getAuthor().getName());
            sendDocument.setCaption(caption);
            sendDocument.setCaptionEntities(Collections.singletonList(
                    MessageEntity.builder().text(caption).offset(3).length(caption.length() - 3).url(getURL(sender)).type("text_link").build()
            ));
            Main.TELEGRAM_BOT.execute(sendDocument);
        }
    }

    private static String htmlEscape(String message) {
        message = message.replace("&", "&amp;");
        message = message.replace("<", "&lt;");
        message = message.replace(">", "&gt;");
        return message;
    }

    private static String getURL(User user) {
        if (user.getTelegramId() != null) {
            return "tg://user?id=%s".formatted(user.getTelegramId());
        }
        if (user.getDiscordId() != null) {
            return "https://discord.com/users/%s".formatted(user.getDiscordId());
        }
        //TODO Handle properly
        return "tg://user?id=-1";
    }

}
