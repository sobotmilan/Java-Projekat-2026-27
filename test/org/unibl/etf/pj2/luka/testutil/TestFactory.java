package org.unibl.etf.pj2.luka.testutil;

import org.unibl.etf.pj2.luka.model.classes.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pomoćna klasa za kreiranje objekata koji se ponavljaju kroz testove.
 * Nema svoju logiku i ne testira se — služi samo da testovi ostanu čitljivi.
 */
public final class TestFactory {

    /** Fiktivna putanja do fotografije. Fajl ne mora postojati jer se sadržaj nigdje ne čita. */
    public static final File FOTO = new File("test-foto.jpg");

    /** Fiktivna putanja do spiska potjernica. */
    public static final File SPISAK = new File("test-potjere.txt");

    private TestFactory() {
    }

    // ---------- plovila ----------

    public static KontejnerskiBrod kontejnerski(String imo) {
        return new KontejnerskiBrod("Kont-" + imo, imo, "M-" + imo, "REG-" + imo, FOTO, 1500);
    }

    public static PutnickiKruzer kruzer(String imo) {
        return new PutnickiKruzer("Kruzer-" + imo, imo, "M-" + imo, "REG-" + imo, FOTO, 800);
    }

    public static Tanker tanker(String imo) {
        return new Tanker("Tanker-" + imo, imo, "M-" + imo, "REG-" + imo, FOTO, 120000.0);
    }

    public static KontejnerskiBrodObalskaStraza kontejnerskiOS(String imo) {
        return new KontejnerskiBrodObalskaStraza("KontOS-" + imo, imo, "M-" + imo, "REG-" + imo, FOTO, 1500, SPISAK);
    }

    public static PutnickiKruzerObalskaStraza kruzerOS(String imo) {
        return new PutnickiKruzerObalskaStraza("KruzerOS-" + imo, imo, "M-" + imo, "REG-" + imo, FOTO, 800, SPISAK);
    }

    public static TankerObalskaStraza tankerOS(String imo) {
        return new TankerObalskaStraza("TankerOS-" + imo, imo, "M-" + imo, "REG-" + imo, FOTO, 120000.0, SPISAK);
    }

    public static PutnickiKruzerCarina kruzerCarina(String imo) {
        return new PutnickiKruzerCarina("KruzerC-" + imo, imo, "M-" + imo, "REG-" + imo, FOTO, 800);
    }

    public static TankerCarina tankerCarina(String imo) {
        return new TankerCarina("TankerC-" + imo, imo, "M-" + imo, "REG-" + imo, FOTO, 120000.0);
    }

    public static TankerVatrogasci tankerVatrogasci(String imo) {
        return new TankerVatrogasci("TankerV-" + imo, imo, "M-" + imo, "REG-" + imo, FOTO, 120000.0);
    }

    // ---------- luka i terminali ----------

    public static Luka luka(int brojTerminala) {
        List<Terminal> terminali = new ArrayList<>();
        for (int i = 0; i < brojTerminala; i++) {
            terminali.add(new Terminal(i));
        }
        return new Luka(terminali, new HashMap<String, LocalDateTime>());
    }

    public static Map<String, LocalDateTime> praznaEvidencija() {
        return new HashMap<>();
    }

    /** Zauzima svaki dok terminala fiktivnim plovilom — terminal postaje pun. */
    public static void popuniSveDokove(Terminal t) {
        for (Dok d : t.getDokovi()) {
            d.getLokacija().setTrenutnoPlovilo(kontejnerski("FILL-" + t.getIdTerminala() + "-" + d.getOznakaVezova()));
        }
    }

    /** Vraća prvi slobodan dok terminala ili null ako slobodnog nema. */
    public static Dok prviSlobodanDok(Terminal t) {
        for (Dok d : t.getDokovi()) {
            if (d.isSlobodan()) {
                return d;
            }
        }
        return null;
    }
}
