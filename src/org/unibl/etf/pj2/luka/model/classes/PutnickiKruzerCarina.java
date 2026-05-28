package org.unibl.etf.pj2.luka.model.classes;

import org.unibl.etf.pj2.luka.model.interfaces.Carina;

import java.io.File;

public class PutnickiKruzerCarina extends PutnickiKruzer implements Carina {
    public PutnickiKruzerCarina(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, int brojPutnika) {
        super(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, brojPutnika);
    }
}
