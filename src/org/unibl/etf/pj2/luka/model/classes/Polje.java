package org.unibl.etf.pj2.luka.model.classes;

import java.io.Serial;
import java.io.Serializable;

public class Polje implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int x;
    private final int y;
    private String oznaka;
    private Plovilo trenutnoPlovilo;

    public Polje(int x, int y, String oznaka, Plovilo trenutnoPlovilo) {
        this.x = x;
        this.y = y;
        this.oznaka = oznaka;
        this.trenutnoPlovilo = trenutnoPlovilo;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getOznaka() {
        return oznaka;
    }

    public void setOznaka(String oznaka) {
        this.oznaka = oznaka;
    }

    public Plovilo getTrenutnoPlovilo() {
        return trenutnoPlovilo;
    }

    public void setTrenutnoPlovilo(Plovilo trenutnoPlovilo) {
        this.trenutnoPlovilo = trenutnoPlovilo;
    }

    @Override
    public String toString() {
        return oznaka;
    }
}
