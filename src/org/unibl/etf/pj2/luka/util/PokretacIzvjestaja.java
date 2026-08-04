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

public class PokretacIzvjestaja {
    private static final String DEFAULT_PATH;

    static{
        DEFAULT_PATH = "takse.csv";
    }

    public static double izracunajTaksuZaPlovilo(Plovilo plovilo, LocalDateTime vrijemeUlaska, LocalDateTime vrijemeIzlaska) {
        if(plovilo instanceof ObalskaStraza || plovilo instanceof Carina || plovilo instanceof Vatrogasci) {
            return 0.0;
        }

        long brojSati = Duration.between(vrijemeUlaska, vrijemeIzlaska).toHours();
        if(brojSati == 0) {
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
            LoggerUtil.logError("Greska prilikom evidentiranja takse u CSV-u!", ioe);
        }
    }

    /**
     * Priprema jednu vrijednost za upis u CSV prema RFC 4180: polje se navodi pod navodnicima
     * ako sadrži zarez, navodnik ili novi red, a unutrašnji navodnici se udvajaju.
     *
     * @param vrijednost Sirova vrijednost polja.
     * @return Vrijednost bezbjedna za upis kao jedna CSV kolona.
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
