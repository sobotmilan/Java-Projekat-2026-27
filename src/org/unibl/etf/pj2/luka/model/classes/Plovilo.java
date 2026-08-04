package org.unibl.etf.pj2.luka.model.classes;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;

/**
 * Apstraktna klasa koja modeluje sve osnovne karakteristike i ponašanja svakog pomorskog plovila u sistemu.
 * Sadrži atribute koji su definisani kao zajednički za sva plovila poput IMO broja, naziva, broja motora, registarskih tablica, brzine itd.
 * Implementira {@link Serializable} interfejs kako bi se omogućilo dugoročno čuvanje stanja plovila kroz više sesija simulacije.
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
     * Konstruktor kojim se ručno inicijalizuju svi osnovni atributi plovila osim brzine, koja se inicijalizuje kao slučajna brojna vrijednost sa pokretnim zarezom.
     *
     * @param naziv Naziv broda.
     * @param imoBroj Identifikacioni broj plovila u međunarodnom saobraćaju.
     * @param brojMotora Serijski broj motora.
     * @param registarskiBroj Registarska oznaka plovila.
     * @param fotografija Referenca na objekat tipa File koji čuva apstraktnu putanju do binarne datoteke koja predstavlja fotografiju plovila.
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
     * Omogućava dobijanje reference na objekat tipa String koji čuva naziv plovila.
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
     * Omogućava dobijanje reference na objekat tipa String koji predstavlja identifikator plovila u međunarodnom saobraćaju.
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
     * ulaska), promjena IMO broja ovim setterom ga "gubi" u toj kolekciji — ostaje u starom bucket-u
     * i više se ne pronalazi po novom ključu. Plovilo prije izmjene ukloniti iz takvih kolekcija i
     * ponovo ga dodati poslije.
     *
     * @param imoBroj Identifikator plovila u međunarodnom saobraćaju.
     */
    public void setImoBroj(String imoBroj) {
        this.imoBroj = imoBroj;
    }

    /**
     * Omogućava dobijanje reference na objekat tipa String koji sadrži serijski broj motora plovila.
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
     * Omogućava dobijanje reference na objekat tipa String koji sadrži registarsku oznaka plovila.
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
     * Redefinisana metoda toString() iz klase {@link java.lang.Object} koja omogućava dobijanje reference na objekat tipa String koji sadrži IMO broj i naziv vozila.
     *
     * @return Referenca na objekat tipa String koji sadrži IMO broj i naziv plovila.
     */
    @Override
    public String toString() {
        return String.format("[%s] %s", this.imoBroj, this.naziv);
    }

    /**
     * Dva plovila su jednaka ako imaju isti IMO broj — on je jedinstveni međunarodni identifikator
     * plovila (M1) i jedini prirodan ključ identiteta. Vidi napomenu na {@link #setImoBroj(String)}
     * o posljedicama promjene IMO broja nad plovilom koje je već u hash-baziranoj kolekciji.
     *
     * @param o Objekat sa kojim se poredi.
     * @return true ako su oba plovila i imaju isti, ne-null IMO broj.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Plovilo)) {
            return false;
        }
        Plovilo drugi = (Plovilo) o;
        return imoBroj != null && imoBroj.equals(drugi.imoBroj);
    }

    @Override
    public int hashCode() {
        return imoBroj == null ? 0 : imoBroj.hashCode();
    }
}
