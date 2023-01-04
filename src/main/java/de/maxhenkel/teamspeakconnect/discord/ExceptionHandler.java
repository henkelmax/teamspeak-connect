package de.maxhenkel.teamspeakconnect.discord;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.exception.CannotMessageUserException;

import java.util.function.Function;

public class ExceptionHandler<T> implements Function<Throwable, T> {

    public static final Logger LOGGER = LogManager.getLogger("DiscordExceptionHandler");

    @Override
    public T apply(Throwable e) {
        if (e instanceof CannotMessageUserException || e.getCause() instanceof CannotMessageUserException) {
            return null;
        }
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
