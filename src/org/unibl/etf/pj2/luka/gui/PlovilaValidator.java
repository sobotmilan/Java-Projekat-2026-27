package org.unibl.etf.pj2.luka.gui;

import org.unibl.etf.pj2.luka.model.classes.KontejnerskiBrod;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Polje;
import org.unibl.etf.pj2.luka.model.classes.PutnickiKruzer;
import org.unibl.etf.pj2.luka.model.classes.Tanker;
import org.unibl.etf.pj2.luka.model.classes.Terminal;

import java.util.ArrayList;
import java.util.List;

public final class PlovilaValidator {

    private PlovilaValidator() {
    }

    public static List<String> validiraj(Luka luka, Plovilo kandidat, String izuzetiImo) {
        List<String> greske = new ArrayList<>();

        String imo = kandidat.getImoBroj();
        if (imo == null || imo.isBlank()) {
            greske.add("IMO broj ne smije biti prazan.");
        } else if (!imoJeSlobodan(luka, imo, izuzetiImo)) {
            greske.add("IMO broj " + imo + " je već u upotrebi.");
        }

        if (kandidat.getFotografija() == null) {
            greske.add("Fotografija je obavezna.");
        }

        if (kandidat instanceof KontejnerskiBrod kb && kb.getKapacitetTEU() <= 0) {
            greske.add("Kapacitet (TEU) mora biti pozitivan.");
        }
        if (kandidat instanceof PutnickiKruzer pk && pk.getBrojPutnika() <= 0) {
            greske.add("Broj putnika mora biti pozitivan.");
        }
        if (kandidat instanceof Tanker t && t.getZapreminaBarel() <= 0) {
            greske.add("Zapremina (barel) mora biti pozitivna.");
        }

        return greske;
    }

    private static boolean imoJeSlobodan(Luka luka, String imo, String izuzetiImo) {
        if (imo.equals(izuzetiImo)) {
            return true;
        }
        for (Terminal t : luka.getTerminali()) {
            for (Polje[] red : t.getMatrica()) {
                for (Polje polje : red) {
                    Plovilo p = polje.getTrenutnoPlovilo();
                    if (p != null && imo.equals(p.getImoBroj())) {
                        return false;
                    }
                }
            }
        }
        return !luka.getEvidencijaUlaska().containsKey(imo);
    }
}
