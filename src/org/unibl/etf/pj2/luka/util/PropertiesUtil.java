package org.unibl.etf.pj2.luka.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Čita konfiguraciju aplikacije iz {@value #DEFAULT_PATH}.
 *
 *
 * <p>Rezultat čitanja se kešira u memoriji jer se properties fajl ne mijenja tokom izvršavanja simulacije.
 * Nedostajući fajl, nedostajući ključ ili neispravna/neočekivana vrijednost ne prekidaju aplikaciju,
 * samo se bilježi upozorenje preko {@link LoggerUtil} klase i koristi se
 * {@value #PODRAZUMIJEVANI_BROJ_TERMINALA}.</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see LoggerUtil
 */
public final class PropertiesUtil {

    /** Putanja do konfiguracione datoteke, relativno na radni direktorijum. */
    public static final String DEFAULT_PATH = "luka.properties";

    /** Ključ pod kojim se u .properties fajlu čuva broj terminala. */
    public static final String KLJUC_BROJ_TERMINALA = "broj.terminala";

    /** Broj terminala koji se koristi ako ključ nedostaje i/ili je neispravan. */
    public static final int PODRAZUMIJEVANI_BROJ_TERMINALA = 3;

    /** Donja granica dozvoljenog broja terminala. Moguće prilagođavati potrebama. */
    public static final int MIN_TERMINALA = 1;

    /** Gornja granica dozvoljenog broja terminala. Moguće prilagođavati potrebama. */
    public static final int MAX_TERMINALA = 20;

    /** Keširan rezultat posljednjeg čitanja fajla, sprječava ponovno čitanje sa diska pri svakom pozivu. */
    private static Properties kesirano;

    private PropertiesUtil() {
    }

    /**
     * Učitava (ili vraća ako je već keširan) sadržaj {@value #DEFAULT_PATH}. Ako fajl ne postoji i/ili se ne
     * može pročitati, vraća prazan {@link Properties} objekat uz upozorenje u logu putem {@link LoggerUtil}, pozivalac
     * time uvijek dobija validan, ne-{@code null} objekat.
     *
     * @return Učitana (ili keširana) svojstva.
     */
    public static synchronized Properties ucitaj() {
        if (kesirano != null) {
            return kesirano;
        }

        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(DEFAULT_PATH)) {
            p.load(fis);
        } catch (java.io.FileNotFoundException fnf) {
            LoggerUtil.logWarning("Fajl " + DEFAULT_PATH
                    + " ne postoji, koriste se podrazumijevane vrijednosti.");
        } catch (IOException e) {
            LoggerUtil.logError("Greska pri citanju " + DEFAULT_PATH
                    + ", koriste se podrazumijevane vrijednosti.", e);
        }

        kesirano = p;
        return kesirano;
    }

    /**
     * Čita broj terminala iz konfiguracije, validirajući pročitanu vrijednost protiv
     * [{@link #MIN_TERMINALA}, {@link #MAX_TERMINALA}]. Pri bilo kakvom odstupanju (ključ
     * nedostaje, nije broj, ili je van opsega) bilježi upozorenje i vraća
     * {@link #PODRAZUMIJEVANI_BROJ_TERMINALA}, umjesto da baci izuzetak i time prekine izvršavanje aplikacije.
     *
     * @return Validan broj terminala za izgradnju nove sesije simulacije, bez obzira na uspješnost učitavanja iz konfiguracije.
     */
    public static int getBrojTerminala() {
        String vrijednost = ucitaj().getProperty(KLJUC_BROJ_TERMINALA);

        if (vrijednost == null || vrijednost.trim().isEmpty()) {
            LoggerUtil.logWarning("Kljuc '" + KLJUC_BROJ_TERMINALA + "' nedostaje u " + DEFAULT_PATH
                    + ", koristi se " + PODRAZUMIJEVANI_BROJ_TERMINALA + ".");
            return PODRAZUMIJEVANI_BROJ_TERMINALA;
        }

        int broj;
        try {
            broj = Integer.parseInt(vrijednost.trim());
        } catch (NumberFormatException nfe) {
            LoggerUtil.logWarning("Neispravna vrijednost za '" + KLJUC_BROJ_TERMINALA + "': '" + vrijednost
                    + "', koristi se " + PODRAZUMIJEVANI_BROJ_TERMINALA + ".");
            return PODRAZUMIJEVANI_BROJ_TERMINALA;
        }

        if (broj < MIN_TERMINALA || broj > MAX_TERMINALA) {
            LoggerUtil.logWarning("Broj terminala " + broj + " je izvan opsega ["
                    + MIN_TERMINALA + ", " + MAX_TERMINALA + "], koristi se "
                    + PODRAZUMIJEVANI_BROJ_TERMINALA + ".");
            return PODRAZUMIJEVANI_BROJ_TERMINALA;
        }

        return broj;
    }

    /**
     * Briše keširan sadržaj, tako da sljedeći poziv {@link #ucitaj()} ponovo čita fajl sa diska.
     * Prvenstveno korisno u testovima koji mijenjaju sadržaj {@value #DEFAULT_PATH} između
     * pokretanja.
     */
    public static synchronized void resetujKes() {
        kesirano = null;
    }
}
