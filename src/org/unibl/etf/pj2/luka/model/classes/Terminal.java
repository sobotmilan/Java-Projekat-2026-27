package org.unibl.etf.pj2.luka.model.classes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.unibl.etf.pj2.luka.model.classes.Polje;
import org.unibl.etf.pj2.luka.model.interfaces.SluzbenoPlovilo;

/**
 * Jedan terminal luke: matrica polja dimenzija 4×17 sa 30 vezova (dokova) i dvotračnim
 * plovnim kanalom kroz sredinu.
 *
 * <p><b>Raspored matrice</b> (red, kolona), po uzoru na šematski prikaz iz specifikacije:</p>
 * <ul>
 *     <li>Kolone 0 i 1 su ulazna/izlazna kolona (oznake {@code v}/{@code ^}) kroz koju plovilo
 *     silazi sa/izlazi na gornju granicu terminala prema kanalu.</li>
 *     <li>Red 0 i red 3 su redovi dokova (30 vezova, po 15 sa svake strane) — {@link Dok}.</li>
 *     <li>Red {@link #KANAL_ULAZ} (2) je istočni (dolazni) trak kanala, red {@link #KANAL_IZLAZ}
 *     (1) je zapadni (odlazni/trak za preticanje) trak kanala — plovidba desnom stranom kanala.
 *     Ulazak u dok je pomjeraj za jedan red gore/dolje sa kanala, nikad kretanje kroz
 *     redove dokova (ranija verzija je greškom vodila plovila kroz red 3).</li>
 * </ul>
 *
 * <p><b>Rezervacija doka:</b> {@link #rezervisiSlobodanDok(Plovilo)} pronalazi i
 * zauzima slobodan dok u jednoj atomarnoj {@code synchronized} operaciji, umjesto da to budu dva
 * odvojena koraka — time se sprečava trka u kojoj dva broda "pronađu" isti slobodan dok prije
 * nego što ijedan stigne da ga zauzme.</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see Dok
 * @see Polje
 */
public class Terminal implements Serializable {
    private static final long serialVersionUID;

    /** Matrica polja terminala, dimenzija 4×17. */
    private final Polje[][] matrica;

    /** Svi vezovi (dokovi) terminala, 30 ukupno — po 15 u redu 0 i redu 3. */
    private final List<Dok> dokovi;

    /** Redni broj terminala unutar luke. */
    private final int idTerminala;

    /** Red matrice koji predstavlja istočni (dolazni) trak plovnog kanala. */
    public static final int KANAL_ULAZ = 2;

    /** Red matrice koji predstavlja zapadni (odlazni, i trak za preticanje) trak plovnog kanala. */
    public static final int KANAL_IZLAZ = 1;

    /** Kolona kroz koju plovilo ulazi u terminal sa gornje/lijeve strane. */
    public static final int KOLONA_ULAZ = 0;

    /** Kolona kroz koju plovilo napušta terminal ka izlazu/narednom terminalu. */
    public static final int KOLONA_IZLAZ = 1;

    /**
     * Redni brojevi vezova koji su trenutno rezervisani (dodijeljeni plovilu koje je još u
     * kanalu na putu ka doku, prije nego što ga fizički zauzme). Odvojeno od {@link Dok#isSlobodan()}
     * jer se fizičko zauzimanje ćelije matrice dešava tek kad plovilo stvarno stigne do veza —
     * rezervacija sprečava da drugo plovilo u međuvremenu krene ka istom, još praznom, vezu.
     */
    private transient java.util.Set<Integer> rezervisaniVezovi;

    /**
     * Da li je saobraćaj na ovom terminalu trenutno blokiran zbog uviđaja incidenta.
     * {@code transient} jer je ovo prolazno stanje trajanja simulacije, ne dio trajnog stanja
     * luke koje se čuva u {@code luka.ser} — blokada koja postoji u trenutku gašenja JVM-a nema
     * smisla nakon ponovnog pokretanja (uviđaj koji ju je izazvao više ne postoji). {@code volatile}
     * jer ga čita svaka nit broda pri svakom pokušaju pomjeranja, a postavlja/skida ga nit koja vodi
     * uviđaj — bez {@code synchronized}, jer je ovo samo zastavica za čitanje/pisanje jedne
     * {@code boolean} vrijednosti, ne složena operacija koja zahtijeva atomarnost sa nečim drugim.
     */
    private transient volatile boolean saobracajBlokiran;

