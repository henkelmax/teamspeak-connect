package de.maxhenkel.teamspeakconnect;

import io.github.cdimascio.dotenv.Dotenv;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Environment {

    public static final Logger LOGGER = LogManager.getLogger("Environment");

    public static final String TEAMSPEAK_HOST;
    public static final int TEAMSPEAK_QUERY_PORT;
    public static final String TEAMSPEAK_QUERY_USERNAME;
    public static final String TEAMSPEAK_QUERY_PASSWORD;
    public static final int TEAMSPEAK_SERVER_ID;
    public static final String TEAMSPEAK_BOT_USERNAME;
    public static final String TEAMSPEAK_BOT_PASSWORD;
    public static final String TEAMSPEAK_BOT_NAME;
    public static final int TEAMSPEAK_BOT_CHANNEL_ID;
    public static final long AUTH_CODE_LIFETIME;

    public static final String DATABASE_URL;
    public static final String DATABASE_NAME;

    public static final String TELEGRAM_BOT_TOKEN;

    public static final String DISCORD_TOKEN;
    public static final long DISCORD_CHANNEL;

    static {
        LOGGER.info("Loading environment variables");

        try {
            Dotenv dotenv = Dotenv.load();
            TEAMSPEAK_HOST = dotenv.get("TEAMSPEAK_HOST", "localhost");
            TEAMSPEAK_QUERY_PORT = Integer.parseInt(dotenv.get("TEAMSPEAK_QUERY_PORT", "10022"));
            TEAMSPEAK_QUERY_USERNAME = dotenv.get("TEAMSPEAK_QUERY_USERNAME", "serveradmin");
            TEAMSPEAK_QUERY_PASSWORD = dotenv.get("TEAMSPEAK_QUERY_PASSWORD", "password");
            TEAMSPEAK_SERVER_ID = Integer.parseInt(dotenv.get("TEAMSPEAK_SERVER_ID", "1"));
            TEAMSPEAK_BOT_USERNAME = dotenv.get("TEAMSPEAK_BOT_USERNAME", "bot");
            TEAMSPEAK_BOT_PASSWORD = dotenv.get("TEAMSPEAK_BOT_PASSWORD", "password");
            TEAMSPEAK_BOT_NAME = dotenv.get("TEAMSPEAK_BOT_NAME", "TeamspeakConnect");
            TEAMSPEAK_BOT_CHANNEL_ID = Integer.parseInt(dotenv.get("TEAMSPEAK_BOT_CHANNEL_ID", "0"));
            AUTH_CODE_LIFETIME = Long.parseLong(dotenv.get("AUTH_CODE_LIFETIME", String.valueOf(1000L * 60L * 5L)));

            DATABASE_URL = dotenv.get("DATABASE_URL", "localhost:27017");
            DATABASE_NAME = dotenv.get("DATABASE_NAME", "teamspeakconnect");

            TELEGRAM_BOT_TOKEN = dotenv.get("TELEGRAM_BOT_TOKEN", "");

            DISCORD_TOKEN = dotenv.get("DISCORD_TOKEN", "");
            DISCORD_CHANNEL = Long.parseLong(dotenv.get("DISCORD_CHANNEL", "0"));

            LOGGER.info("Loaded environment variables");
        } catch (Exception e) {
            LOGGER.error("Error while loading environment variables", e);
            throw e;
        }
    }

}
