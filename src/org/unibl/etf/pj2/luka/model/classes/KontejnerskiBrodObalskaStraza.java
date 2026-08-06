package org.unibl.etf.pj2.luka.model.classes;

import org.unibl.etf.pj2.luka.model.interfaces.ObalskaStraza;

import java.io.File;

/**
 *
 * Klasa koja predstavlja podtip klase {@link KontejnerskiBrod} namijenjen za reprezentovanje kontejnerskih brodova u upotrebi od strane obalske straže.
 *
 * <p>Za razliku od roditeljske klase {@link KontejnerskiBrod}, ova klasa takođe implementira ugovor nametnut od strane interfejsa {@link ObalskaStraza} kroz atribut instance {@code rotacija}.
 * Ova klasa takođe definiše atribut instance {@code spisakPotjera}, koji čuva putanju do datoteke sa spiskom IMO brojeva plovila za kojima je raspisana međunarodna potjernica (M6).</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see KontejnerskiBrod
 * @see ObalskaStraza
 */
public class KontejnerskiBrodObalskaStraza extends KontejnerskiBrod implements ObalskaStraza {
    /** Prioritet plovila obalske straže dok mu je upaljena rotacija. */
    public static final int PRIORITET_POD_ROTACIJOM = 2;

    /** Referenca na objekat klase {@link File} koja čuva apstraktnu putanju do datoteke koja sadrži spisak aktivnih potjernica. */
    private File spisakPotjera;

    /** Polje koje označava da li je rotacija na vozilu upaljena. */
    private boolean rotacija;

    /**
     *
     * Konstruktor koji inicijalizuje sva polja instance roditeljske klase {@link KontejnerskiBrod} (odnosno {@link Plovilo}),
     * kao i polje {@code spisakPotjera}, izuzev prioriteta, koji se postavlja na podrazumijevanu vrijednost (10),
     * kao i rotacije koja je u trenutku inicijalizacije ugašena.
     *
     * @param naziv Naziv kontejnerskog broda
     * @param imoBroj IMO broj kontejnerskog broda
     * @param brojMotora Broj motora kontejnerskog broda
     * @param registarskiBroj Registarska oznaka kontejnerskog broda
     * @param fotografija Putanja do binarne datoteke čiji sadržaj predstavlja fotografiju kontejnerskog broda
     * @param kapacitetTEU Kapacitet kontejnerskog broda izražen u TEU.
     * @param spisakPotjera Referenca na objekat klase {@link File} koji čuva putanju do datoteke koja sadrži spisak aktivnih potjera.
     *
     */
    public KontejnerskiBrodObalskaStraza(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, int kapacitetTEU, File spisakPotjera) {
        super(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, kapacitetTEU);
        this.spisakPotjera = spisakPotjera;
        this.rotacija = false;
    }

    /**
     * Provjerava da li plovilo trenutno ima uključenu rotaciju. Upaljena rotacija daje prioritet pri kretanju kroz plovne kanale terminala.
     *
     * @return true ako je rotacija aktivna, u suprotnom false.
     *
     */
    public boolean isRotacija() {
        return rotacija;
    }

    /**
     * Getter funkcija koja provjerava trenutno stanje {@code rotacija} atributa, te ukoliko je aktivna vraća vrijednost 2, a u suprotnom prioritet plovila zadat pri inicijalizaciji.
     *
     * @return Cjelobrojna vrijednost koja predstavlja prioritet pri kretanju kroz terminal.
     *
     */
    @Override
    public int getPrioritet() {
        return isRotacija() ? PRIORITET_POD_ROTACIJOM : super.getPrioritet();
    }

    /**
     * Omogućava postavljanje rotacije na true ukoliko je rotacija aktivna, odnosno false ako je rotacija ugašena.
     *
     * @param rotacija Vrijednost na koju se postavlja stanje rotacije plovila.
     *
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
        return this.spisakPotjera;
    }
}
