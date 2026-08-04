package org.unibl.etf.pj2.luka.model.classes;

import org.unibl.etf.pj2.luka.model.interfaces.Vatrogasci;

import java.io.File;

public class TankerVatrogasci extends Tanker implements Vatrogasci {
    /** Prioritet vatrogasnog plovila dok mu je upaljena rotacija. */
    public static final int PRIORITET_POD_ROTACIJOM = 1;

    /** Polje koje označava da li je rotacija na vozilu upaljena. */
    private boolean rotacija;

    public TankerVatrogasci(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, double zapreminaBarel) {
        super(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, zapreminaBarel);
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
}
