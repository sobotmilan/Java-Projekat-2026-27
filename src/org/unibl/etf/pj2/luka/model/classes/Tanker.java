package org.unibl.etf.pj2.luka.model.classes;

import java.io.File;

/**
 *
 * Klasa koja reprezentuje tip objekta klase {@link Plovilo} namijenjen za prevoz tečnog tereta (npr. nafte), spada pod komercijalni tip plovila.
 *
 * <p>Jedinstvena osobina svakog tankera jeste njegova zapremina, izražena u barelima (nafte).</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see Plovilo
 */
public class Tanker extends Plovilo {
    /**
     *
     * Jedinstvena osobina svakog tankera - njegova zapremina izražena u barelima.
     *
     */
    private double zapreminaBarel;

    /**
     *
     * Konstruktor koji inicijalizuje sva polja instance roditeljske klase {@link Plovilo} izuzev prioriteta, kao i atribut instance {@code zapreminaBarel}, a prioritet postavlja na podrazumijevanu vrijednost (10).
     *
     * @param naziv Naziv tankera
     * @param imoBroj IMO broj tankera
     * @param brojMotora Broj motora tankera
     * @param registarskiBroj Registarska oznaka tankera
     * @param fotografija Putanja do binarne datoteke čiji sadržaj predstavlja fotografiju tankera
     * @param zapreminaBarel Zapremina tankera izražena u barelima
     *
     */
    public Tanker(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, double zapreminaBarel) {
        this(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, zapreminaBarel,  10);
    }

    /**
     *
     * Konstruktor koji inicijalizuje sva polja instance roditeljske klase {@link Plovilo}, kao i atribut instance {@code zapreminaBarel}.
     *
     * @param naziv Naziv tankera
     * @param imoBroj IMO broj tankera
     * @param brojMotora Broj motora tankera
     * @param registarskiBroj Registarska oznaka tankera
     * @param fotografija Putanja do binarne datoteke čiji sadržaj predstavlja fotografiju tankera
     * @param zapreminaBarel Zapremina tankera izražena u barelima
     * @param prioritet Prioritet tankera u saobraćaju
     *
     */
    public Tanker(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, double zapreminaBarel, int prioritet) {
        super(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, prioritet);
        this.zapreminaBarel = zapreminaBarel;
    }

    /**
     *
     * Getterska metoda koja vraća realnu brojnu vrijednost koja predstavlja zapreminu tankera izraženu u barelima.
     *
     * @return Zapremina tankera izražena u barelima
     *
     */
    public double getZapreminaBarel() {
        return zapreminaBarel;
    }

    /**
     *
     * Setterska metoda koja postavlja zapreminu tankera na realnu brojnu vrijednost proslijeđenu kao parametar poziva metode.
     *
     * @param zapreminaBarel Vrijednost zapremine izražene u barelima na koju se postavlja zapremina posmatranog tankera
     *
     */
    public void setZapreminaBarel(double zapreminaBarel) {
        this.zapreminaBarel = zapreminaBarel;
    }

}
