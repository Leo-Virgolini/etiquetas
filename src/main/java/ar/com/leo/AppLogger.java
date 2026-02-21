package ar.com.leo;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class AppLogger {

    private static final Logger LOGGER = Logger.getLogger("Etiquetas");

    private AppLogger() {
    }

    public static void info(String message) {
        LOGGER.log(Level.INFO, message);
    }

    public static void warn(String message) {
        LOGGER.log(Level.WARNING, message);
    }

    public static void error(String message, Throwable t) {
        LOGGER.log(Level.SEVERE, message, t);
    }
}
