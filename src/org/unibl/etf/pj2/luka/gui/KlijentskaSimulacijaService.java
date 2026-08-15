package org.unibl.etf.pj2.luka.gui;

import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.model.interfaces.SluzbenoPlovilo;
import org.unibl.etf.pj2.luka.simulation.BrodThread;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class KlijentskaSimulacijaService {

    public static final int MAX_MINIMUM_PO_TERMINALU = 30;
    public static final double UDIO_ODLASKA = 0.15;

    private KlijentskaSimulacijaService() {
    }

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

    // Bira 15% (zaokruženo naviše, bar jedno ako terminal ima plovila) plovila jednog terminala
    // za odlazak (C7) — preferira komercijalna plovila, službena bira samo ako komercijalnih nema
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

    public static boolean imaSlobodnogVezaBiloGdje(Luka luka) {
        for (Terminal t : luka.getTerminali()) {
            if (t.getBrojRaspolozivihVezova() > 0) {
                return true;
            }
        }
        return false;
    }

    // Dodavanje plovila TOKOM simulacije (C8/C9) — ide kroz punu navigacionu logiku
    // BrodThread-a (ulazak kroz kanal, rezervacija veza), NIKAD kroz
    // UredjivanjePlovilaService.dodajPlovilo() koji je setup-only i piše direktno u matricu bez
    // rezervacije, što bi se utrkivalo sa živim nitima simulacije.
    public static List<String> dodajTokomSimulacije(Luka luka, Plovilo kandidat) {
        if (!imaSlobodnogVezaBiloGdje(luka)) {
            List<String> greske = new ArrayList<>();
            greske.add("U luci trenutno nema slobodnog veza — plovilo se ne može dodati.");
            return greske;
        }
        BrodThread bt = new BrodThread(kandidat, luka);
        Thread nit = new Thread(bt, "Brod-" + kandidat.getImoBroj());
        nit.setDaemon(true);
        nit.start();
        return List.of();
    }

    public static BrodThread pronadjiAktivnuNit(Luka luka, String imoBroj) {
        for (BrodThread bt : luka.getAktivnaPlovila()) {
            if (bt.getPlovilo().getImoBroj().equals(imoBroj)) {
                return bt;
            }
        }
        return null;
    }

    // Kraj simulacije (E1): sva plovila označena za odlazak (Korak 3) su napustila luku (niti se
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
