package org.unibl.etf.pj2.luka.util;

import org.unibl.etf.pj2.luka.model.classes.KontejnerskiBrod;
import org.unibl.etf.pj2.luka.model.classes.KontejnerskiBrodObalskaStraza;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Polje;
import org.unibl.etf.pj2.luka.model.classes.PutnickiKruzer;
import org.unibl.etf.pj2.luka.model.classes.PutnickiKruzerCarina;
import org.unibl.etf.pj2.luka.model.classes.PutnickiKruzerObalskaStraza;
import org.unibl.etf.pj2.luka.model.classes.Tanker;
import org.unibl.etf.pj2.luka.model.classes.TankerCarina;
import org.unibl.etf.pj2.luka.model.classes.TankerObalskaStraza;
import org.unibl.etf.pj2.luka.model.classes.TankerVatrogasci;
import org.unibl.etf.pj2.luka.model.classes.Terminal;

import java.io.File;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class GeneratorPlovila {

    public static final double UDIO_KOMERCIJALNIH = 0.90;

    public static final double UDIO_OBALSKA_STRAZA = 0.50;
    public static final double UDIO_CARINA = 0.25;
    public static final double UDIO_VATROGASCI = 1.0 - UDIO_OBALSKA_STRAZA - UDIO_CARINA;

    private static final int KAPACITET_TEU_MIN = 500;
    private static final int KAPACITET_TEU_RASPON = 19_500;
    private static final int BROJ_PUTNIKA_MIN = 50;
    private static final int BROJ_PUTNIKA_RASPON = 4_950;
    private static final double ZAPREMINA_BAREL_MIN = 10_000.0;
    private static final double ZAPREMINA_BAREL_RASPON = 490_000.0;

    private static final AtomicInteger SLEDECI_IMO = new AtomicInteger(1_000_000);

    private static final String[] IMENA = {
            "Aurora", "Neptun", "Posejdon", "Zora", "Sirena", "Nada",
            "Sloboda", "Bosna", "Vardar", "Jadran", "Drina", "Neretva"
    };


    private static final File FOTOGRAFIJA_PLACEHOLDER = new File("resources/placeholder-fotografija.txt");

    private static final File SPISAK_POTJERA_PLACEHOLDER = new File("resources/spisak-potjera-default.txt");

    private GeneratorPlovila() {
    }

    public static Plovilo generisiSlucajno() {
        return generisiSlucajno(ThreadLocalRandom.current());
    }

    public static Plovilo generisiSlucajno(Random rnd) {
        if (rnd.nextDouble() < UDIO_KOMERCIJALNIH) {
            return generisiKomercijalno(rnd);
        }
        return generisiDrzavno(rnd);
    }

    private static Plovilo generisiKomercijalno(Random rnd) {
        String imo = sledeciImo();
        switch (rnd.nextInt(3)) {
            case 0:
                return new KontejnerskiBrod(sledeciNaziv(rnd), imo, motorZa(imo), registarskiZa(imo),
                        FOTOGRAFIJA_PLACEHOLDER, sledeciKapacitetTEU(rnd));
            case 1:
                return new PutnickiKruzer(sledeciNaziv(rnd), imo, motorZa(imo), registarskiZa(imo),
                        FOTOGRAFIJA_PLACEHOLDER, sledeciBrojPutnika(rnd));
            default:
                return new Tanker(sledeciNaziv(rnd), imo, motorZa(imo), registarskiZa(imo),
                        FOTOGRAFIJA_PLACEHOLDER, sledeciZapreminaBarel(rnd));
        }
    }

    private static Plovilo generisiDrzavno(Random rnd) {
        double izbor = rnd.nextDouble();
        if (izbor < UDIO_OBALSKA_STRAZA) {
            return generisiObalskuStrazu(rnd);
        }
        if (izbor < UDIO_OBALSKA_STRAZA + UDIO_CARINA) {
            return generisiCarinu(rnd);
        }
        return generisiVatrogasce(rnd);
    }

    private static Plovilo generisiObalskuStrazu(Random rnd) {
        String imo = sledeciImo();
        switch (rnd.nextInt(3)) {
            case 0:
                return new KontejnerskiBrodObalskaStraza(sledeciNaziv(rnd), imo, motorZa(imo),
                        registarskiZa(imo), FOTOGRAFIJA_PLACEHOLDER, sledeciKapacitetTEU(rnd),
                        SPISAK_POTJERA_PLACEHOLDER);
            case 1:
                return new PutnickiKruzerObalskaStraza(sledeciNaziv(rnd), imo, motorZa(imo),
                        registarskiZa(imo), FOTOGRAFIJA_PLACEHOLDER, sledeciBrojPutnika(rnd),
                        SPISAK_POTJERA_PLACEHOLDER);
            default:
                return new TankerObalskaStraza(sledeciNaziv(rnd), imo, motorZa(imo),
                        registarskiZa(imo), FOTOGRAFIJA_PLACEHOLDER, sledeciZapreminaBarel(rnd),
                        SPISAK_POTJERA_PLACEHOLDER);
        }
    }

    private static Plovilo generisiCarinu(Random rnd) {
        String imo = sledeciImo();
        if (rnd.nextBoolean()) {
            return new PutnickiKruzerCarina(sledeciNaziv(rnd), imo, motorZa(imo),
                    registarskiZa(imo), FOTOGRAFIJA_PLACEHOLDER, sledeciBrojPutnika(rnd));
        }
        return new TankerCarina(sledeciNaziv(rnd), imo, motorZa(imo),
                registarskiZa(imo), FOTOGRAFIJA_PLACEHOLDER, sledeciZapreminaBarel(rnd));
    }

    private static Plovilo generisiVatrogasce(Random rnd) {
        String imo = sledeciImo();
        return new TankerVatrogasci(sledeciNaziv(rnd), imo, motorZa(imo),
                registarskiZa(imo), FOTOGRAFIJA_PLACEHOLDER, sledeciZapreminaBarel(rnd));
    }


    public static void obezbijediJedinstvenostImoZa(Luka luka) {
        int maxPostojeci = 0;
        for (Terminal t : luka.getTerminali()) {
            for (Polje[] red : t.getMatrica()) {
                for (Polje polje : red) {
                    Plovilo p = polje.getTrenutnoPlovilo();
                    if (p != null) {
                        maxPostojeci = Math.max(maxPostojeci, parsirajImoBezbjedno(p.getImoBroj()));
                    }
                }
            }
        }
        for (String imo : luka.getEvidencijaUlaska().keySet()) {
            maxPostojeci = Math.max(maxPostojeci, parsirajImoBezbjedno(imo));
        }
        if (maxPostojeci > 0) {
            int minimalniSledeci = maxPostojeci + 1;
            SLEDECI_IMO.updateAndGet(trenutni -> Math.max(trenutni, minimalniSledeci));
        }
    }

    private static int parsirajImoBezbjedno(String imo) {
        try {
            return Integer.parseInt(imo);
        } catch (NumberFormatException | NullPointerException e) {
            return 0;
        }
    }

    private static String sledeciImo() {
        return String.valueOf(SLEDECI_IMO.getAndIncrement());
    }

    private static String sledeciNaziv(Random rnd) {
        return IMENA[rnd.nextInt(IMENA.length)];
    }

    private static String motorZa(String imo) {
        return "MOT-" + imo;
    }

    private static String registarskiZa(String imo) {
        return "REG-" + imo;
    }

    private static int sledeciKapacitetTEU(Random rnd) {
        return KAPACITET_TEU_MIN + rnd.nextInt(KAPACITET_TEU_RASPON);
    }

    private static int sledeciBrojPutnika(Random rnd) {
        return BROJ_PUTNIKA_MIN + rnd.nextInt(BROJ_PUTNIKA_RASPON);
    }

    private static double sledeciZapreminaBarel(Random rnd) {
        return ZAPREMINA_BAREL_MIN + rnd.nextDouble() * ZAPREMINA_BAREL_RASPON;
    }

    static void resetujImoBrojacZaTest(int pocetnaVrijednost) {
        SLEDECI_IMO.set(pocetnaVrijednost);
    }
}
