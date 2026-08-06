package org.unibl.etf.pj2.luka.util;

import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.interfaces.Carina;
import org.unibl.etf.pj2.luka.model.interfaces.ObalskaStraza;
import org.unibl.etf.pj2.luka.model.interfaces.Vatrogasci;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Klasa koja vrši obračun i evidentiranje taksi pri izlasku plovila iz luke.
 *
 * @author Milan Šobot
 * @version 1.0
 */
public class PokretacIzvjestaja {
    /** Putanja do CSV datoteke u koju se evidentiraju obračunate takse, relativno na radni direktorijum. */
    private static final String DEFAULT_PATH;

    static{
        DEFAULT_PATH = "takse.csv";
    }

    /**
     * Obračunava taksu za dato plovilo prema proteklom vremenu boravka prema sljedećem principu:
     * -do 12h po principu "plafona" od 100 KM po započetom satu (najviše 1000 KM),
     * -do 24h analogno (najviše 2000 KM)
     * -preko 24h 2000 KM plus 100 KM po svakom narednom započetom satu
     * Vrijeme se zaokružuje naviše ({@link Math#ceil}) na cijele sate, tj. započeti sat se naplaćuje kao pun (35 min == 1h cjenovno, 1h 1 min == 2h cjenovno).
     * Državna plovila (obalska straža, carina, vatrogasci) ne plaćaju taksu.
     *
     * @param plovilo Plovilo za koje se obračunava taksa.
     * @param vrijemeUlaska Vrijeme ulaska plovila u luku.
     * @param vrijemeIzlaska Vrijeme izlaska plovila iz luke.
     *
     * @return Iznos takse u KM ({@code 0.0} za državna plovila).
     */
    public static double izracunajTaksuZaPlovilo(Plovilo plovilo, LocalDateTime vrijemeUlaska, LocalDateTime vrijemeIzlaska) {
        if(plovilo instanceof ObalskaStraza || plovilo instanceof Carina || plovilo instanceof Vatrogasci) {
            return 0.0;
        }

        long brojSati = (long) Math.ceil(Duration.between(vrijemeUlaska, vrijemeIzlaska).toMinutes() / 60.0);
        if (brojSati < 1) {
            brojSati = 1;
        }

        if(brojSati <= 12) {
            if(brojSati >= 10) {
                return 1000.00;
            }else {
                return brojSati * 100.00;
            }
        } else if (brojSati <= 24) {
            if(brojSati >= 20) {
                return 2000.00;
            } else {
                return brojSati * 100.00;
            }
        } else {
            return 2000.00 + ((brojSati - 24) * 100.00);
        }
    }

    /**
     * Upisuje jedan red u CSV izvještaj taksi, dodajući ga na kraj {@code DEFAULT_PATH}
     * i pišući zaglavlje samo ako fajl još ne postoji ili je prazan. Polja poput IMO broj, naziv i tip
     * plovila se navode preko {@link #escapeCsv(String)}, a iznos se formatira
     * sa {@link Locale#US} da decimalna tačka ne bi postala zarez na lokalizovanim mašinama.
     * Ova metoda je {@code synchronized} jer više niti brodova može istovremeno napuštati luku.
     *
     * @param plovilo Plovilo za koje se evidentira taksa.
     * @param vrijemeUlaska Vrijeme ulaska plovila u luku.
     * @param vrijemeIzlaska Vrijeme izlaska plovila iz luke.
     * @param iznos Obračunati iznos takse.
     */
    public static synchronized void evidentirajUCSV(Plovilo plovilo, LocalDateTime vrijemeUlaska, LocalDateTime vrijemeIzlaska, double iznos) {
        File file = new File(DEFAULT_PATH);
        boolean exists = (file.exists()) && (file.length() > 0);

        try(PrintWriter pw = new PrintWriter(new FileWriter(file, true))) {
            if(!exists) {
                pw.println("IMO Broj,Naziv,Tip,Vrijeme ulaska,Vrijeme izlaska,Iznos");
            }

            pw.printf(Locale.US, "%s,%s,%s,%s,%s,%.2f%n",
                    escapeCsv(plovilo.getImoBroj()),
                    escapeCsv(plovilo.getNaziv()),
                    escapeCsv(plovilo.getClass().getSimpleName()),
                    vrijemeUlaska.toString(),
                    vrijemeIzlaska.toString(),
                    iznos);
        } catch(IOException ioe) {
            LoggerUtil.logError("Greska prilikom evidentiranja takse u CSV, ", ioe);
        }
    }

    /**
     * Priprema jednu vrijednost za upis u CSV: polje se navodi pod navodnicima
     * ako sadrži zarez, navodnik ili novi red, a unutrašnji navodnici se udvajaju.
     *
     * @param vrijednost Vrijednost polja.
     * @return Vrijednost bezbjedna za upis u CSV kao jedna kolona.
     */
    private static String escapeCsv(String vrijednost) {
        if (vrijednost == null) {
            return "";
        }
        boolean trebaNavodnike = vrijednost.contains(",")
                || vrijednost.contains("\"")
                || vrijednost.contains("\n")
                || vrijednost.contains("\r");
        if (!trebaNavodnike) {
            return vrijednost;
        }
        return "\"" + vrijednost.replace("\"", "\"\"") + "\"";
    }
}
