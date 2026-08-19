package org.unibl.etf.pj2.luka.gui;

import org.unibl.etf.pj2.luka.model.classes.Dok;
import org.unibl.etf.pj2.luka.model.classes.KontejnerskiBrod;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.PutnickiKruzer;
import org.unibl.etf.pj2.luka.model.classes.Tanker;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.model.interfaces.Carina;
import org.unibl.etf.pj2.luka.model.interfaces.ObalskaStraza;
import org.unibl.etf.pj2.luka.model.interfaces.SluzbenoPlovilo;
import org.unibl.etf.pj2.luka.model.interfaces.Vatrogasci;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pretvara stanje jednog terminala u redove pogodne za prikaz u tabeli administratorskog prozora.
 *
 * @author Milan Šobot
 * @version 1.0
 * @see Terminal
 */
public final class PregledTerminalaService {

    /** Nazivi kolona tabele, istim redoslijedom kao vrijednosti koje vraća {@link #redovi(Terminal)}. */
    public static final String[] ZAGLAVLJA = {
            "IMO", "Naziv", "Tip", "Registarski broj", "Specifičan atribut", "Služba", "Rotacija"
    };

    private PregledTerminalaService() {
    }

    /**
     * Sakuplja po jedan red za svako plovilo trenutno privezano na doku zadatog terminala.
     *
     * @param terminal Terminal čiji se dokovi pregledaju.
     * @return Lista redova, po jedan niz kolona za svako privezano plovilo.
     */
    public static List<String[]> redovi(Terminal terminal) {
        synchronized (terminal) {
            List<String[]> redovi = new ArrayList<>();
            for (Dok d : terminal.getDokovi()) {
                Plovilo p = d.getLokacija().getTrenutnoPlovilo();
                if (p == null) {
                    continue;
                }
                redovi.add(red(p));
            }
            return redovi;
        }
    }

    /**
     * Pronalazi plovilo privezano na zadatom terminalu prema njegovom IMO broju.
     *
     * @param terminal Terminal koji se pretražuje.
     * @param imoBroj IMO broj traženog plovila.
     * @return Pronađeno plovilo, ili {@code null} ako nijedno privezano plovilo na terminalu nema
     *         zadati IMO broj.
     */
    public static Plovilo pronadjiPlovilo(Terminal terminal, String imoBroj) {
        synchronized (terminal) {
            for (Dok d : terminal.getDokovi()) {
                Plovilo p = d.getLokacija().getTrenutnoPlovilo();
                if (p != null && p.getImoBroj().equals(imoBroj)) {
                    return p;
                }
            }
            return null;
        }
    }

    /**
     * Sastavlja jedan red tabele za zadato plovilo, kolonama u istom redoslijedu kao {@link #ZAGLAVLJA}.
     *
     * @param p Plovilo za koje se red sastavlja.
     * @return Niz tekstualnih vrijednosti kolona.
     */
    private static String[] red(Plovilo p) {
        return new String[]{
                p.getImoBroj(),
                p.getNaziv(),
                tip(p),
                p.getRegistarskiBroj(),
                specificanAtribut(p),
                sluzba(p),
                rotacija(p)
        };
    }

    /**
     * Određuje čitljiv naziv trupa plovila.
     *
     * @param p Plovilo čiji se tip određuje.
     * @return Naziv trupa ("Kontejnerski brod"/"Putnički kruzer"/"Tanker"), ili {@code "?"} ako
     *         trup nije prepoznat.
     */
    private static String tip(Plovilo p) {
        if (p instanceof KontejnerskiBrod) {
            return "Kontejnerski brod";
        }
        if (p instanceof PutnickiKruzer) {
            return "Putnički kruzer";
        }
        if (p instanceof Tanker) {
            return "Tanker";
        }
        return "?";
    }

    /**
     * Formatira vrijednost polja specifičnog za trup plovila (kapacitet, broj putnika ili
     * zapremina) kao čitljiv tekst sa jedinicom mjere.
     *
     * @param p Plovilo čiji se specifičan atribut formatira.
     * @return Formatiran tekst specifičnog atributa, ili {@code "-"} ako trup nije prepoznat.
     */
    private static String specificanAtribut(Plovilo p) {
        if (p instanceof KontejnerskiBrod kb) {
            return kb.getKapacitetTEU() + " TEU";
        }
        if (p instanceof PutnickiKruzer pk) {
            return pk.getBrojPutnika() + " putnika";
        }
        if (p instanceof Tanker t) {
            return String.format(Locale.US, "%.2f barela", t.getZapreminaBarel());
        }
        return "-";
    }

    /**
     * Određuje naziv državne službe kojoj plovilo pripada.
     *
     * @param p Plovilo čija se služba određuje.
     * @return Naziv službe ("Vatrogasci"/"Obalska straža"/"Carina"), ili {@code "-"} ako je u
     *         pitanju obično komercijalno plovilo.
     */
    private static String sluzba(Plovilo p) {
        if (p instanceof Vatrogasci) {
            return "Vatrogasci";
        }
        if (p instanceof ObalskaStraza) {
            return "Obalska straža";
        }
        if (p instanceof Carina) {
            return "Carina";
        }
        return "-";
    }

    /**
     * Određuje tekstualni prikaz stanja rotacije plovila.
     *
     * @param p Plovilo čije se stanje rotacije prikazuje.
     * @return {@code "Da"}/{@code "Ne"} za državna plovila, ili {@code "-"} za obično komercijalno
     *         plovilo koje uopšte nema rotaciju.
     */
    private static String rotacija(Plovilo p) {
        if (p instanceof SluzbenoPlovilo sluzbeno) {
            return sluzbeno.isRotacija() ? "Da" : "Ne";
        }
        return "-";
    }
}
