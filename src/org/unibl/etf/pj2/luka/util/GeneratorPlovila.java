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

/**
 * Generiše slučajna plovila za dopunu flote do korisnički zadatog minimuma po terminalu.
 *
 * <p>Komercijalno/državno se bira prvo i nezavisno, tako da je omjer 90/10 tačan po konstrukciji ({@link #UDIO_KOMERCIJALNIH}).
 * Tek unutar državne grane bira se služba,
 * a zatim dozvoljeni tip plovila za tu službu
 * (vatrogasci isključivo tanker, obalska straža kontejnerski/kruzer/tanker, carina kruzer/tanker).
 * Obrnut redoslijeda bi mogao proizvesti nepostojeću kombinaciju
 * (npr. vatrogasni kruzer).</p>
 *
 *
 * <p>IMO brojevi i sufiks naziva se plovilima dodjeljuju preko dijeljenih {@link AtomicInteger}
 * brojača, nezavisno od proslijeđenog {@link Random}-a — ponovljivost seed-a pokriva izbor
 * tipa/službe/trupa i brojčanih atributa (i koje se ime iz {@link #IMENA} izvlači), ne i sami IMO
 * broj ili brojčani sufiks naziva, koji po prirodi moraju biti jedinstveni, ne reproduktivni.
 *
 * @author Milan Šobot
 * @version 1.0
 */
public final class GeneratorPlovila {

    /** Udio komercijalnih plovila u ukupnoj generisanoj floti. Ostatak su državna plovila. */
    public static final double UDIO_KOMERCIJALNIH = 0.90;

    /** Udio obalske straže unutar državnih 10% plovila. */
    public static final double UDIO_OBALSKA_STRAZA = 0.50;

    /** Udio carine unutar državnih 10% plovila. */
    public static final double UDIO_CARINA = 0.25;

    /** Udio vatrogasaca unutar državnih 10% plovila. */
    public static final double UDIO_VATROGASCI = 1.0 - UDIO_OBALSKA_STRAZA - UDIO_CARINA;

    private static final int KAPACITET_TEU_MIN = 500;
    private static final int KAPACITET_TEU_RASPON = 19_500;
    private static final int BROJ_PUTNIKA_MIN = 50;
    private static final int BROJ_PUTNIKA_RASPON = 4_950;
    private static final double ZAPREMINA_BAREL_MIN = 10_000.0;
    private static final double ZAPREMINA_BAREL_RASPON = 490_000.0;

    /** Brojač za dodjelu jedinstvenih IMO brojeva (sedmocifrenih), zajednički za sve niti. */
    private static final AtomicInteger SLJEDECI_IMO = new AtomicInteger(1_000_000);

    /**
     * Brojač dodat nazivu svakog generisanog plovila kako bi nazivi plovila ostali jedinstveni bez obzira na
     * veličinu flote, {@link #IMENA} je namjerno mali, fiksni spisak,
     * pa bi biranje iz njega bez ikakvih sufiksa/prefiksa neizbježno ponavljalo imena već kod
     * dvadesetak plovila (paradoks rođendana). Zajednički za sve niti, isti obrazac kao
     * {@link #SLJEDECI_IMO}.
     */
    private static final AtomicInteger SLJEDECI_NAZIV = new AtomicInteger(1);

    private static final String[] IMENA = {
            "Aurora", "Neptun", "Posejdon", "Zora", "Sirena", "Nada",
            "Sloboda", "Bosna", "Vardar", "Jadran", "Drina", "Neretva"
    };


    /** Podrazumijevana putanja do placeholder fotografije koju dobija svako generisano plovilo, budući da je fotografija obavezno polje. */
    private static final File FOTOGRAFIJA_PLACEHOLDER = new File("resources/placeholder-photo.jpg");

    /** Podrazumijevana putanja do <i>placeholder</i> spiska potjera koju dobija svako plovilo obalske straže, budući da je spisak potjera obavezno polje. */
    private static final File SPISAK_POTJERA_PLACEHOLDER = new File("resources/spisak-potjera-default.txt");

    private GeneratorPlovila() {
    }

