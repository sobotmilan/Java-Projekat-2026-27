package org.unibl.etf.pj2.luka.model.classes;

import org.unibl.etf.pj2.luka.model.interfaces.Carina;

import java.io.File;

public class PutnickiKruzerCarina extends PutnickiKruzer implements Carina {
    /** Polje koje označava da li je rotacija na vozilu upaljena. */
    private boolean rotacija;

    public PutnickiKruzerCarina(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, int brojPutnika) {
        super(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, brojPutnika, 3);
        this.rotacija = false;
    }

    /**
     * Provjerava da li plovilo trenutno ima uključenu rotaciju. Upaljena rotacija daje prioritet pri kretanju kroz plovne kanale terminala.
     *
     * @return true ako je rotacija aktivna, u suprotnom false.
     * */
    public boolean isRotacija() {
        return rotacija;
    }

    @Override
    public int getPrioritet() {
        return isRotacija() ? 3 : 10;
    }

    /**
     * Omogućava postavljanje rotacije na true ukoliko je rotacija aktivna, odnosno false ako je rotacija ugasena.
     *
     * @param rotacija Vrijednost na koju se postavlja stanje rotacije plovila.
     */
    public void setRotacija(boolean rotacija) {
        this.rotacija = rotacija;
    }
}
