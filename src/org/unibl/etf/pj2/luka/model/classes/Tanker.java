package org.unibl.etf.pj2.luka.model.classes;

import java.io.File;

public class Tanker extends Plovilo {
    private double zapreminaBarel;

    public Tanker(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, double zapreminaBarel) {
        super(naziv, imoBroj, brojMotora, registarskiBroj, fotografija);
        this.zapreminaBarel = zapreminaBarel;
    }

    public double getZapreminaBarel() {
        return zapreminaBarel;
    }

    public void setZapreminaBarel(double zapreminaBarel) {
        this.zapreminaBarel = zapreminaBarel;
    }

}
