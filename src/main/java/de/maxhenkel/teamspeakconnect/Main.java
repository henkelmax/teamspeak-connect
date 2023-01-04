package de.maxhenkel.teamspeakconnect;

import de.maxhenkel.teamspeakconnect.database.Database;
import de.maxhenkel.teamspeakconnect.discord.DiscordBot;
import de.maxhenkel.teamspeakconnect.ratelimit.RateLimiter;
import de.maxhenkel.teamspeakconnect.teamspeak.TeamspeakBot;
import de.maxhenkel.teamspeakconnect.telegram.TelegramBot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    public static final Logger LOGGER = LogManager.getLogger("TeamspeakConnect");

    public static TeamspeakBot TEAMSPEAK_BOT;
    public static Database DATABASE;
    public static TelegramBot TELEGRAM_BOT;
    public static DiscordBot DISCORD_BOT;
    public static ScheduledExecutorService EXECUTOR;
    public static RateLimiter RATE_LIMITER;

    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting TeamspeakConnect");

        LOGGER.info("Starting executor");
        EXECUTOR = Executors.newSingleThreadScheduledExecutor();

        LOGGER.info("Starting rate limiter");
        RATE_LIMITER = new RateLimiter(1000);
        EXECUTOR.scheduleAtFixedRate(RATE_LIMITER::updateRates, 10, 10, TimeUnit.SECONDS);

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

        LOGGER.info("Starting Discord bot");
        DISCORD_BOT = new DiscordBot(Environment.DISCORD_TOKEN);
        DISCORD_BOT.connect();
    }

}
