package org.unibl.etf.pj2.luka.gui;

import org.unibl.etf.pj2.luka.model.classes.KontejnerskiBrod;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Polje;
import org.unibl.etf.pj2.luka.model.classes.PutnickiKruzer;
import org.unibl.etf.pj2.luka.model.classes.Tanker;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.model.interfaces.ObalskaStraza;

import java.util.ArrayList;
import java.util.List;

/**
 * Provjerava ispravnost podataka unesenih u administratorsku formu prije nego što se plovilo doda
 * ili izmijeni.
 *
 * @author Milan Šobot
 * @version 1.0
 */
public final class PlovilaValidator {

    private PlovilaValidator() {
    }

    /**
     * Provjerava kandidata za dodavanje ili izmjenu: obavezna tekstualna polja ne smiju biti
     * prazna, IMO broj mora biti jedinstven u cijeloj luci, fotografija je obavezna, plovila
     * obalske straže moraju imati spisak potjera, a specifično polje trupa (kapacitet, broj
     * putnika ili zapremina) mora biti pozitivno.
     *
     * @param luka Luka protiv čije se cjelokupne flote provjerava jedinstvenost IMO broja.
     * @param kandidat Plovilo čiji se podaci provjeravaju.
     * @param izuzetiImo IMO broj koji se izuzima iz provjere jedinstvenosti (sopstveni IMO
     *                   plovila koje se izmjenjuje, kad se ne mijenja), ili {@code null} pri
     *                   dodavanju novog plovila.
     * @return Lista tekstualnih opisa svih pronađenih grešaka, prazna ako je kandidat ispravan.
     */
    public static List<String> validiraj(Luka luka, Plovilo kandidat, String izuzetiImo) {
        List<String> greske = new ArrayList<>();

        if (kandidat.getNaziv() == null || kandidat.getNaziv().isBlank()) {
            greske.add("Naziv plovila ne smije biti prazan.");
        }
        if (kandidat.getBrojMotora() == null || kandidat.getBrojMotora().isBlank()) {
            greske.add("Broj motora plovila ne smije biti prazan.");
        }
        if (kandidat.getRegistarskiBroj() == null || kandidat.getRegistarskiBroj().isBlank()) {
            greske.add("Registarski broj plovila ne smije biti prazan.");
        }

        String imo = kandidat.getImoBroj();
        if (imo == null || imo.isBlank()) {
            greske.add("IMO broj plovila ne smije biti prazan.");
        } else if (!imoJeSlobodan(luka, imo, izuzetiImo)) {
            greske.add("IMO broj " + imo + " je već u upotrebi.");
        }

        if (kandidat.getFotografija() == null) {
            greske.add("Fotografija plovila je obavezna.");
        }

        if (kandidat instanceof ObalskaStraza os && os.getSpisakPotjera() == null) {
            greske.add("Spisak potjernica je obavezan za plovila obalske straže.");
        }
        if (kandidat instanceof KontejnerskiBrod kb && kb.getKapacitetTEU() <= 0) {
            greske.add("Kapacitet (izrazen u TEU) mora biti pozitivna vrijednost.");
        }
        if (kandidat instanceof PutnickiKruzer pk && pk.getBrojPutnika() <= 0) {
            greske.add("Broj putnika plovila mora biti pozitivan.");
        }
        if (kandidat instanceof Tanker t && t.getZapreminaBarel() <= 0) {
            greske.add("Zapremina (izrazena u barelima) plovila mora biti pozitivna.");
        }

        return greske;
    }

    /**
     * Provjerava da li je zadati IMO broj slobodan u cijeloj luci — obilazi svako polje svakog
     * terminala tražeći plovilo sa istim IMO brojem.
     *
     * @param luka Luka čija se flota pretražuje.
     * @param imo IMO broj čija se slobodnost provjerava.
     * @param izuzetiImo IMO broj koji se izuzima iz provjere (uvijek smatran slobodnim).
     * @return {@code true} ako nijedno plovilo u luci (osim eventualno izuzetog) nema zadati IMO.
     */
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
        return true;
    }
}
