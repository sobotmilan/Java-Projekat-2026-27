package org.unibl.etf.pj2.luka.model.classes;

import java.io.Serializable;

public class Dok implements Serializable {
    private final Polje lokacija;
    private final int oznakaVezova;

    public Dok(Polje lokacija, int oznakaVezova) {
        this.lokacija = lokacija;
        this.oznakaVezova = oznakaVezova;
    }

    public Polje getLokacija() {
        return lokacija;
    }

    public int getOznakaVezova() {
        return oznakaVezova;
    }

    public boolean isSlobodan() {
        return lokacija.getTrenutnoPlovilo() == null;
    }
}
