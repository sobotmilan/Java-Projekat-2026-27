package org.unibl.etf.pj2.luka.gui;

import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.model.interfaces.SluzbenoPlovilo;
import org.unibl.etf.pj2.luka.simulation.BrodThread;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Sve nesvingovske operacije koje pokreće klijentski prozor tokom žive simulacije: validacija
 * unosa, biranje plovila za odlazak, dodavanje novih plovila i provjera kraja simulacije.
 *
 * @author Milan Šobot
 * @version 1.0
 * @see BrodThread
 */
public final class KlijentskaSimulacijaService {

    /** Najveći dozvoljeni minimalan broj plovila po terminalu, odgovara kapacitetu jednog terminala. */
    public static final int MAX_MINIMUM_PO_TERMINALU = 30;

    /** Udio plovila po terminalu koji se bira za odlazak nakon pokretanja simulacije. */
    public static final double UDIO_ODLASKA = 0.15;

    private KlijentskaSimulacijaService() {
    }

    /**
     * Provjerava da li je uneseni tekst ispravan minimalan broj plovila po terminalu: mora biti
     * cio, pozitivan broj koji ne prelazi {@link #MAX_MINIMUM_PO_TERMINALU}.
     *
     * @param tekst Tekst unesen u polje za minimalan broj plovila.
     * @return Prazna lista ako je unos ispravan, inače lista opisa grešaka.
     */
    public static List<String> validirajMinimum(String tekst) {
        List<String> greske = new ArrayList<>();
        int vrijednost;
        try {
            vrijednost = Integer.parseInt(tekst.trim());
        } catch (NumberFormatException ex) {
            greske.add("Minimalan broj plovila po terminalu mora biti cio broj.");
            return greske;
        }
        if (vrijednost <= 0) {
            greske.add("Minimalan broj plovila po terminalu mora biti pozitivan.");
        } else if (vrijednost > MAX_MINIMUM_PO_TERMINALU) {
            greske.add("Minimalan broj plovila po terminalu ne smije biti veći od "
                    + MAX_MINIMUM_PO_TERMINALU + " (kapacitet terminala).");
        }
        return greske;
    }

    /**
     * Bira {@link #UDIO_ODLASKA} (zaokruženo naviše) plovila sa jednog terminala koja treba da
     * napuste luku nakon pokretanja simulacije.
     *
     * @param naTerminalu Sve niti plovila trenutno privezanih na jednom terminalu.
     * @return Odabrane niti plovila koja treba da napuste luku, prazna lista ako terminal nema
     *         nijedno plovilo.
     */
    // Bira 15% (zaokruženo naviše, bar jedno ako terminal ima plovila) plovila jednog terminala
    // za odlazak — preferira komercijalna plovila, službena bira samo ako komercijalnih nema
    // dovoljno (potrebna su za odziv na incidente).
    public static List<BrodThread> odaberiZaOdlazak(List<BrodThread> naTerminalu) {
        if (naTerminalu.isEmpty()) {
            return List.of();
        }
        int brojZaOdlazak = (int) Math.ceil(naTerminalu.size() * UDIO_ODLASKA);

        List<BrodThread> komercijalna = new ArrayList<>();
        List<BrodThread> sluzbena = new ArrayList<>();
        for (BrodThread bt : naTerminalu) {
            if (bt.getPlovilo() instanceof SluzbenoPlovilo) {
                sluzbena.add(bt);
            } else {
                komercijalna.add(bt);
            }
        }

        List<BrodThread> odabrani = new ArrayList<>();
        for (BrodThread bt : komercijalna) {
            if (odabrani.size() >= brojZaOdlazak) {
                break;
            }
            odabrani.add(bt);
        }
        for (BrodThread bt : sluzbena) {
            if (odabrani.size() >= brojZaOdlazak) {
                break;
            }
            odabrani.add(bt);
        }
        return odabrani;
    }

