package org.unibl.etf.pj2.luka.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggerUtil {

    public static final String DEFAULT_PATH = "error.log";

    static Logger logger;

    static {
        try {
            logger = Logger.getLogger("LukaLogger");
            logger.setUseParentHandlers(false);

            FileHandler fh = new FileHandler(DEFAULT_PATH, true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            logger.setLevel(Level.ALL);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private LoggerUtil() {
    }

    public static void logError(String msg, Throwable t) {
        logger.log(Level.SEVERE, msg, t);
    }


    public static void logError(String msg) {
        logger.log(Level.SEVERE, msg);
    }


    public static void logWarning(String msg) {
        logger.log(Level.WARNING, msg);
    }


    public static void logInfo(String msg) {
        logger.log(Level.INFO, msg);
    }
}
