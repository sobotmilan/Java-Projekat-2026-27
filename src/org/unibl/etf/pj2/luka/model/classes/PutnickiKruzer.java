package org.unibl.etf.pj2.luka.model.classes;

import java.io.File;

public class PutnickiKruzer extends Plovilo {
    private int brojPutnika;

    public PutnickiKruzer(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, int brojPutnika) {
        this(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, brojPutnika, 10);
    }

    public PutnickiKruzer(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, int brojPutnika, int prioritet) {
        super(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, prioritet);
        this.brojPutnika = brojPutnika;
    }

    public int getBrojPutnika() {
        return brojPutnika;
    }

    public void setBrojPutnika(int brojPutnika) {
        this.brojPutnika = brojPutnika;
    }

}
