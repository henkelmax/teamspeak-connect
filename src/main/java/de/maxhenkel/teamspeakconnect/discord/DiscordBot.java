package de.maxhenkel.teamspeakconnect.discord;

import de.maxhenkel.teamspeakconnect.Main;
import de.maxhenkel.teamspeakconnect.telegram.DiscordTelegramConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.component.*;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.ButtonClickEvent;
import org.javacord.api.event.interaction.ModalSubmitEvent;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.exception.CannotMessageUserException;
import org.javacord.api.interaction.ButtonInteraction;

import java.awt.*;
import java.io.IOException;
import java.util.List;

public class DiscordBot {

    public static final Logger LOGGER = LogManager.getLogger("DiscordBot");

    private static final String CONNECT_BUTTON = "connect";
    private static final String CONNECT_MODAL = "connect";
    private static final String CONNECT_TEXT_FIELD = "connect";

    private final String discordToken;

    private DiscordApi api;

    public DiscordBot(String discordToken) {
        this.discordToken = discordToken;
    }

    public void connect() {
        api = new DiscordApiBuilder().setToken(discordToken).login().join();
        api.updateActivity(ActivityType.WATCHING, "TeamSpeak");

        api.addButtonClickListener(this::onButtonClick);
        api.addModalSubmitListener(this::onModalSubmit);
        api.addMessageCreateListener(this::onMessage);

        for (Server server : api.getServers()) {
            for (ServerChannel channel : server.getChannels()) {
                if (channel.getName().equals("teamspeak-connect")) {
                    channel.asServerTextChannel().ifPresent(this::initChannel);
                }
            }
        }
    }

    private void initChannel(ServerTextChannel channel) {
        channel.getMessagesAsStream().forEach(message -> {
            if (message.getAuthor().getId() == api.getClientId()) {
                message.delete().exceptionally(new ExceptionHandler<>());
            }
        });
        channel.sendMessage(new EmbedBuilder()
                        .setTitle("Connect with TeamSpeak")
                        .setDescription("""
                                Please press the `Connect` button to connect to TeamSpeak.
                                """)
                        .setColor(Color.GREEN),
                ActionRow.of(new ButtonBuilder().setCustomId(CONNECT_BUTTON).setLabel("Connect").setStyle(ButtonStyle.SUCCESS).build())
        ).exceptionally(new ExceptionHandler<>());
    }

    private void onButtonClick(ButtonClickEvent event) {
        String id = event.getButtonInteraction().getCustomId();
        if (CONNECT_BUTTON.equals(id)) {
            ButtonInteraction buttonInteraction = event.getButtonInteraction();
            buttonInteraction.respondWithModal(CONNECT_MODAL, "Activation code",
                    ActionRow.of(TextInput.create(TextInputStyle.SHORT, CONNECT_TEXT_FIELD, "Please enter your activation code"))
            ).exceptionally(new ExceptionHandler<>());
        }
    }

    public void onModalSubmit(ModalSubmitEvent event) {
        String id = event.getModalInteraction().getCustomId();
        if (CONNECT_MODAL.equals(id)) {
            String value = event.getModalInteraction().getTextInputValueByCustomId(CONNECT_TEXT_FIELD).orElse(null);
            if (value == null) {
                return;
            }

            try {
                int code = Integer.parseInt(value);
                onCode(event, code);
            } catch (NumberFormatException e) {
                sendModalResponse(event, "Please enter a valid number");
            }
        }
    }

