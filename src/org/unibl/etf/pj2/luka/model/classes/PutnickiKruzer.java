package org.unibl.etf.pj2.luka.model.classes;

import java.io.File;

/**
 *
 * Klasa koja reprezentuje tip objekta klase {@link Plovilo} namijenjen za prevoz putnika, spada pod komercijalna plovila.
 *
 * <p>Jedinstvena osobina svakog putničkog kruzera jeste broj putnika koje može prevesti.</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see Plovilo
 */
public class PutnickiKruzer extends Plovilo {
    /**
     *
     * Jedinstvena osobina svakog putničkog kruzera - broj putnika koje kruzer može prevesti.
     *
     */
    private int brojPutnika;

    /**
     *
     * Konstruktor koji inicijalizuje sva polja instance roditeljske klase {@link Plovilo} izuzev prioriteta, kao i atribut instance {@code brojPutnika}, a prioritet postavlja na podrazumijevanu vrijednost (10).
     *
     * @param naziv Naziv putničkog kruzera
     * @param imoBroj IMO broj putničkog kruzera
     * @param brojMotora Broj motora putničkog kruzera
     * @param registarskiBroj Registarska oznaka putničkog kruzera
     * @param fotografija Putanja do binarne datoteke čiji sadržaj predstavlja fotografiju putničkog kruzera
     * @param brojPutnika Broj putnika koje kruzer može prevesti.
     *
     */
    public PutnickiKruzer(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, int brojPutnika) {
        this(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, brojPutnika, 10);
    }

    /**
     *
     * Konstruktor koji inicijalizuje sva polja instance roditeljske klase {@link Plovilo}, kao i atribut instance {@code brojPutnika}.
     *
     * @param naziv Naziv putničkog kruzera
     * @param imoBroj IMO broj putničkog kruzera
     * @param brojMotora Broj motora putničkog kruzera
     * @param registarskiBroj Registarska oznaka putničkog kruzera
     * @param fotografija Putanja do binarne datoteke čiji sadržaj predstavlja fotografiju putničkog kruzera
     * @param brojPutnika Broj putnika koje kruzer može prevesti
     * @param prioritet Prioritet putničkog kruzera u saobraćaju
     *
     */
    public PutnickiKruzer(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, int brojPutnika, int prioritet) {
        super(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, prioritet);
        this.brojPutnika = brojPutnika;
    }

    /**
     *
     * Getterska metoda koja vraća cjelobrojnu vrijednost koja predstavlja broj putnika koje kruzer može prevesti.
     *
     * @return Broj putnika na kruzeru.
     *
     */
    public int getBrojPutnika() {
        return brojPutnika;
    }

    /**
     *
     * Setterska metoda koja postavlja broj putnika na kruzeru na cjelobrojnu vrijednost proslijeđenu kao parametar poziva metode.
     *
     * @param brojPutnika Vrijednost broja putnika na koju se postavlja broj putnika posmatranog kruzera.
     *
     */
    public void setBrojPutnika(int brojPutnika) {
        this.brojPutnika = brojPutnika;
    }

}
