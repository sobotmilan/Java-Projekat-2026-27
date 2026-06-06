package org.unibl.etf.pj2.luka.model.classes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Luka implements Serializable {
    private final List<Terminal> terminali;
    private final Map<String, LocalDateTime> evidencijaUlaska;
    private final Map<Terminal, AtomicInteger> brojSlobodnihVezova;

    public Luka(List<Terminal> terminali, Map<String, LocalDateTime> evidencijaUlaska) {
        this.terminali = terminali;
        this.evidencijaUlaska = evidencijaUlaska;
        this.brojSlobodnihVezova = new HashMap<Terminal, AtomicInteger>();
        for(Iterator<Terminal> iter = terminali.iterator(); iter.hasNext(); ) {
            this.brojSlobodnihVezova.put(iter.next(), new AtomicInteger(0));
        }
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
        this.evidencijaUlaska.put(imoBroj, time);
    }

}
