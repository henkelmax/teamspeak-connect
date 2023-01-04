package de.maxhenkel.teamspeakconnect.discord;

import de.maxhenkel.teamspeakconnect.Main;
import de.maxhenkel.teamspeakconnect.database.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.awt.*;
import java.io.InputStream;

public class TelegramDiscordConverter {

    public static final Logger LOGGER = LogManager.getLogger("TelegramDiscordConverter");

    public static void sendDiscord(Update update, User sender, Long userId) {
        Message message = update.getMessage();

        if (message.getDocument() != null) {
            handleDocument(message, sender, message.getDocument(), userId);
        }

        if (message.getPhoto() != null && !message.getPhoto().isEmpty()) {
            handlePhoto(message, sender, message.getPhoto().get(message.getPhoto().size() - 1), userId);
        }

        if (message.getVoice() != null) {
            handleVoice(message, sender, message.getVoice(), userId);
        }

        if (message.getAudio() != null) {
            handleAudio(message, sender, message.getAudio(), userId);
        }

        if (message.getVideoNote() != null) {
            handleVideoNote(message, sender, message.getVideoNote(), userId);
        }

        if (message.getVideo() != null) {
            handleVideo(message, sender, message.getVideo(), userId);
        }

        if (message.getContact() != null) {
            handleContact(message, sender, message.getContact(), userId);
        }

        if (message.getText() != null && !message.getText().isEmpty()) {
            handleText(message, sender, message.getText(), userId);
        }

        if (message.getCaption() != null && !message.getCaption().isEmpty()) {
            handleText(message, sender, message.getCaption(), userId);
        }
    }

    private static void handleContact(Message message, User sender, Contact contact, Long userId) {
        Main.DISCORD_BOT.getApi().getUserById(userId).thenAccept(user -> {
            user.sendMessage(
                    getUsername(message, sender),
                    new EmbedBuilder()
                            .setTitle("%s %s".formatted(contact.getFirstName() == null ? "" : contact.getFirstName(), contact.getLastName() == null ? "" : contact.getLastName()))
                            .setDescription("`%s`".formatted(contact.getPhoneNumber()))
            ).exceptionally(new ExceptionHandler<>());
        }).exceptionally(new ExceptionHandler<>());
    }

    private static void handleVideo(Message message, User sender, Video video, Long userId) {
        sendFile(video.getFileId(), video.getFileName(), message, sender, userId);
    }

    private static void handleVideoNote(Message message, User sender, VideoNote videoNote, Long userId) {
        sendFile(videoNote.getFileId(), "video_note.mp4", message, sender, userId);
    }

    private static void handleAudio(Message message, User sender, Audio audio, Long userId) {
        sendFile(audio.getFileId(), audio.getFileName(), message, sender, userId);
    }

    private static void handleVoice(Message message, User sender, Voice audio, Long userId) {
        sendFile(audio.getFileId(), "voice.ogg", message, sender, userId);
    }

    private static void handlePhoto(Message message, User sender, PhotoSize photoSize, Long userId) {
        sendFile(photoSize.getFileId(), "photo.jpg", message, sender, userId);
    }

    private static void handleDocument(Message message, User sender, Document document, Long userId) {
        sendFile(document.getFileId(), document.getFileName(), message, sender, userId);
    }

    private static void handleText(Message message, User sender, String text, Long userId) {
        Main.DISCORD_BOT.getApi().getUserById(userId).thenAccept(user -> {
            user.sendMessage("%s\n%s".formatted(getUsername(message, sender), text)).exceptionally(new ExceptionHandler<>());
        }).exceptionally(new ExceptionHandler<>());
    }

    private static void sendFile(String fileId, String fileName, Message message, User sender, Long userId) {
        try {
            GetFile getFile = new GetFile();
            getFile.setFileId(fileId);

            File file = Main.TELEGRAM_BOT.execute(getFile);

            if (file.getFileSize() > 6 * 1024 * 1024) {
                Main.DISCORD_BOT.getApi().getUserById(userId).thenAccept(user -> {
                    user.sendMessage(
                            getUsername(message, sender),
                            new EmbedBuilder()
                                    .setTitle("File `%s` is too large".formatted(fileName))
                                    .setDescription("%s sent a file that's too large to send in Discord".formatted(getUsername(message, sender)))
                                    .setColor(Color.RED)
                    ).exceptionally(new ExceptionHandler<>());
                }).exceptionally(new ExceptionHandler<>());
                return;
            }

            InputStream inputStream = Main.TELEGRAM_BOT.getTelegramFileDownloader().downloadFileAsStream(file.getFilePath());

            Main.DISCORD_BOT.getApi().getUserById(userId).thenAccept(dcUser -> {
                dcUser.sendMessage("By %s".formatted(getUsername(message, sender)), inputStream, fileName).exceptionally(new ExceptionHandler<>());
            }).exceptionally(new ExceptionHandler<>());
        } catch (TelegramApiException e) {
            LOGGER.error("Could not download file", e);
        }
    }

    private static String getUsername(Message message, User sender) {
        if (sender.getDiscordId() != null) {
            return "<@%s>".formatted(sender.getDiscordId());
        }
        return "**%s**".formatted(getUsername(message));
    }

    private static String getUsername(Message message) {
        String userName = message.getFrom().getUserName();
        if (userName == null || userName.isEmpty()) {
            return "%s %s".formatted(message.getFrom().getFirstName(), message.getFrom().getLastName() == null ? "" : message.getFrom().getLastName());
        }
        return "@%s".formatted(userName);
    }

}