    private void onCode(ModalSubmitEvent event, int code) {
        try {
            de.maxhenkel.teamspeakconnect.database.User databaseUser = Main.DATABASE.getUserByDiscordId(event.getModalInteraction().getUser().getId());


            if (databaseUser != null) {
                sendModalResponse(event, "You are already connected to a TeamSpeak user");
                //TODO Add disconnect button
                return;
            }

            de.maxhenkel.teamspeakconnect.database.User dbUser = Main.DATABASE.getUserByAuthCode(code);

            if (dbUser == null) {
                sendModalResponse(event, "Unknown code");
                //TODO Ban system
                return;
            }

            Main.DATABASE.removeAuthCode(dbUser.getId());

            event.getModalInteraction().getUser().sendMessage(":wave:").thenAccept(message -> {
                sendModalResponse(event, "You have successfully linked your TeamSpeak account with Telegram!\nYou will now receive a DM.");
                Main.DATABASE.setDiscordId(dbUser.getId(), event.getModalInteraction().getUser().getId());
                LOGGER.info("User {} ({}) linked account", event.getModalInteraction().getUser().getName(), dbUser.getId().toHexString());
            }).exceptionally(throwable -> {
                if (throwable instanceof CannotMessageUserException || throwable.getCause() instanceof CannotMessageUserException) {
                    sendModalResponse(event, "Failed to send direct message. Please enable direct messages in `User Settings` -> `Privacy & Safety` -> `Allow direct messages from server members`.");
                }
                return null;
            });
        } catch (Exception e) {
            LOGGER.error("Failed to process connect interaction for '{}'", event.getModalInteraction().getUser().getName(), e);
        }
    }

    private void sendModalResponse(ModalSubmitEvent event, String message) {
        event.getModalInteraction().createImmediateResponder().setContent(message).setFlags(MessageFlag.EPHEMERAL).respond().exceptionally(new ExceptionHandler<>());
    }

    private void onMessage(MessageCreateEvent event) {
        if (!event.isPrivateMessage()) {
            return;
        }
        if (event.getMessageAuthor().isYourself()) {
            return;
        }
        if (!event.getMessageAuthor().isRegularUser()) {
            return;
        }
        User user = event.getMessageAuthor().asUser().orElse(null);
        if (user == null) {
            return;
        }
        de.maxhenkel.teamspeakconnect.database.User sender = Main.DATABASE.getUserByDiscordId(user.getId());
        if (sender == null) {
            event.getMessage().reply("You are not connected to TeamSpeak!").exceptionally(new ExceptionHandler<>());
            return;
        }

        if (!Main.RATE_LIMITER.addRate(sender, getMessageCost(event))) {
            event.getMessage().reply("You are rate limited!").exceptionally(new ExceptionHandler<>());
            return;
        }

        List<de.maxhenkel.teamspeakconnect.database.User> clients = Main.TEAMSPEAK_BOT.getClientsInChannel(sender.getTeamSpeakId());

        if (clients == null) {
            event.getMessage().reply("You are not in a TeamSpeak channel!").exceptionally(new ExceptionHandler<>());
            return;
        }

        for (de.maxhenkel.teamspeakconnect.database.User client : clients) {
            if (client.getDiscordId() != null && client.getDiscordId().equals(event.getMessageAuthor().getId())) {
                continue;
            }
            if (client.getTelegramId() != null) {
                DiscordTelegramConverter.sendTelegram(event, sender, client.getTelegramId());
            }

            if (client.getDiscordId() != null) {
                forwardMessage(event, sender, client.getDiscordId());
            }
        }
    }

    private static int getMessageCost(MessageCreateEvent event) {
        int cost = 25;
        cost += event.getMessageAttachments().size() * 75;
        return cost;
    }

    private void forwardMessage(MessageCreateEvent event, de.maxhenkel.teamspeakconnect.database.User sender, Long discordId) {
        api.getUserById(discordId).thenAccept(user -> {
            if (!event.getMessage().getContent().isEmpty()) {
                user.sendMessage("<@%s>\n%s".formatted(event.getMessageAuthor().getId(), event.getMessage().getContent())).exceptionally(new ExceptionHandler<>());
            }
            for (MessageAttachment attachment : event.getMessageAttachments()) {
                try {
                    user.sendMessage("By <@%s>".formatted(event.getMessageAuthor().getId()), attachment.getUrl().openStream(), attachment.getFileName()).exceptionally(new ExceptionHandler<>());
                } catch (IOException e) {
                    LOGGER.error("Failed to open stream", e);
                }
            }
        }).exceptionally(new ExceptionHandler<>());
    }

    public DiscordApi getApi() {
        return api;
    }
}
