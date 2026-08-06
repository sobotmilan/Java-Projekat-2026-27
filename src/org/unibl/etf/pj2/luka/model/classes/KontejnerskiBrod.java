package org.unibl.etf.pj2.luka.model.classes;

import java.io.File;

/**
 *
 * Klasa koja reprezentuje tip objekta klase {@link Plovilo}-a namijenjen za prevoz teškog tereta, spada pod komercijalna plovila.
 *
 * <p>Jedinstvena osobina svakog kontejnerskog broda jeste njegov kapacitet, izražen u TEU (<i>Twenty-foot Equivalent Unit</i>).</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see Plovilo
 *
 */
public class KontejnerskiBrod extends Plovilo {
    /**
     *
     * Jedinstvena osobina svakog kontejnerskog broda - njegov maksimalni kapacitet prenosa tereta izražen u TEU (<i>Twenty-foot Equivalent Unit</i>).
     *
     */
    private int kapacitetTEU;

    /**
     *
     * Konstruktor koji inicijalizuje sva polja instance roditeljske klase {@link Plovilo} izuzev prioriteta, kao i atribut instance {@code kapacitetTEU }, a prioritet postavlja na podrazumijevanu vrijednost (10).
     *
     * @param naziv Naziv kontejnerskog broda
     * @param imoBroj IMO broj kontejnerskog broda
     * @param brojMotora Broj motora kontejnerskog broda
     * @param registarskiBroj Registarska oznaka kontejnerskog broda
     * @param fotografija Putanja do binarne datoteke čiji sadržaj predstavlja fotografiju kontejnerskog broda
     * @param kapacitetTEU Kapacitet kontejnerskog broda izražen u TEU.
     *
     */
    public KontejnerskiBrod(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, int kapacitetTEU) {
        this(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, kapacitetTEU, 10);
    }

    /**
     *
     * Konstruktor koji inicijalizuje sva polja instance roditeljske klase {@link Plovilo}, kao i atribut instance {@code kapacitetTEU}.
     *
     * @param naziv Naziv kontejnerskog broda
     * @param imoBroj IMO broj kontejnerskog broda
     * @param brojMotora Broj motora kontejnerskog broda
     * @param registarskiBroj Registarska oznaka kontejnerskog broda
     * @param fotografija Putanja do binarne datoteke čiji sadržaj predstavlja fotografiju kontejnerskog broda
     * @param kapacitetTEU Kapacitet kontejnerskog broda izražen u TEU.
     * @param prioritet Prioritet kontejnerskog broda u saobraćaju
     *
     */
    public KontejnerskiBrod(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, int kapacitetTEU, int prioritet) {
        super(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, prioritet);
        this.kapacitetTEU = kapacitetTEU;
    }

    /**
     *
     * Getterska metoda koja vraća cjelobrojnu vrijednost koja predstavlja kapacitet kontejnerskog broda izraženog u TEU.
     *
     * @return kapacitet kontejnerskog broda izražen u TEU.
     *
     */
    public int getKapacitetTEU() {
        return kapacitetTEU;
    }

    /**
     *
     * Setterska metoda koja postavlja kapacitet kontejnerskog broda na cjelobrojnu vrijednost proslijeđenu kao parametar poziva metode.
     *
     * @param kapacitetTEU vrijednost kapaciteta izraženog u TEU na koju se postavlja kapacitet posmatranog kontejnerskog broda.
     *
     */
    public void setKapacitetTEU(int kapacitetTEU) {
        this.kapacitetTEU = kapacitetTEU;
    }

}
