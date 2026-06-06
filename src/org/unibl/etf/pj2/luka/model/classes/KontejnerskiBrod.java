package org.unibl.etf.pj2.luka.model.classes;

import java.io.File;

public class KontejnerskiBrod extends Plovilo {
    private int kapacitetTEU;

    public KontejnerskiBrod(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, int kapacitetTEU) {
        this(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, kapacitetTEU, 10);
    }

    public KontejnerskiBrod(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, int kapacitetTEU, int prioritet) {
        super(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, prioritet);
        this.kapacitetTEU = kapacitetTEU;
    }

    public int getKapacitetTEU() {
        return kapacitetTEU;
    }

    public void setKapacitetTEU(int kapacitetTEU) {
        this.kapacitetTEU = kapacitetTEU;
    }

}
