package org.unibl.etf.pj2.luka.model.classes;

import org.unibl.etf.pj2.luka.simulation.BrodThread;

import java.io.Serializable;

public class Polje implements Serializable {
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
