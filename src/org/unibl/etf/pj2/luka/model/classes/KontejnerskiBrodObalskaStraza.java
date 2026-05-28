package org.unibl.etf.pj2.luka.model.classes;

import org.unibl.etf.pj2.luka.model.interfaces.ObalskaStraza;

import java.io.File;

public class KontejnerskiBrodObalskaStraza extends KontejnerskiBrod implements ObalskaStraza {
    private File spisakPotjera;

    public KontejnerskiBrodObalskaStraza(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, int kapacitetTEU, File spisakPotjera) {
        super(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, kapacitetTEU);
        this.spisakPotjera = spisakPotjera;
    }

    @Override
    public File getSpisakPotjera() {
        return this.spisakPotjera;
    }
}
