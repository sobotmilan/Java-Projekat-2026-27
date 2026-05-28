package org.unibl.etf.pj2.luka.model.classes;

import org.unibl.etf.pj2.luka.model.interfaces.ObalskaStraza;

import java.io.File;

public class TankerObalskaStraza extends Tanker implements ObalskaStraza {
    private File spisakPotjera;

    public TankerObalskaStraza(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, double zapreminaBarel, File spisakPotjera) {
        super(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, zapreminaBarel);
        this.spisakPotjera = spisakPotjera;
    }

    @Override
    public File getSpisakPotjera() {
        return spisakPotjera;
    }
}
