package org.unibl.etf.pj2.luka.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public final class PropertiesUtil {

    public static final String DEFAULT_PATH = "luka.properties";
    public static final String KLJUC_BROJ_TERMINALA = "broj.terminala";
    public static final int PODRAZUMIJEVANI_BROJ_TERMINALA = 3;
    public static final int MIN_TERMINALA = 1;
    public static final int MAX_TERMINALA = 20;

    private static Properties kesirano;

    private PropertiesUtil() {
    }

    public static synchronized Properties ucitaj() {
        if (kesirano != null) {
            return kesirano;
        }

        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(DEFAULT_PATH)) {
            p.load(fis);
        } catch (IOException e) {
            LoggerUtil.logError("Nije moguce procitati " + DEFAULT_PATH
                    + ", koriste se podrazumijevane vrijednosti.", e);
        }

        kesirano = p;
        return kesirano;
    }

    public static int getBrojTerminala() {
        String vrijednost = ucitaj().getProperty(KLJUC_BROJ_TERMINALA);

        if (vrijednost == null || vrijednost.trim().isEmpty()) {
            LoggerUtil.logError("Kljuc '" + KLJUC_BROJ_TERMINALA + "' nedostaje u " + DEFAULT_PATH
                            + ", koristi se " + PODRAZUMIJEVANI_BROJ_TERMINALA + ".",
                    new IllegalStateException("Nedostaje kljuc: " + KLJUC_BROJ_TERMINALA));
            return PODRAZUMIJEVANI_BROJ_TERMINALA;
        }

        int broj;
        try {
            broj = Integer.parseInt(vrijednost.trim());
        } catch (NumberFormatException nfe) {
            LoggerUtil.logError("Neispravna vrijednost za '" + KLJUC_BROJ_TERMINALA + "': '" + vrijednost
                    + "', koristi se " + PODRAZUMIJEVANI_BROJ_TERMINALA + ".", nfe);
            return PODRAZUMIJEVANI_BROJ_TERMINALA;
        }

        if (broj < MIN_TERMINALA || broj > MAX_TERMINALA) {
            LoggerUtil.logError("Broj terminala " + broj + " je izvan opsega ["
                            + MIN_TERMINALA + ", " + MAX_TERMINALA + "], koristi se "
                            + PODRAZUMIJEVANI_BROJ_TERMINALA + ".",
                    new IllegalArgumentException("Broj terminala izvan opsega: " + broj));
            return PODRAZUMIJEVANI_BROJ_TERMINALA;
        }

        return broj;
    }

    public static synchronized void resetujKes() {
        kesirano = null;
    }
}
