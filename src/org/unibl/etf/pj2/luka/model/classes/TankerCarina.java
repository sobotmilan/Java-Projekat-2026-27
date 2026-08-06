package org.unibl.etf.pj2.luka.model.classes;

import org.unibl.etf.pj2.luka.model.interfaces.Carina;

import java.io.File;

/**
 *
 * Klasa koja predstavlja podtip klase {@link Tanker} namijenjen za reprezentovanje tankera u upotrebi od strane carine.
 *
 * <p>Za razliku od roditeljske klase {@link Tanker}, ova klasa implementira ugovor nametnut od strane interfejsa {@link Carina} kroz atribut instance {@code rotacija}.</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see Tanker
 * @see Carina
 */
public class TankerCarina extends Tanker implements Carina {
    /** Prioritet carinskog plovila dok mu je uključena rotacija. */
    public static final int PRIORITET_POD_ROTACIJOM = 3;

    /** Polje koje označava da li je rotacija na vozilu uključena. */
    private boolean rotacija;

    /**
     *
     * Konstruktor koji inicijalizuje sve atribute instance roditeljske klase {@link Tanker} (odnosno {@link Plovilo}),
     * uz podrazumijevanu vrijednost prioriteta (10), i rotaciju koja je u trenutku inicijalizacije isključena.
     *
     * @param naziv Naziv tankera
     * @param imoBroj IMO broj tankera
     * @param brojMotora Broj motora tankera
     * @param registarskiBroj Registarska oznaka tankera
     * @param fotografija Putanja do binarne datoteke čiji sadržaj predstavlja fotografiju tankera
     * @param zapreminaBarel Zapremina tankera, izražena u barelima
     *
     */
    public TankerCarina(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, double zapreminaBarel) {
        super(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, zapreminaBarel);
        this.rotacija = false;
    }

    /**
     * Provjerava da li plovilo trenutno ima uključenu rotaciju. Uključena rotacija mu daje prioritet pri kretanju kroz plovne kanale terminala.
     *
     * @return true ako je rotacija uključena, u suprotnom false.
     * */
    public boolean isRotacija() {
        return rotacija;
    }

    /**
     * Getter funkcija koja provjerava trenutno stanje {@code rotacija} atributa, te ukoliko je uključena vraća imenovanu konstantu {@link #PRIORITET_POD_ROTACIJOM}, a u suprotnom prioritet plovila zadat pri inicijalizaciji.
     *
     * @return Cjelobrojna vrijednost koja predstavlja prioritet pri kretanju kroz terminal.
     *
     */
    @Override
    public int getPrioritet() {
        return isRotacija() ? PRIORITET_POD_ROTACIJOM : super.getPrioritet();
    }

    /**
     * Omogućava postavljanje rotacije na true ukoliko je rotacija uključena, odnosno false ako je rotacija isključena.
     *
     * @param rotacija Vrijednost na koju se postavlja stanje rotacije plovila.
     */
    public void setRotacija(boolean rotacija) {
        this.rotacija = rotacija;
    }
}