    /**
     * Generiše jedno slučajno plovilo koristeći {@link ThreadLocalRandom#current()} kao izvor slučajnosti.
     *
     * @return generisano plovilo slučajnog tipa.
     */
    public static Plovilo generisiSlucajno() {
        return generisiSlucajno(ThreadLocalRandom.current());
    }

    /**
     * Generiše jedno slučajno plovilo koristeći zadati izvor slučajnosti.
     *
     * @param rnd Izvor slučajnosti.
     * @return Novogenerisano plovilo slučajnog tipa.
     */
    public static Plovilo generisiSlucajno(Random rnd) {
        if (rnd.nextDouble() < UDIO_KOMERCIJALNIH) {
            return generisiKomercijalno(rnd);
        }
        return generisiDrzavno(rnd);
    }

    /**
     * Generiše komercijalno plovilo, sa jednakom vjerovatnoćom tipa kontejnerskog
     * broda, putničkog kruzera i tankera. IMO se čita jednom u lokalnu promjenljivu kako bi motor
     * i registarski broj referencirali isti (trenutni), a ne sljedeći, IMO broj.
     *
     * @param rnd Izvor slučajnosti.
     * @return Novo komercijalno plovilo.
     */
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

    /**
     * Bira državnu službu (obalska straža, carina ili vatrogasci) prema udjelima
     * definisanim kao atributi klase, i generiše odgovarajuće plovilo.
     *
     * @param rnd Izvor slučajnosti.
     * @return Novo državno plovilo.
     */
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

    /**
     * Generiše plovilo obalske straže,
     * sa jednakom vjerovatnoćom biraći trup,
     * i sa dodijeljenim spiskom potjera.
     *
     * @param rnd Izvor slučajnosti.
     * @return Novo plovilo obalske straže.
     */
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

    /**
     * Generiše carinsko plovilo,
     * sa jednakom vjerovatnoćom tipa plovila.
     *
     * @param rnd Izvor slučajnosti.
     * @return Novo carinsko plovilo.
     */
    private static Plovilo generisiCarinu(Random rnd) {
        String imo = sledeciImo();
        if (rnd.nextBoolean()) {
            return new PutnickiKruzerCarina(sledeciNaziv(rnd), imo, motorZa(imo),
                    registarskiZa(imo), FOTOGRAFIJA_PLACEHOLDER, sledeciBrojPutnika(rnd));
        }
        return new TankerCarina(sledeciNaziv(rnd), imo, motorZa(imo),
                registarskiZa(imo), FOTOGRAFIJA_PLACEHOLDER, sledeciZapreminaBarel(rnd));
    }

    /**
     * Generiše vatrogasno plovilo (isključivo tanker).
     *
     * @param rnd Izvor slučajnosti.
     * @return Novo vatrogasno plovilo.
     */
    private static Plovilo generisiVatrogasce(Random rnd) {
        String imo = sledeciImo();
        return new TankerVatrogasci(sledeciNaziv(rnd), imo, motorZa(imo),
                registarskiZa(imo), FOTOGRAFIJA_PLACEHOLDER, sledeciZapreminaBarel(rnd));
    }


