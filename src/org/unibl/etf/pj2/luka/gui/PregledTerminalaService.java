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

public final class PregledTerminalaService {

    public static final String[] ZAGLAVLJA = {
            "IMO", "Naziv", "Tip", "Registarski broj", "Specifičan atribut", "Služba", "Rotacija"
    };

    private PregledTerminalaService() {
    }

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

    private static String rotacija(Plovilo p) {
        if (p instanceof SluzbenoPlovilo sluzbeno) {
            return sluzbeno.isRotacija() ? "Da" : "Ne";
        }
        return "-";
    }
}