    /**
     * Provjerava da li luka ima bar jedan slobodan, nerezervisan vez u bilo kojem terminalu.
     *
     * @param luka Luka koja se provjerava.
     * @return {@code true} ako bar jedan terminal ima slobodan vez.
     */
    public static boolean imaSlobodnogVezaBiloGdje(Luka luka) {
        for (Terminal t : luka.getTerminali()) {
            if (t.getBrojRaspolozivihVezova() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Dodaje novo plovilo u luku dok je simulacija u toku, pokrećući za njega pravu nit koja
     * prolazi kroz isti ulazni kanal i istu rezervaciju veza kao svako drugo plovilo.
     *
     * @param luka Luka u koju se plovilo dodaje.
     * @param kandidat Novo plovilo koje treba dodati.
     * @return Prazna lista ako je pokretanje uspjelo, inače lista opisa grešaka (npr. luka je puna).
     */
    // Dodavanje plovila TOKOM simulacije — ide kroz punu navigacionu logiku
    // BrodThread-a (ulazak kroz kanal, rezervacija veza), NIKAD kroz
    // UredjivanjePlovilaService.dodajPlovilo() koji je setup-only i piše direktno u matricu bez
    // rezervacije, što bi se utrkivalo sa živim nitima simulacije.
    public static List<String> dodajTokomSimulacije(Luka luka, Plovilo kandidat) {
        if (!imaSlobodnogVezaBiloGdje(luka)) {
            List<String> greske = new ArrayList<>();
            greske.add("U luci trenutno nema slobodnog veza — plovilo se ne može dodati.");
            return greske;
        }
        // Isti format kao BrodThread.log() ("[naziv] poruka") — namjerno, da se u konzoli vidi
        // da je BAŠ OVAJ zapis pokrenuo ulazak kroz udjiULuku(), za razliku od plovila koje je
        // već bilo dokovano kad je nit napravljena (npr. dodato preko AdminProzor-a prije pokretanja
        // klijenta) — takvo plovilo NIKAD ne loguje "Ušao u terminal"/"Usidren na vezu", jer
        // predokovani konstruktor uopšte ne prolazi kroz udjiULuku().
        System.out.println("[" + kandidat.getNaziv() + "] Dodato tokom simulacije, pokušava ući u luku.");
        BrodThread bt = new BrodThread(kandidat, luka);
        Thread nit = new Thread(bt, "Brod-" + kandidat.getImoBroj());
        nit.setDaemon(true);
        nit.start();
        return List.of();
    }

    /**
     * Pronalazi aktivnu nit plovila sa zadatim IMO brojem.
     *
     * @param luka Luka čiji se registar aktivnih plovila pretražuje.
     * @param imoBroj IMO broj traženog plovila.
     * @return Nit koja upravlja plovilom, ili {@code null} ako trenutno nije aktivno (još nije
     *         pokrenuto ili je već napustilo luku).
     */
    public static BrodThread pronadjiAktivnuNit(Luka luka, String imoBroj) {
        for (BrodThread bt : luka.getAktivnaPlovila()) {
            if (bt.getPlovilo().getImoBroj().equals(imoBroj)) {
                return bt;
            }
        }
        return null;
    }

    /**
     * Provjerava da li je simulacija stigla do kraja: sva plovila označena za odlazak su
     * napustila luku, a sva plovila dodata tokom simulacije su ili privezana ili su njihove niti
     * već završile (bez obzira jesu li se stigle privezati).
     *
     * @param luka Luka čije se stanje provjerava.
     * @param imoZaOdlazak IMO brojevi plovila označenih da napuste luku.
     * @param imoDodataTokomSimulacije IMO brojevi plovila dodatih tokom simulacije.
     * @return {@code true} ako je simulacija završena.
     */
    // Kraj simulacije: sva plovila označena za odlazak (Korak 3) su napustila luku (niti se
    // više ne nalaze u Luka.getAktivnaPlovila()), I sva plovila dodata tokom simulacije (Korak 4)
    // su privezana. Svjesna odluka: plovilo dodato tokom simulacije čija je nit završila BEZ
    // privezivanja (npr. luka se napunila taman prije nego što je stvarno pokušalo ući — uska,
    // prihvaćena trka opisana u ZAHTJEVI.md) se tretira kao "razriješeno", ne kao trajna blokada —
    // inače bi taj rijedak slučaj zauvijek spriječio kraj simulacije.
    public static boolean jeSimulacijaZavrsena(Luka luka, Set<String> imoZaOdlazak,
                                                Set<String> imoDodataTokomSimulacije) {
        for (String imo : imoZaOdlazak) {
            if (pronadjiAktivnuNit(luka, imo) != null) {
                return false;
            }
        }
        for (String imo : imoDodataTokomSimulacije) {
            BrodThread bt = pronadjiAktivnuNit(luka, imo);
            if (bt != null && !bt.isPrivezan()) {
                return false;
            }
        }
        return true;
    }
}