    static{
        serialVersionUID = 1L;
    }

    {
        this.matrica = new Polje[4][17];
        this.dokovi = new ArrayList<>();
    }

    /**
     * Kreira terminal sa zadatim identifikatorom i inicijalizuje njegovu matricu (kanal, dokovi,
     * ulazna/izlazna kolona).
     *
     * @param idTerminala Redni broj terminala unutar luke.
     */
    public Terminal(int idTerminala) {
        this.idTerminala = idTerminala;
        initializeMatrix();
    }

    /**
     * Omogućava dobijanje matrice polja terminala.
     *
     * @return Matrica polja dimenzija 4×17.
     */
    public Polje[][] getMatrica() {
        return matrica;
    }

    /**
     * Omogućava dobijanje liste svih vezova (dokova) terminala.
     *
     * @return Lista vezova terminala.
     */
    public List<Dok> getDokovi() {
        return dokovi;
    }

    /**
     * Omogućava dobijanje rednog broja terminala unutar luke.
     *
     * @return Identifikator terminala.
     */
    public int getIdTerminala() {
        return idTerminala;
    }


    /**
     * Broji vezove koji trenutno nemaju privezano plovilo. Ne uzima u obzir rezervacije u
     * toku — za tu svrhu koristiti {@link #getBrojRaspolozivihVezova()}.
     *
     * @return Broj fizički slobodnih vezova terminala.
     */
    public int getBrojSlobodnihVezova() {
        int counter = 0;
        for(Dok d: dokovi) {
            if(d.isSlobodan()) {
                counter++;
            }
        }
        return counter;
    }

    /**
     * Broji vezove koji su i fizički slobodni i trenutno nisu rezervisani od strane nekog drugog
     * plovila u tranzitu ka njima. Ovo je vrijednost koju treba koristiti prilikom odlučivanja
     * da li terminal ima mjesta za novo plovilo, jer {@link #getBrojSlobodnihVezova()}
     * ne vidi rezervacije u toku.
     *
     * @return Broj stvarno raspoloživih (slobodnih i nerezervisanih) vezova terminala.
     */
    public synchronized int getBrojRaspolozivihVezova() {
        int counter = 0;
        for (Dok d : dokovi) {
            if (d.isSlobodan() && !rezervisani().contains(d.getOznakaVezova())) {
                counter++;
            }
        }
        return counter;
    }


    private java.util.Set<Integer> rezervisani() {
        if (rezervisaniVezovi == null) {
            rezervisaniVezovi = new java.util.HashSet<>();
        }
        return rezervisaniVezovi;
    }

    /**
     * Pronalazi slobodan, nerezervisan vez i odmah ga rezerviše za dato plovilo — u jednoj
     * atomarnoj {@code synchronized} operaciji, čime se sprečava trka: bez
     * atomarnosti, dva plovila koja istovremeno traže slobodan dok mogu oba "pronaći" isti dok
     * prije nego što ijedno stigne da ga zauzme.
     *
     * @param p Plovilo za koje se traži i rezerviše vez.
     * @return Rezervisani {@link Dok}, ili {@code null} ako nema slobodnog nerezervisanog veza
     *         (ili je {@code p} {@code null}).
     */
    public synchronized Dok rezervisiSlobodanDok(Plovilo p) {
        if (p == null) {
            return null;
        }
        for (Dok d : dokovi) {
            if (d.isSlobodan() && !rezervisani().contains(d.getOznakaVezova())) {
                rezervisani().add(d.getOznakaVezova());
                return d;
            }
        }
        return null;
    }

    /**
     * Otkazuje prethodno napravljenu rezervaciju veza (npr. ako plovilo ne uspije stići do njega
     * u razumnom broju pokušaja), oslobađajući ga za druga plovila.
     *
     * @param d Vez čija se rezervacija otkazuje. Ignoriše se ako je {@code null}.
     */
    public synchronized void otkaziRezervaciju(Dok d) {
        if (d != null) {
            rezervisani().remove(d.getOznakaVezova());
        }
    }

