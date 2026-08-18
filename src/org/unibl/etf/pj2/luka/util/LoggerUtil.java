package org.unibl.etf.pj2.luka.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Pomoćna klasa koja predstavlja centralizovan logging alat aplikacije preko {@link Logger}-a.
 *
 * <p>Svi izuzeci kao i  upozorenja prolaze kroz ovu klasu i bivaju upisani u datoteku
 * {@value #DEFAULT_PATH}, umjesto da se ispisuju direktno na konzolu pri izvršavanju. Nivo logovanja za {@link Logger} objekat korišten
 * je {@code Level.ALL.}</p>
 *
 * @author Milan Šobot
 * @version 1.0
 */
public class LoggerUtil {

    /** Putanja do datoteke u koju se upisuju svi logovi, relativno na radni direktorijum. */
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

    /**
     * Loguje grešku na nivou {@link Level#SEVERE} zajedno sa pratećim izuzetkom (odnosno njegovim stack trace-om).
     *
     * @param msg Poruka koja opisuje grešku.
     * @param t Izuzetak koji je uzrokovao grešku.
     */
    public static void logError(String msg, Throwable t) {
        logger.log(Level.SEVERE, msg, t);
    }


    /**
     * Loguje grešku na nivou {@link Level#SEVERE} bez pratećeg izuzetka.
     *
     * @param msg Poruka koja opisuje grešku.
     */
    public static void logError(String msg) {
        logger.log(Level.SEVERE, msg);
    }


    /**
     * Loguje upozorenje na nivou {@link Level#WARNING} - za stanja koja nisu nužno greška, ali
     * odstupaju od očekivanog/normalnog toka izvršavanja (npr. neispravna vrijednost u properties fajlu).
     *
     * @param msg Poruka upozorenja.
     */
    public static void logWarning(String msg) {
        logger.log(Level.WARNING, msg);
    }


    /**
     * Loguje informativnu poruku na nivou {@link Level#INFO}.
     *
     * @param msg Informativna poruka koja se loguje.
     */
    public static void logInfo(String msg) {
        logger.log(Level.INFO, msg);
    }
}
