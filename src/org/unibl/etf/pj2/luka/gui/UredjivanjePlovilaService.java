package org.unibl.etf.pj2.luka.gui;

import org.unibl.etf.pj2.luka.model.classes.Dok;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.model.interfaces.SluzbenoPlovilo;

import java.util.ArrayList;
import java.util.List;

/**
 * Izvršava dodavanje, izmjenu i brisanje plovila na terminalu, pozvano iz administratorskog
 * prozora nad flotom koja trenutno miruje (nije pokrenuta live simulacija).
 *
 * @author Milan Šobot
 * @version 1.0
 * @see PlovilaValidator
 */
public final class UredjivanjePlovilaService {

    private UredjivanjePlovilaService() {
    }

    /**
     * Dodaje novo plovilo na prvi slobodan dok zadatog terminala, nakon uspješne validacije.
     *
     * @param luka Luka kojoj terminal pripada, potrebna validaciji zbog provjere jedinstvenosti IMO broja.
     * @param terminal Terminal na koji se plovilo dodaje.
     * @param novo Novo plovilo koje treba dodati.
     * @return Prazna lista ako je dodavanje uspjelo, inače lista opisa grešaka (neispravan unos
     *         ili nema slobodnog veza).
     */
    // Direktan upis u Polje umjesto Terminal.rezervisiSlobodanDok()/otkaziRezervaciju() —
    // bezbjedno je SAMO zato što admin aplikacija radi dok simulacija ne radi (nema BrodThread-ova
    // koji bi se takmičili za isti vez). Ista pretpostavka kao PokretacSimulacije-ove
    // setup-only metode.
    public static List<String> dodajPlovilo(Luka luka, Terminal terminal, Plovilo novo) {
        List<String> greske = PlovilaValidator.validiraj(luka, novo, null);
        if (!greske.isEmpty()) {
            return greske;
        }

        synchronized (terminal) {
            for (Dok d : terminal.getDokovi()) {
                if (d.isSlobodan()) {
                    luka.getEvidencijaUlaska().remove(novo.getImoBroj());
                    d.getLokacija().setTrenutnoPlovilo(novo);
                    return List.of();
                }
            }
        }

        List<String> puno = new ArrayList<>();
        puno.add("Terminal je pun, nema slobodnog veza.");
        return puno;
    }

    /**
     * Zamjenjuje postojeće plovilo na terminalu ažuriranim podacima, čuvajući postojeću rotaciju
     * (ako je primjenjivo) i brzinu sa starog plovila.
     *
     * @param luka Luka kojoj terminal pripada, potrebna validaciji zbog provjere jedinstvenosti IMO broja.
     * @param terminal Terminal na kojem se plovilo izmjenjuje.
     * @param stariImoBroj IMO broj plovila koje se zamjenjuje, izuzet iz provjere jedinstvenosti.
     * @param azurirano Ažurirano plovilo koje zamjenjuje postojeće.
     * @return Prazna lista ako je izmjena uspjela, inače lista opisa grešaka.
     */
    public static List<String> izmijeniPlovilo(Luka luka, Terminal terminal, String stariImoBroj, Plovilo azurirano) {
        return izmijeniPlovilo(luka, terminal, stariImoBroj, azurirano, false);
    }

    /**
     * Zamjenjuje postojeće plovilo na terminalu ažuriranim podacima.
     *
     * @param luka Luka kojoj terminal pripada, potrebna validaciji zbog provjere jedinstvenosti IMO broja.
     * @param terminal Terminal na kojem se plovilo izmjenjuje.
     * @param stariImoBroj IMO broj plovila koje se zamjenjuje, izuzet iz provjere jedinstvenosti.
     * @param azurirano Ažurirano plovilo koje zamjenjuje postojeće.
     * @param rotacijaEksplicitnoZadata {@code true} ako je rotacija ažuriranog plovila već
     *                                  eksplicitno postavljena (npr. iz checkbox-a na formi) i ne
     *                                  treba je prepisati stanjem sa starog plovila.
     * @return Prazna lista ako je izmjena uspjela, inače lista opisa grešaka.
     */
    // rotacijaEksplicitnoZadata=true preskače prenos rotacije sa starog plovila (ispod) — koristi
    // ga PlovilaFormaDijalog kad je kandidat već dobio rotaciju iz checkbox-a na formi, inače bi
    // prenos odmah prepisao tu vrijednost starom i checkbox ne bi imao efekta pri izmjeni.
    public static List<String> izmijeniPlovilo(Luka luka, Terminal terminal, String stariImoBroj,
                                                Plovilo azurirano, boolean rotacijaEksplicitnoZadata) {
        List<String> greske = PlovilaValidator.validiraj(luka, azurirano, stariImoBroj);
        if (!greske.isEmpty()) {
            return greske;
        }

        synchronized (terminal) {
            for (Dok d : terminal.getDokovi()) {
                Plovilo trenutno = d.getLokacija().getTrenutnoPlovilo();
                if (trenutno != null && trenutno.getImoBroj().equals(stariImoBroj)) {
                    azurirano.setBrzina(trenutno.getBrzina());
                    if (!rotacijaEksplicitnoZadata
                            && trenutno instanceof SluzbenoPlovilo staro
                            && azurirano instanceof SluzbenoPlovilo novoSluzbeno) {
                        novoSluzbeno.setRotacija(staro.isRotacija());
                    }
                    d.getLokacija().setTrenutnoPlovilo(azurirano);
                    return List.of();
                }
            }
        }

        List<String> nijePronadjeno = new ArrayList<>();
        nijePronadjeno.add("Plovilo sa IMO brojem " + stariImoBroj + " nije pronađeno na terminalu.");
        return nijePronadjeno;
    }

    /**
     * Uklanja plovilo sa zadatim IMO brojem sa terminala, oslobađajući njegov dok.
     *
     * @param terminal Terminal sa kojeg se plovilo uklanja.
     * @param imoBroj IMO broj plovila koje treba ukloniti.
     * @return {@code true} ako je plovilo pronađeno i uklonjeno.
     */
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
