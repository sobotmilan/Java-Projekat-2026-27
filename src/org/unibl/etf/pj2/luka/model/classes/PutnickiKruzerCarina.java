package org.unibl.etf.pj2.luka.model.classes;

import org.unibl.etf.pj2.luka.model.interfaces.Carina;

import java.io.File;

/**
 *
 * Klasa koja predstavlja podtip klase {@link PutnickiKruzer} namijenjen za reprezentovanje putničkih kruzera u upotrebi od strane carine.
 *
 * <p>Za razliku od roditeljske klase {@link PutnickiKruzer}, ova klasa implementira ugovor nametnut od strane interfejsa {@link Carina} kroz atribut instance {@code rotacija}.</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see PutnickiKruzer
 * @see Carina
 */
public class PutnickiKruzerCarina extends PutnickiKruzer implements Carina {
    /** Prioritet carinskog plovila dok mu je upaljena rotacija. */
    public static final int PRIORITET_POD_ROTACIJOM = 3;

    /** Polje koje označava da li je rotacija na vozilu upaljena. */
    private boolean rotacija;

    /**
     *
     * Konstruktor koji inicijalizuje sva polja instance roditeljske klase {@link PutnickiKruzer} (odnosno {@link Plovilo}),
     * uz podrazumijevanu vrijednost prioriteta (10), i rotaciju koja je u trenutku inicijalizacije isključena tj. postavljena na false.
     *
     * @param naziv Naziv putničkog kruzera
     * @param imoBroj IMO broj putničkog kruzera
     * @param brojMotora Broj motora putničkog kruzera
     * @param registarskiBroj Registarska oznaka putničkog kruzera
     * @param fotografija Putanja do binarne datoteke čiji sadržaj predstavlja fotografiju putničkog kruzera
     * @param brojPutnika Broj putnika koje kruzer može prevesti
     *
     */
    public PutnickiKruzerCarina(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, int brojPutnika) {
        super(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, brojPutnika);
        this.rotacija = false;
    }

    /**
     * Provjerava da li plovilo trenutno ima uključenu rotaciju. Uključena rotacija daje prioritet pri kretanju kroz plovne kanale terminala.
     *
     * @return true ako je rotacija aktivna, u suprotnom false.
     */
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
     * Omogućava postavljanje rotacije na true ukoliko je rotacija aktivna, odnosno false ako je rotacija ugasena.
     *
     * @param rotacija Vrijednost na koju se postavlja stanje rotacije plovila.
     */
    public void setRotacija(boolean rotacija) {
        this.rotacija = rotacija;
    }
}
