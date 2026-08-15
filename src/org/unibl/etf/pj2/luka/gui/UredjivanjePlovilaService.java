package org.unibl.etf.pj2.luka.gui;

import org.unibl.etf.pj2.luka.model.classes.Dok;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Terminal;

import java.util.ArrayList;
import java.util.List;

public final class UredjivanjePlovilaService {

    private UredjivanjePlovilaService() {
    }

    public static List<String> dodajPlovilo(Luka luka, Terminal terminal, Plovilo novo) {
        List<String> greske = PlovilaValidator.validiraj(luka, novo, null);
        if (!greske.isEmpty()) {
            return greske;
        }

        Dok dok = terminal.rezervisiSlobodanDok(novo);
        if (dok == null) {
            greske = new ArrayList<>();
            greske.add("Terminal je pun, nema slobodnog veza.");
            return greske;
        }

        synchronized (terminal) {
            dok.getLokacija().setTrenutnoPlovilo(novo);
        }
        terminal.otkaziRezervaciju(dok);
        return List.of();
    }

    public static List<String> izmijeniPlovilo(Luka luka, Terminal terminal, String stariImoBroj, Plovilo azurirano) {
        List<String> greske = PlovilaValidator.validiraj(luka, azurirano, stariImoBroj);
        if (!greske.isEmpty()) {
            return greske;
        }

        synchronized (terminal) {
            for (Dok d : terminal.getDokovi()) {
                Plovilo trenutno = d.getLokacija().getTrenutnoPlovilo();
                if (trenutno != null && trenutno.getImoBroj().equals(stariImoBroj)) {
                    d.getLokacija().setTrenutnoPlovilo(azurirano);
                    return List.of();
                }
            }
        }

        List<String> nijePronadjeno = new ArrayList<>();
        nijePronadjeno.add("Plovilo sa IMO brojem " + stariImoBroj + " nije pronađeno na terminalu.");
        return nijePronadjeno;
    }

    public static boolean obrisiPlovilo(Terminal terminal, String imoBroj) {
        synchronized (terminal) {
            for (Dok d : terminal.getDokovi()) {
                Plovilo trenutno = d.getLokacija().getTrenutnoPlovilo();
                if (trenutno != null && trenutno.getImoBroj().equals(imoBroj)) {
                    d.getLokacija().setTrenutnoPlovilo(null);
                    return true;
                }
            }
        }
        return false;
    }
}