    /**
     * Blokira saobraćaj na ovom terminalu — poziva se kad počne uviđaj incidenta. Nakon ovog
     * poziva {@link #smijeProci(Plovilo)} propušta samo plovila pod aktivnom rotacijom (službena
     * plovila koja idu ka mjestu incidenta), sva ostala plovila u ovom terminalu stoje u mjestu.
     * Ostali terminali luke nisu pogođeni — blokada je po instanci {@code Terminal}-a.
     *
     * <p>Samo postavlja zastavicu, ne čeka niti uspavljuje pozivaoca — sama logika trajanja
     * uviđaja i njegov redoslijed su predmet dispečovanja/koordinacije uviđaja, ne ove metode.</p>
     */
    public void blokirajSaobracaj() {
        this.saobracajBlokiran = true;
    }

    /**
     * Skida blokadu saobraćaja postavljenu preko {@link #blokirajSaobracaj()} — poziva se kad se
     * uviđaj završi, nakon čega {@link #smijeProci(Plovilo)} ponovo propušta svako plovilo.
     */
    public void odblokirajSaobracaj() {
        this.saobracajBlokiran = false;
    }

    /**
     * Provjerava da li je saobraćaj na ovom terminalu trenutno blokiran.
     *
     * @return {@code true} ako je terminal trenutno pod blokadom (uviđaj u toku).
     */
    public boolean isSaobracajBlokiran() {
        return saobracajBlokiran;
    }

    /**
     * Provjerava da li dato plovilo smije da se pomjeri unutar ovog terminala u trenutnom stanju
     * saobraćaja. Ako terminal nije blokiran, prolaze sva plovila. Ako jeste, prolaze samo
     * plovila pod aktivnom rotacijom — službena plovila (obalska straža/carina/vatrogasci) koja su
     * upravo pozvana na mjesto incidenta ({@link SluzbenoPlovilo#isRotacija()}).
     *
     * <p>Poziva se iz pokreta broda ({@code BrodThread.pomjeriNaPolje()}) prije svakog fizičkog
     * pomjeranja — samo čita {@link #saobracajBlokiran}, ne ulazi u {@code synchronized(this)}
     * blok i ne čeka ni na čemu, pa se poziv smije nalaziti bilo gdje u putanji kretanja bez rizika
     * po {@link org.unibl.etf.pj2.luka.view.PrikazTerminala#render(Terminal)}.</p>
     *
     * @param p Plovilo za koje se provjerava da li smije da se pomjeri.
     * @return {@code true} ako plovilo smije da se pomjeri, {@code false} ako je terminal blokiran
     *         i plovilo nije pod aktivnom rotacijom.
     */
    public boolean smijeProci(Plovilo p) {
        if (!saobracajBlokiran) {
            return true;
        }
        return p instanceof SluzbenoPlovilo sluzbeno && sluzbeno.isRotacija();
    }

    /**
     * Popunjava matricu terminala: ulazna/izlazna kolona (0/1), dokovi u redovima 0 i 3 (30
     * ukupno), i strelice plovnog kanala u redovima {@link #KANAL_IZLAZ}/{@link #KANAL_ULAZ}.
     */
    private void initializeMatrix() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 17; j++) {
                matrica[i][j] = new Polje(i, j, "", null);
            }
        }

        for (int i = 0; i < 4; i++) {
            matrica[i][0].setOznaka("v");
            matrica[i][1].setOznaka("^");
        }

        int vezCounter = 1;
        for (int j = 2; j < 17; j++) {
            matrica[0][j].setOznaka("D");
            Dok d1 = new Dok(matrica[0][j], vezCounter++);
            dokovi.add(d1);

            matrica[3][j].setOznaka("D");
            Dok d2 = new Dok(matrica[3][j], vezCounter++);
            dokovi.add(d2);
        }

        for (int j = 2; j < 17; j++) {
            matrica[KANAL_IZLAZ][j].setOznaka("<-");
            matrica[KANAL_ULAZ][j].setOznaka("->");
        }
    }
}
