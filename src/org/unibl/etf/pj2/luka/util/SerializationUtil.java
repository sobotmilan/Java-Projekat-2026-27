package org.unibl.etf.pj2.luka.util;

import org.unibl.etf.pj2.luka.model.classes.Luka;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Pomoćna klasa koja sadrži statičke metoda za serijalizaciju i deserijalizaciju stanja luke.
 *
 * <p>omogućava da se cijela konfiguracija terminala i flote sačuva između sesija pokretanja i izvršavanja aplikacije.</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see Luka
 */
public class SerializationUtil {
    /** Putanja do datoteke u koju se serijalizuje stanje luke (i iz koje se deserijalizuje stanje luke), relativna u odnosu na radni direktorijum. Podrazumijevano je 'luka.ser'.*/
    private static final String DEFAULT_PATH = "luka.ser";

    /**
     * Donja granica pauze (F6) ispod koje se {@link Luka#pomjeriEvidencijuZaPauzu(Duration)} ne
     * poziva — sprečava da beznačajna vremenska razlika između uzastopnih save/load poziva (npr.
     * unutar iste test metode, svega par milisekundi) nepotrebno pomjeri evidenciju. Podešljivo
     * (ne {@code final}) po istom obrascu kao ostale D5-stil konstante, radi determinizma testova.
     */
    public static volatile long PRAG_PAUZE_MS = 60_000L;

    /**
     * Serijalizuje stanje luke u {@value #DEFAULT_PATH}, prethodno bilježeći trenutno vrijeme kao
     * {@link Luka#setVrijemeZadnjegCuvanja(LocalDateTime)} (F6) — osnova za isključivanje perioda
     * dok je aplikacija ugašena iz narednog obračuna proteklog vremena boravka. Greška pri upisu
     * se bilježi u log i ne prekida izvršavanje aplikacije.
     *
     * @param luka Trenutno stanje luke koje treba sačuvati.
     */
    public static void serijalizujStanjeLuke(Luka luka) {
        luka.setVrijemeZadnjegCuvanja(LocalDateTime.now());
        try(FileOutputStream fos = new FileOutputStream(DEFAULT_PATH); ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(luka);
        }catch(IOException ioe) {
            LoggerUtil.logError("Greska prilikom serijalizacije stanja luke, ", ioe);
        }
    }

    /**
     * Učitava prethodno serijalizovano stanje luke iz {@value #DEFAULT_PATH}. Nepostojanje fajla se
     * tretira kao prvo (svježe) pokretanje aplikacije, a ne kao greška (odnosno, gubitak podataka ne znači nužno nemogućnost pokretanja/izvršavanja aplikacije).
     * U tom slučaju, vraća se {@code null} i aplikacija se pokreće sa potpuno novom konfiguracijom.
     *
     * @return Učitano stanje luke, ili {@code null} ako fajl ne postoji i/ili se ne može pročitati.
     */
    public static Luka ucitajStanjeLuke() {
        File file = new File(DEFAULT_PATH);
        if(!file.exists()) {
            return null;
        }

        try(FileInputStream fis = new FileInputStream(DEFAULT_PATH); ObjectInputStream ois = new ObjectInputStream(fis)) {
            Luka luka = (Luka) ois.readObject();
            primijeniPauzu(luka);
            return luka;
        }catch(IOException | ClassNotFoundException e) {
            LoggerUtil.logError("Greska prilikom deserijalizacije stanja luke. Pokrece se nova konfiguracija, ", e);
            return null;
        }
    }

    /**
     * Izračunava koliko je vremena prošlo od posljednjeg čuvanja luke (F6) i, ako je razlika bar
     * {@link #PRAG_PAUZE_MS}, pomjera evidenciju ulaska za taj period preko
     * {@link Luka#pomjeriEvidencijuZaPauzu(Duration)}. Bez efekta ako luka nikad nije sačuvana
     * (npr. {@code luka.ser} nastao prije uvođenja ovog polja) — {@link Luka#getVrijemeZadnjegCuvanja()}
     * tada vraća {@code null}. Paket-privatna vidljivost radi direktnog testiranja bez potrebe za
     * stvarnim čekanjem od {@value #PRAG_PAUZE_MS} ms između save i load poziva.
     *
     * @param luka Upravo učitana luka čiju evidenciju treba eventualno pomjeriti.
     */
    static void primijeniPauzu(Luka luka) {
        LocalDateTime zadnjeCuvanje = luka.getVrijemeZadnjegCuvanja();
        if (zadnjeCuvanje == null) {
            return;
        }
        Duration pauza = Duration.between(zadnjeCuvanje, LocalDateTime.now());
        if (pauza.toMillis() < PRAG_PAUZE_MS) {
            return;
        }
        luka.pomjeriEvidencijuZaPauzu(pauza);
    }
}
