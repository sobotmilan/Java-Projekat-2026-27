package org.unibl.etf.pj2.luka.model.classes;

import org.unibl.etf.pj2.luka.simulation.BrodThread;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Korijenska klasa čitavog modela, predstavlja luku kao jednu cjelinu.
 *
 *
 * <p>Sastoji se od sljedećih komponenata: niz {@link Terminal}-a, evidencija
 * vremena ulaska svakog plovila (ključ je IMO broj plovila) i registar trenutno aktivnih niti (Thread-ova) plovila.</p>
 *
 * <p>Ova klasa implementira interfejs {@link Serializable} kako bi se cijelo stanje luke moglo sačuvati u
 * {@code luka.ser} i pauzirati/nastaviti između sesija pokretanja/izvršavanja aplikacije.</p>
 *
 * <p><b>Napomena o zavisnosti paketa:</b> ova klasa uvozi {@link BrodThread} zbog polja {@code aktivnaPlovila}
 * čime nastaje kružno importovanje paketa.</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see Terminal
 * @see BrodThread
 */
public class Luka implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** Terminali koji čine luku, u redoslijedu kojim ih plovilo obilazi pri ulasku. */
    private final List<Terminal> terminali;

    /**
     * Evidencija vremena ulaska po IMO broju plovila, osnova za obračun taksi.
     * Ova mapa je tip {@link ConcurrentHashMap}-a jer joj pristupaju niti više brodova istovremeno preko metode
     * {@link #addToEvidencija(String, LocalDateTime)}.
     */
    private final Map<String, LocalDateTime> evidencijaUlaska;

    /**
     * Registar živih niti brodova trenutno prisutnih u luci, predstavlja osnovu za pretragu najbliže
     * patrole na nivou cijele luke i za pozivanje privezanih plovila da napuste terminal.
     * Označena sa {@code Transient} jer {@link BrodThread} nije serijalizabilan, a žive niti ionako ne
     * preživljavaju ponovno pokretanje JVM-a.
     *
     * <p><b>Namjerno nije {@code final}</b> iako bi konceptualno trebalo biti: Java inline
     * inicijalizator transient polja nikad se ne izvršava pri deserijalizaciji (samo pri
     * običnoj konstrukciji preko konstruktora), pa bi {@code final} polje sa inline
     * inicijalizatorom ostalo trajno {@code null} nakon učitavanja iz {@code luka.ser}.
     * Umjesto toga inicijalizacija se dešava u konstruktoru i ponovo u
     * {@link #readObject(ObjectInputStream)}.</p>
     */
    private transient Set<BrodThread> aktivnaPlovila;

    /**
     * Vrijeme posljednjeg čuvanja stanja luke, postavlja ga {@code SerializationUtil} pri
     * svakom pozivu {@code serijalizujStanjeLuke()}. Koristi se pri sljedećem učitavanju da se
     * period dok je JVM bio ugašen (aplikacija zatvorena) isključi iz obračuna proteklog vremena
     * boravka plovila u evidenciji ulaska — bez ovoga bi plovilo zatečeno u {@code luka.ser} od
     * prošle sesije platilo taksu i za vrijeme dok aplikacija uopšte nije radila.
     * {@code null} za luku koja još nikad nije sačuvana (prvo pokretanje) ili učitanu iz starijeg
     * {@code luka.ser} fajla nastalog prije uvođenja ovog polja.
     */
    private LocalDateTime vrijemeZadnjegCuvanja;

    /**
     * Kreira luku sa zadatim terminalima i evidencijom ulaska. Evidencija se kopira u novu
     * {@link ConcurrentHashMap} bez obzira na implementaciju proslijeđene mape.
     *
     * @param terminali Terminali koji čine luku.
     * @param evidencijaUlaska Početna evidencija vremena ulaska po IMO broju (npr. prazna mapa
     *                         pri prvom pokretanju, ili preuzeta iz prethodne sesije).
     */
    public Luka(List<Terminal> terminali, Map<String, LocalDateTime> evidencijaUlaska) {
        this.terminali = terminali;
        this.evidencijaUlaska = new ConcurrentHashMap<String, LocalDateTime>(evidencijaUlaska);
        this.aktivnaPlovila = ConcurrentHashMap.newKeySet();
    }

    /**
     * Omogućava dobijanje liste svih terminala luke, u redoslijedu obilaska.
     *
     * @return Lista terminala luke.
     */
    public List<Terminal> getTerminali() {
        return terminali;
    }

    /**
     * Dodaje terminal u luku.
     *
     * @param t Terminal koji se dodaje.
     * @return {@code true} ako je terminal uspješno dodat.
     */
    public boolean addTerminal(Terminal t) {
        return this.terminali.add(t);
    }

    /**
     * Uklanja terminal iz luke.
     *
     * @param t Terminal koji se uklanja.
     * @return {@code true} ako je terminal bio prisutan i uspješno uklonjen.
     */
    public boolean removeTerminal(Terminal t) {
        return this.terminali.remove(t);
    }

    /**
     * Omogućava dobijanje evidencije vremena ulaska po IMO broju plovila.
     *
     * @return Mapa IMO broj → vrijeme ulaska.
     */
    public Map<String, LocalDateTime> getEvidencijaUlaska() {
        return evidencijaUlaska;
    }

    /**
     * Evidentira vrijeme ulaska plovila u luku, ako za taj IMO broj već ne postoji zapis.
     * Atomarna operacija ({@link ConcurrentHashMap#putIfAbsent}) — sprečava da dva broda sa istim
     * IMO brojem (npr. usljed kolizije IMO brojača) prepišu tuđe vrijeme ulaska.
     *
     * @param imoBroj IMO broj plovila koje ulazi.
     * @param time Vrijeme ulaska.
     */
    public void addToEvidencija(String imoBroj, LocalDateTime time) {
        this.evidencijaUlaska.putIfAbsent(imoBroj, time);
    }

    /**
     * Omogućava dobijanje registra živih niti brodova trenutno prisutnih u luci.
     *
     * @return Skup aktivnih {@link BrodThread} instanci.
     * @see #aktivnaPlovila
     */
    public Set<BrodThread> getAktivnaPlovila() {
        return aktivnaPlovila;
    }

    /**
     * Omogućava dobijanje vremena posljednjeg čuvanja stanja luke.
     *
     * @return Vrijeme posljednjeg poziva {@code serijalizujStanjeLuke()}, ili {@code null} ako
     *         luka još nije sačuvana ili je učitana iz starijeg fajla bez ovog polja.
     * @see #vrijemeZadnjegCuvanja
     */
    public LocalDateTime getVrijemeZadnjegCuvanja() {
        return vrijemeZadnjegCuvanja;
    }

    /**
     * Omogućava postavljanje vremena posljednjeg čuvanja stanja luke. Poziva ga
     * {@code SerializationUtil} neposredno prije upisa u {@code luka.ser}.
     *
     * @param vrijemeZadnjegCuvanja Vrijeme čuvanja koje treba zapamtiti.
     */
    public void setVrijemeZadnjegCuvanja(LocalDateTime vrijemeZadnjegCuvanja) {
        this.vrijemeZadnjegCuvanja = vrijemeZadnjegCuvanja;
    }

    /**
     * Pomjera sve postojeće timestampove u evidenciji ulaska unaprijed za zadatu pauzu —
     * "preskače" period koji pauza predstavlja, kao da plovila nisu čekala dok je aplikacija bila
     * ugašena. Bez efekta ako je {@code pauza} {@code null}, nula ili negativna.
     *
     * @param pauza Trajanje pauze koje treba isključiti iz obračuna proteklog vremena.
     */
    public void pomjeriEvidencijuZaPauzu(Duration pauza) {
        if (pauza == null || pauza.isZero() || pauza.isNegative()) {
            return;
        }
        for (Map.Entry<String, LocalDateTime> unos : evidencijaUlaska.entrySet()) {
            unos.setValue(unos.getValue().plus(pauza));
        }
    }

    /**
     * Ponovo inicijalizuje {@link #aktivnaPlovila} nakon deserijalizacije, jer je polje
     * {@code transient} i njegov inline inicijalizator se pri deserijalizaciji ne izvršava
     * (vidi napomenu uz {@link #aktivnaPlovila}).
     *
     * @param ois Ulazni tok iz kojeg se stanje objekta čita.
     * @throws IOException Ako dođe do greške pri čitanju toka.
     * @throws ClassNotFoundException Ako klasa serijalizovanog objekta nije pronađena.
     */
    @Serial
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        this.aktivnaPlovila = ConcurrentHashMap.newKeySet();
    }
}
