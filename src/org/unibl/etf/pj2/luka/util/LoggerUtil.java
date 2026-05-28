package org.unibl.etf.pj2.luka.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggerUtil {
    static Logger logger;

    static{
        try {
            logger = Logger.getLogger("LukaLogger");
            FileHandler fh = new FileHandler("error.log", true);
            SimpleFormatter sf = new SimpleFormatter();
            fh.setFormatter(sf);
            logger.addHandler(fh);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void logError(String msg, Throwable t) {
        logger.log(Level.SEVERE, msg, t);
    }
}
