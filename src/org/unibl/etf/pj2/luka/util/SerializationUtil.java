package org.unibl.etf.pj2.luka.util;

import org.unibl.etf.pj2.luka.model.classes.Luka;

import java.io.*;

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
     * Serijalizuje stanje luke u {@value #DEFAULT_PATH}. Greška pri upisu se bilježi u log i ne prekida izvršavanje aplikacije.
     *
     * @param luka Trenutno stanje luke koje treba sačuvati.
     */
    public static void serijalizujStanjeLuke(Luka luka) {
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
            return (Luka) ois.readObject();
        }catch(IOException | ClassNotFoundException e) {
            LoggerUtil.logError("Greska prilikom deserijalizacije stanja luke. Pokrece se nova konfiguracija, ", e);
            return null;
        }
    }
}
