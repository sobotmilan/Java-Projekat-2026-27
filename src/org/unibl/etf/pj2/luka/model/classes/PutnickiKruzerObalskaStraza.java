package org.unibl.etf.pj2.luka.model.classes;

import org.unibl.etf.pj2.luka.model.interfaces.ObalskaStraza;

import java.io.File;

public class PutnickiKruzerObalskaStraza extends PutnickiKruzer implements ObalskaStraza {
    /** Prioritet plovila obalske straže dok mu je upaljena rotacija. */
    public static final int PRIORITET_POD_ROTACIJOM = 2;

    private File spisakPotjera;

    /** Polje koje označava da li je rotacija na vozilu upaljena. */
    private boolean rotacija;

    public PutnickiKruzerObalskaStraza(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, int brojPutnika, File spisakPotjera) {
        super(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, brojPutnika);
        this.spisakPotjera = spisakPotjera;
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
        return isRotacija() ? PRIORITET_POD_ROTACIJOM : super.getPrioritet();
    }


    /**
     * Omogućava postavljanje rotacije na true ukoliko je rotacija aktivna, odnosno false ako je rotacija ugasena.
     *
     * @param rotacija Vrijednost na koju se postavlja stanje rotacije plovila.
     */
    public void setRotacija(boolean rotacija) {
        this.rotacija = rotacija;
    }

    @Override
    public File getSpisakPotjera() {
        return this.spisakPotjera;
    }
}
