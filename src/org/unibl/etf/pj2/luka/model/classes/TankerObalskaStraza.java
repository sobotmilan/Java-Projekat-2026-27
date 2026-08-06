package org.unibl.etf.pj2.luka.model.classes;

import org.unibl.etf.pj2.luka.model.interfaces.ObalskaStraza;

import java.io.File;

/**
 *
 * Klasa koja predstavlja podtip klase {@link Tanker} namijenjen za reprezentovanje tankera u upotrebi od strane obalske straže.
 *
 * <p>Za razliku od roditeljske klase {@link Tanker}, ova klasa implementira ugovor nametnut od strane interfejsa {@link ObalskaStraza} kroz atribut instance {@code rotacija}.
 * Ova klasa takođe definiše atribut instance {@code spisakPotjera}, koji čuva putanju do datoteke sa spiskom IMO brojeva plovila za kojima je raspisana međunarodna potjernica.</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see Tanker
 * @see ObalskaStraza
 */
public class TankerObalskaStraza extends Tanker implements ObalskaStraza {
    /** Prioritet plovila obalske straže dok mu je uključena rotacija. */
    public static final int PRIORITET_POD_ROTACIJOM = 2;

    /** Referenca na objekat klase {@link File} koja čuva apstraktnu putanju do datoteke koja sadrži spisak aktivnih potjernica. */
    private File spisakPotjera;

    /** Polje koje označava da li je rotacija na vozilu uključena. */
    private boolean rotacija;

    /**
     *
     * Konstruktor koji inicijalizuje sva polja instance roditeljske klase {@link Tanker} (odnosno {@link Plovilo}),
     * kao i polje {@code spisakPotjera}, uz podrazumijevanu vrijednost prioriteta (10), i rotaciju koja je u trenutku inicijalizacije isključena.
     *
     * @param naziv Naziv tankera
     * @param imoBroj IMO broj tankera
     * @param brojMotora Broj motora tankera
     * @param registarskiBroj Registarska oznaka tankera
     * @param fotografija Putanja do binarne datoteke čiji sadržaj predstavlja fotografiju tankera
     * @param zapreminaBarel Zapremina tankera izražena u barelima
     * @param spisakPotjera Referenca na objekat klase {@link File} koji čuva putanju do datoteke koja sadrži spisak aktivnih potjera
     *
     */
    public TankerObalskaStraza(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, double zapreminaBarel, File spisakPotjera) {
        super(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, zapreminaBarel);
        this.spisakPotjera = spisakPotjera;
        this.rotacija = false;
    }

    /**
     * Provjerava da li plovilo trenutno ima uključenu rotaciju. Uključena rotacija daje prioritet pri kretanju kroz plovne kanale terminala.
     *
     * @return true ako je rotacija aktivna, u suprotnom false.
     * */
    public boolean isRotacija() {
        return rotacija;
    }

    /**
     * Getter funkcija koja provjerava trenutno stanje {@code rotacija} atributa, te ukoliko je aktivna vraća imenovanu konstantu {@link #PRIORITET_POD_ROTACIJOM}, a u suprotnom prioritet plovila zadat pri inicijalizaciji.
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

    /**
     *
     * Getter metoda koja vraća referencu na objekat klase {@link File} koji čuva putanju do datoteke koja sadrži spisak aktivnih potjera.
     *
     * @return putanja do datoteke sa spiskom potjera
     *
     */
    @Override
    public File getSpisakPotjera() {
        return spisakPotjera;
    }
}
