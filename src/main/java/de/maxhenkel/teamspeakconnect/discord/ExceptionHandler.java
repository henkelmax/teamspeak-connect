package de.maxhenkel.teamspeakconnect.discord;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Function;

public class ExceptionHandler<T> implements Function<Throwable, T> {

    public static final Logger LOGGER = LogManager.getLogger("DiscordExceptionHandler");

    @Override
    public T apply(Throwable e) {
        for (StackTraceElement element : e.getStackTrace()) {
            if (element.getClassName().contains("CompletableFuture")) {
                continue;
            }
            LOGGER.error("{}:{}: {}", element.getClassName(), element.getLineNumber(), e.getMessage());
            break;
        }

        return null;
    }

}
