package org.unibl.etf.pj2.luka.model.classes;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Luka implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<Terminal> terminali;
    private final Map<String, LocalDateTime> evidencijaUlaska;

    public Luka(List<Terminal> terminali, Map<String, LocalDateTime> evidencijaUlaska) {
        this.terminali = terminali;
        this.evidencijaUlaska = new ConcurrentHashMap<String, LocalDateTime>(evidencijaUlaska);
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

}
