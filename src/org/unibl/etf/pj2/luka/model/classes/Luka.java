package org.unibl.etf.pj2.luka.model.classes;

import org.unibl.etf.pj2.luka.simulation.BrodThread;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Luka implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<Terminal> terminali;
    private final Map<String, LocalDateTime> evidencijaUlaska;

    /**
     * Registar živih niti brodova trenutno prisutnih u luci — osnova za pretragu najbliže
     * patrole na nivou cijele luke (R4) i za pozivanje privezanih plovila da napuste terminal
     * (C7/C8). Transient jer {@link BrodThread} nije serijalizabilan, a žive niti ionako ne
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

    public Luka(List<Terminal> terminali, Map<String, LocalDateTime> evidencijaUlaska) {
        this.terminali = terminali;
        this.evidencijaUlaska = new ConcurrentHashMap<String, LocalDateTime>(evidencijaUlaska);
        this.aktivnaPlovila = ConcurrentHashMap.newKeySet();
    }

    public List<Terminal> getTerminali() {
        return terminali;
    }

    public boolean addTerminal(Terminal t) {
        return this.terminali.add(t);
    }

    public boolean removeTerminal(Terminal t) {
        return this.terminali.remove(t);
    }

    public Map<String, LocalDateTime> getEvidencijaUlaska() {
        return evidencijaUlaska;
    }

    public void addToEvidencija(String imoBroj, LocalDateTime time) {
        this.evidencijaUlaska.putIfAbsent(imoBroj, time);
    }

    public Set<BrodThread> getAktivnaPlovila() {
        return aktivnaPlovila;
    }

    @Serial
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        this.aktivnaPlovila = ConcurrentHashMap.newKeySet();
    }
}
