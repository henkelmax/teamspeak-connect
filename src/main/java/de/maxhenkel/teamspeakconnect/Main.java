package de.maxhenkel.teamspeakconnect;

import de.maxhenkel.teamspeakconnect.database.Database;
import de.maxhenkel.teamspeakconnect.teamspeak.TeamspeakBot;
import de.maxhenkel.teamspeakconnect.telegram.TelegramBot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Main {

    public static final Logger LOGGER = LogManager.getLogger("TeamspeakConnect");

    public static TeamspeakBot TEAMSPEAK_BOT;
    public static Database DATABASE;
    public static TelegramBot TELEGRAM_BOT;
    public static ScheduledExecutorService EXECUTOR;

    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting TeamspeakConnect");

        LOGGER.info("Starting executor");
        EXECUTOR = Executors.newSingleThreadScheduledExecutor();

        LOGGER.info("Connecting to database");
        DATABASE = new Database();

        LOGGER.info("Starting TeamSpeak bot");
        TEAMSPEAK_BOT = new TeamspeakBot(
                Environment.TEAMSPEAK_HOST,
                Environment.TEAMSPEAK_QUERY_PORT,
                Environment.TEAMSPEAK_QUERY_USERNAME,
                Environment.TEAMSPEAK_QUERY_PASSWORD,
                Environment.TEAMSPEAK_SERVER_ID,
                Environment.TEAMSPEAK_BOT_USERNAME,
                Environment.TEAMSPEAK_BOT_PASSWORD,
                Environment.TEAMSPEAK_BOT_NAME,
                Environment.TEAMSPEAK_BOT_CHANNEL_ID
        );
        TEAMSPEAK_BOT.connect();

        LOGGER.info("Starting Telegram bot");
        TELEGRAM_BOT = new TelegramBot(Environment.TELEGRAM_BOT_TOKEN);
        TELEGRAM_BOT.connect();
    }

}
