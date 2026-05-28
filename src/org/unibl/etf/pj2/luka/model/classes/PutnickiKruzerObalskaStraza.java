package org.unibl.etf.pj2.luka.model.classes;

import org.unibl.etf.pj2.luka.model.interfaces.ObalskaStraza;

import java.io.File;

public class PutnickiKruzerObalskaStraza extends PutnickiKruzer implements ObalskaStraza {
    private File spisakPotjera;

    public PutnickiKruzerObalskaStraza(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, int brojPutnika, File spisakPotjera) {
        super(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, brojPutnika);
        this.spisakPotjera = spisakPotjera;
    }

    @Override
    public File getSpisakPotjera() {
        return this.spisakPotjera;
    }
}
