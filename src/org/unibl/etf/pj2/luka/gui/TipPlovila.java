package org.unibl.etf.pj2.luka.gui;

import org.unibl.etf.pj2.luka.model.classes.KontejnerskiBrod;
import org.unibl.etf.pj2.luka.model.classes.KontejnerskiBrodObalskaStraza;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.PutnickiKruzer;
import org.unibl.etf.pj2.luka.model.classes.PutnickiKruzerCarina;
import org.unibl.etf.pj2.luka.model.classes.PutnickiKruzerObalskaStraza;
import org.unibl.etf.pj2.luka.model.classes.Tanker;
import org.unibl.etf.pj2.luka.model.classes.TankerCarina;
import org.unibl.etf.pj2.luka.model.classes.TankerObalskaStraza;
import org.unibl.etf.pj2.luka.model.classes.TankerVatrogasci;

/**
 * Sve kombinacije trupa i (opcione) državne službe koje administratorska forma nudi u padajućem
 * meniju, po jedna za svaku konkretnu klasu koja nasljeđuje {@link Plovilo}.
 *
 * <p>Svaka vrijednost nosi svoj prikazni naziv (za padajući meni), naziv službe kojoj plovilo
 * pripada (ili {@code null} za obično komercijalno plovilo), i naziv polja koje je specifično za
 * taj trup (kapacitet, broj putnika ili zapremina).</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see Plovilo
 */
public enum TipPlovila {
    /** Obični kontejnerski brod, bez pripadnosti državnoj službi. */
    KONTEJNERSKI("Kontejnerski brod", null, "Kapacitet (TEU)"),
    /** Kontejnerski brod u upotrebi obalske straže. */
    KONTEJNERSKI_OBALSKA_STRAZA("Kontejnerski brod (obalska straza)", "Obalska straža", "Kapacitet (TEU)"),
    /** Obični putnički kruzer, bez pripadnosti državnoj službi. */
    KRUZER("Putnički kruzer", null, "Broj putnika"),
    /** Putnički kruzer u upotrebi obalske straže. */
    KRUZER_OBALSKA_STRAZA("Putnički kruzer (obalska straza)", "Obalska straža", "Broj putnika"),
    /** Putnički kruzer u upotrebi carine. */
    KRUZER_CARINA("Putnički kruzer (carina)", "Carina", "Broj putnika"),
    /** Obični tanker, bez pripadnosti državnoj službi. */
    TANKER("Tanker", null, "Zapremina (barel)"),
    /** Tanker u upotrebi obalske straže. */
    TANKER_OBALSKA_STRAZA("Tanker (obalska straza)", "Obalska straža", "Zapremina (barel)"),
    /** Tanker u upotrebi carine. */
    TANKER_CARINA("Tanker (carina)", "Carina", "Zapremina (barel)"),
    /** Tanker u upotrebi vatrogasaca. */
    TANKER_VATROGASCI("Tanker (vatrogasci)", "Vatrogasci", "Zapremina (barel)");

    private final String naziv;
    private final String sluzba;
    private final String nazivSpecificnogPolja;

    TipPlovila(String naziv, String sluzba, String nazivSpecificnogPolja) {
        this.naziv = naziv;
        this.sluzba = sluzba;
        this.nazivSpecificnogPolja = nazivSpecificnogPolja;
    }

    /**
     * Omogućava dobijanje prikaznog naziva ovog tipa plovila, korištenog u padajućem meniju.
     *
     * @return Prikazni naziv tipa plovila.
     */
    public String getNaziv() {
        return naziv;
    }

    /**
     * Omogućava dobijanje naziva državne službe kojoj ovaj tip plovila pripada.
     *
     * @return Naziv službe, ili {@code null} ako je u pitanju obično komercijalno plovilo.
     */
    public String getSluzba() {
        return sluzba;
    }

    /**
     * Omogućava dobijanje naziva polja koje je specifično za trup ovog tipa plovila (kapacitet,
     * broj putnika ili zapremina), korištenog kao oznaka polja za unos u formi.
     *
     * @return Naziv specifičnog polja.
     */
    public String getNazivSpecificnogPolja() {
        return nazivSpecificnogPolja;
    }

    /**
     * Provjerava da li ovaj tip plovila zahtijeva spisak potjera, tj. da li pripada obalskoj
     * straži.
     *
     * @return {@code true} ako je u pitanju plovilo obalske straže.
     */
    public boolean zahtijevaSpisakPotjera() {
        return "Obalska straža".equals(sluzba);
    }

    /**
     * Redefinisana metoda {@code toString()} koja vraća prikazni naziv, korištena pri ispisu u
     * padajućem meniju.
     *
     * @return Prikazni naziv tipa plovila.
     */
    @Override
    public String toString() {
        return naziv;
    }

    /**
     * Određuje tip plovila na osnovu konkretne klase proslijeđenog objekta, korišteno pri
     * predpopunjavanju forme za izmjenu postojećeg plovila.
     *
     * @param p Plovilo čiji se tip određuje.
     * @return Odgovarajuća vrijednost enumeracije za konkretnu klasu plovila {@code p}.
     * @throws IllegalArgumentException Ako {@code p} nije nijedna od poznatih konkretnih klasa.
     */
    public static TipPlovila odObjekta(Plovilo p) {
        if (p instanceof KontejnerskiBrodObalskaStraza) {
            return KONTEJNERSKI_OBALSKA_STRAZA;
        }
        if (p instanceof KontejnerskiBrod) {
            return KONTEJNERSKI;
        }
        if (p instanceof PutnickiKruzerObalskaStraza) {
            return KRUZER_OBALSKA_STRAZA;
        }
        if (p instanceof PutnickiKruzerCarina) {
            return KRUZER_CARINA;
        }
        if (p instanceof PutnickiKruzer) {
            return KRUZER;
        }
        if (p instanceof TankerObalskaStraza) {
            return TANKER_OBALSKA_STRAZA;
        }
        if (p instanceof TankerCarina) {
            return TANKER_CARINA;
        }
        if (p instanceof TankerVatrogasci) {
            return TANKER_VATROGASCI;
        }
        if (p instanceof Tanker) {
            return TANKER;
        }
        throw new IllegalArgumentException("Nepoznat tip plovila: " + (p == null ? "null" : p.getClass()));
    }
}
