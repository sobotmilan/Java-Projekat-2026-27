package org.unibl.etf.pj2.luka.model.classes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class Luka implements Serializable {
    private final List<Terminal> terminali;
    private final Map<String, LocalDateTime> evidencijaUlaska;

    public Luka(List<Terminal> terminali, Map<String, LocalDateTime> evidencijaUlaska) {
        this.terminali = terminali;
        this.evidencijaUlaska = evidencijaUlaska;
    }

    public List<Terminal> getTerminali() {
        return terminali;
    }

    public Map<String, LocalDateTime> getEvidencijaUlaska() {
        return evidencijaUlaska;
    }

}
