package ar.com.leo;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AppLogger {

    private static final Logger LOGGER = Logger.getLogger("Etiquetas");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static volatile Consumer<String> uiLogger;

    private AppLogger() {
    }

    public static void setUiLogger(Consumer<String> logger) {
        uiLogger = logger;
    }

    public static void info(String message) {
        LOGGER.log(Level.INFO, message);
        sendToUi("[" + LocalTime.now().format(TIME_FMT) + "] " + message);
    }

    public static void success(String message) {
        LOGGER.log(Level.INFO, message);
        sendToUi("[" + LocalTime.now().format(TIME_FMT) + "] [OK] " + message);
    }

    public static void warn(String message) {
        LOGGER.log(Level.WARNING, message);
        sendToUi("[" + LocalTime.now().format(TIME_FMT) + "] [WARN] " + message);
    }

    public static void error(String message, Throwable t) {
        LOGGER.log(Level.SEVERE, message, t);
        sendToUi("[" + LocalTime.now().format(TIME_FMT) + "] [ERROR] " + message);
        if (t != null) {
            sendToUi("[" + LocalTime.now().format(TIME_FMT) + "] [ERROR] " + t.getMessage());
        }
    }

    private static void sendToUi(String timestamped) {
        Consumer<String> logger = uiLogger;
        if (logger != null) {
            logger.accept(timestamped);
        }
    }
}