    /**
     * Pomjera IMO brojač iznad najvišeg IMO broja pronađenog u zatečenoj luci i u matricama
     * terminala (privezana plovila i ona trenutno u kanalu), i u {@link Luka#getEvidencijaUlaska()}
     * (plovila koja su već napustila luku, ali čiji zapis o vremenu ulaska i dalje postoji radi
     * obračuna taksi).
     *
     * <p><b>Mora se pozvati odmah nakon deserijalizacije luke, prije prvog poziva
     * {@link #generisiSlucajno()}/{@link #generisiSlucajno(Random)}</b>, jer sama metoda ne radi
     * ništa dok se eksplicitno ne pozove, a bez ovog redoslijeda je kolizija IMO brojeva sa
     * zatečenom flotom gotovo zagarantovana (brojač uvijek kreće od {@code 1_000_000} pri pokretanju
     * JVM-a).</p>
     *
     * @param luka Luka čije zatečeno stanje (terminali i evidencija ulaska) treba pregledati.
     */
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
            SLJEDECI_IMO.updateAndGet(trenutni -> Math.max(trenutni, minimalniSledeci));
        }
    }

    /**
     * Parsira IMO broj u cijeli broj, vraćajući 0 umjesto bacanja izuzetka za IMO brojeve koji
     * ne odgovaraju numeričkom formatu koji ovaj generator koristi.
     *
     * @param imo IMO broj kao tekst.
     * @return Numerička vrijednost IMO broja, 0 ako parsiranje nije moguće.
     */
    private static int parsirajImoBezbjedno(String imo) {
        try {
            return Integer.parseInt(imo);
        } catch (NumberFormatException | NullPointerException e) {
            return 0;
        }
    }

    /**
     * Dodjeljuje sljedeći jedinstveni, sedmocifreni IMO broj iz dijeljenog atomičnog brojača.
     *
     * @return Sljedeći IMO broj.
     */
    private static String sledeciImo() {
        return String.valueOf(SLJEDECI_IMO.getAndIncrement());
    }

    /**
     * Bira slučajan naziv iz fiksnog spiska imena {@link #IMENA} i dodaje mu sufiks iz
     * {@link #SLJEDECI_NAZIV} (npr. {@code "Aurora-14"}) da naziv ostane jedinstven bez obzira na
     * to koliko se puta isto ime izvuče iz malog spiska.
     *
     * @param rnd Izvor slučajnosti.
     * @return Slučajno odabran, garantovano jedinstven naziv.
     */
    private static String sledeciNaziv(Random rnd) {
        return IMENA[rnd.nextInt(IMENA.length)] + "-" + SLJEDECI_NAZIV.getAndIncrement();
    }

    /**
     * Izvodi serijski broj motora iz IMO broja plovila.
     *
     * @param imo IMO broj plovila.
     * @return Serijski broj motora.
     */
    private static String motorZa(String imo) {
        return "M-" + imo;
    }

    /**
     * Izvodi registarsku oznaku iz IMO broja plovila.
     *
     * @param imo IMO broj plovila.
     * @return Registarska oznaka.
     */
    private static String registarskiZa(String imo) {
        return "BIH-" + imo;
    }

    /**
     * Generiše slučajan kapacitet kontejnerskog broda u opsegu
     * [{@value #KAPACITET_TEU_MIN}, {@value #KAPACITET_TEU_MIN} + {@value #KAPACITET_TEU_RASPON}).
     *
     * @param rnd Izvor slučajnosti.
     * @return Slučajan kapacitet u TEU.
     */
    private static int sledeciKapacitetTEU(Random rnd) {
        return KAPACITET_TEU_MIN + rnd.nextInt(KAPACITET_TEU_RASPON);
    }

    /**
     * Generiše slučajan broj putnika za putnički kruzer u opsegu
     * [{@value #BROJ_PUTNIKA_MIN}, {@value #BROJ_PUTNIKA_MIN} + {@value #BROJ_PUTNIKA_RASPON}).
     *
     * @param rnd Izvor slučajnosti.
     * @return Slučajan broj putnika.
     */
    private static int sledeciBrojPutnika(Random rnd) {
        return BROJ_PUTNIKA_MIN + rnd.nextInt(BROJ_PUTNIKA_RASPON);
    }

    /**
     * Generiše slučajnu zapreminu tankera u barelima, u opsegu
     * [{@value #ZAPREMINA_BAREL_MIN}, {@value #ZAPREMINA_BAREL_MIN} + {@value #ZAPREMINA_BAREL_RASPON}).
     *
     * @param rnd Izvor slučajnosti.
     * @return Slučajna zapremina u barelima.
     */
    private static double sledeciZapreminaBarel(Random rnd) {
        return ZAPREMINA_BAREL_MIN + rnd.nextDouble() * ZAPREMINA_BAREL_RASPON;
    }

    /**
     * Vraća IMO brojač na zadatu početnu vrijednost.
     *
     * @param pocetnaVrijednost Vrijednost na koju se brojač postavlja.
     */
    static void resetujImoBrojacZaTest(int pocetnaVrijednost) {
        SLJEDECI_IMO.set(pocetnaVrijednost);
    }

    /**
     * Vraća brojač sufiksa naziva na zadatu početnu vrijednost.
     *
     * @param pocetnaVrijednost Vrijednost na koju se brojač postavlja.
     */
    static void resetujNazivBrojacZaTest(int pocetnaVrijednost) {
        SLJEDECI_NAZIV.set(pocetnaVrijednost);
    }
}
