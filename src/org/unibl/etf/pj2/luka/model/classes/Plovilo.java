package org.unibl.etf.pj2.luka.model.classes;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;

/**
 * Apstraktna klasa koja modeluje sve osnovne karakteristike i ponašanja svakog pomorskog plovila u sistemu.
 *
 * <p> Sadrži atribute koji su definisani kao zajednički za sva plovila poput IMO broja, naziva, broja motora, registarske oznake, brzine itd.
 * Implementira {@link Serializable} interfejs kako bi se omogućilo dugoročno čuvanje stanja plovila kroz više sesija pokretanja i izvršavanja simulacije.</p>
 *
 * @author Milan Šobot
 * @version 1.0
 */
abstract public class Plovilo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** Naziv plovila. */
    private String naziv;

    /** Identifikacioni broj plovila u međunarodnom saobraćaju. */
    private String imoBroj;

    /** Serijski broj motora. */
    private String brojMotora;

    /** Registarska oznaka plovila. */
    private String registarskiBroj;

    /** Referenca na objekat tipa File koji čuva apstraktnu putanju do binarne datoteke koja predstavlja fotografiju plovila. */
    private File fotografija;

    /** Brzina kretanja plovila. */
    private double brzina;

    /** Vozila sa većim prioritetom imaju nižu vrijednost u ovom polju.*/
    private final int prioritet;

    /**
     * Konstruktor kojim se ručno inicijalizuju svi osnovni atributi plovila osim brzine, koja se inicijalizuje kao slučajna brojna vrijednost sa pokretnim zarezom u intervalu [1,50).
     *
     * @param naziv Naziv plovila.
     * @param imoBroj Identifikacioni broj plovila u međunarodnom saobraćaju.
     * @param brojMotora Serijski broj motora.
     * @param registarskiBroj Registarska oznaka plovila.
     * @param fotografija Referenca na objekat tipa File koji čuva apstraktnu putanju do binarne datoteke koja predstavlja fotografiju plovila.
     * @param prioritet Prioritet plovila u saobraćaju (niža vrijednost == viši prioritet), službena plovila redefinišu getPrioritet() i ovu vrijednost ignorišu, jer njihov prioritet zavisi od toga da li je rotacija uključena ili ne.
     *
     */
    public Plovilo(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, int prioritet) {
        this.naziv = naziv;
        this.imoBroj = imoBroj;
        this.brojMotora = brojMotora;
        this.registarskiBroj = registarskiBroj;
        this.fotografija = fotografija;
        this.brzina = java.util.concurrent.ThreadLocalRandom.current().nextDouble(1,50);
        this.prioritet = prioritet;
    }

    /**
     * Getterska metoda za naziv plovila.
     *
     * @return Naziv plovila.
     */
    public String getNaziv() {
        return naziv;
    }

    /**
     * Omogućava postavljanje naziva plovila.
     *
     * @param naziv Naziv plovila.
     */
    public void setNaziv(String naziv) {
        this.naziv = naziv;
    }

    /**
     * Getterska metoda za identifikator plovila u međunarodnom saobraćaju.
     *
     * @return IMO broj plovila.
     */
    public String getImoBroj() {
        return imoBroj;
    }

    /**
     * Omogućava postavljanje identifikatora plovila u međunarodnom saobraćaju.
     * <p>
     * <b>Napomena:</b> {@link #equals(Object)} i {@link #hashCode()} se računaju iz IMO broja.
     * Ako je plovilo već ubačeno u {@link java.util.HashMap}/{@link java.util.HashSet} (npr. evidenciju
     * ulaska), promjena IMO broja ovim setterom ga "gubi" u toj kolekciji, odnosno on ostaje u starom <i>bucket</i>-u
     * i više se ne pronalazi po novom ključu. Poželjno je plovilo prije izmjene IMO broja ukloniti iz takvih kolekcija i
     * ponovo ga dodati nakon promjene istog.
     *
     * @param imoBroj Identifikator plovila u međunarodnom saobraćaju.
     */
    public void setImoBroj(String imoBroj) {
        this.imoBroj = imoBroj;
    }

    /**
     * Getterska metoda za serijski broj motora plovila.
     *
     * @return Serijski broj motora plovila.
     */
    public String getBrojMotora() {
        return brojMotora;
    }

    /**
     * Omogućava postavljanje serijskog broja motora plovila.
     *
     * @param brojMotora Serijski broj motora plovila.
     */
    public void setBrojMotora(String brojMotora) {
        this.brojMotora = brojMotora;
    }

    /**
     * Getterska metoda za registarsku oznaka plovila.
     *
     * @return Registarsku oznaku plovila.
     */
    public String getRegistarskiBroj() {
        return registarskiBroj;
    }

    /**
     * Omogućava postavljanje registarske oznake plovila na proizvoljni niz karaktera.
     *
     * @param registarskiBroj Registarska oznaka plovila.
     */
    public void setRegistarskiBroj(String registarskiBroj) {
        this.registarskiBroj = registarskiBroj;
    }

    /**
     * Omogućava dobijanje reference na objekat tipa File koji čuva putanju do fotografije plovila.
     *
     * @return referenca na objekat tipa File koji čuva apstraktnu putanju do fotografije plovila.
     */
    public File getFotografija() {
        return fotografija;
    }

    /**
     * Omogućava postavljanje reference na apstraktnu putanju do datoteke koja predstavlja fotografiju plovila.
     *
     * @param fotografija Referenca na objekat tipa File koji sadrži apstraktnu putanju do fotografije plovila.
     */
    public void setFotografija(File fotografija) {
        this.fotografija = fotografija;
    }

    /**
     * Omogućava dobijanje vrijednosti trenutne brzine kretanja plovila.
     *
     * @return brzina vozila izražena kao brojna vrijednost sa pokretnim zarezom.
     */
    public double getBrzina() {
        return brzina;
    }

    /**
     * Omogućava postavljanje brzine kretanja plovila na proizvoljnu brojnu vrijednost.
     *
     * @param brzina Vrijednost na koju se postavlja brzina plovila.
     */
    public void setBrzina(double brzina) {
        this.brzina = brzina;
    }

    /**
     * Omogućava dobijanje vrijednosti prioriteta datog plovila. Niža vrijednost označava viši prioritet (relativno ostalim).
     *
     * @return prioritet plovila.
     * */
    public int getPrioritet() {
        return prioritet;
    }

    /**
     * Redefinisana metoda toString() koja omogućava dobijanje reference na objekat tipa String koji sadrži IMO broj i naziv vozila.
     *
     * @return Referenca na objekat tipa String koji sadrži IMO broj i naziv plovila.
     */
    @Override
    public String toString() {
        return String.format("[%s] %s", this.imoBroj, this.naziv);
    }

    /**
     * Dva plovila su jednaka ako imaju isti IMO broj, jer on je jedinstveni međunarodni identifikator
     * plovila (M1) i jedini prirodan ključ identiteta plovila (u suštini, primarni ključ).
     *
     * @param o Objekat sa kojim se tekući objekat poredi.
     * @return true ako su oba objekta koja se porede tipovi plovila i imaju isti, ne-{@code null} IMO broj.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Plovilo drugi)) {
            return false;
        }
        return imoBroj != null && imoBroj.equals(drugi.imoBroj);
    }

    /**
     *
     * Pošto je IMO broj jedina zagarantovana jedinstvena vrijednost između svih plovila u sistemu, redefinicija metode hashCode() zavisi isključivo od IMO broja plovila, kao i redefinisana equals() metoda.
     *
     * @return Hash plovila
     */
    @Override
    public int hashCode() {
        return imoBroj == null ? 0 : imoBroj.hashCode();
    }
}
