package org.unibl.etf.pj2.luka.simulation;

import org.unibl.etf.pj2.luka.model.classes.Dok;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.model.interfaces.Carina;
import org.unibl.etf.pj2.luka.model.interfaces.ObalskaStraza;
import org.unibl.etf.pj2.luka.model.interfaces.SluzbenoPlovilo;
import org.unibl.etf.pj2.luka.model.interfaces.Vatrogasci;
import org.unibl.etf.pj2.luka.util.LoggerUtil;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Nit koja u pozadini vodi kompletan uviđaj jednog sudara: blokira saobraćaj na terminalu gdje se
 * sudar desio, poziva najbliže patrole tri službe (vatrogasci, obalska straža, carina), čeka
 * njihov dolazak, "obavlja" uviđaj u trajanju od nekoliko sekundi, upisuje zapis o incidentu, i na
 * kraju vraća saobraćaj i patrole u normalno stanje.
 *
 * <p>Pokreće je {@link BrodThread} čim detektuje sudar tokom preticanja, kao zasebna daemon nit,
 * tako da nit plovila koje je sudar prijavilo ne mora čekati da se cijeli uviđaj završi.</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see BrodThread
 * @see Incident
 */
public class KoordinatorUvidjaja implements Runnable {

    /** Koliko često se provjerava da li su pozvane patrole stigle do mjesta incidenta, u milisekundama. */
    public static volatile long INTERVAL_PROVJERE_DOLASKA_MS = 100L;

    /** Najduže vrijeme koje se čeka da pozvane patrole stignu do mjesta incidenta prije nego što uviđaj počne bez njih, u milisekundama. */
    public static volatile long MAX_CEKANJE_DOLASKA_MS = 15000L;

    /** Luka u kojoj se incident dogodio. */
    private final Luka luka;

    /** Terminal na kojem se incident dogodio i koji ova nit blokira za vrijeme uviđaja. */
    private final Terminal terminal;

    /** Plovila koja su učestvovala u sudaru. */
    private final List<Plovilo> ucesniciSudara;

    /** X koordinata polja na kojem se sudar dogodio. */
    private final int incidentX;

    /** Y koordinata polja na kojem se sudar dogodio. */
    private final int incidentY;

    /** Direktorijum u koji se upisuje zapis o incidentu, ili {@code null} za podrazumijevani. */
    private final File direktorijumIncidenta;

    /**
     * Kreira koordinatora koji će zapis o incidentu upisati u podrazumijevani direktorijum.
     *
     * @param luka Luka u kojoj se incident dogodio.
     * @param terminal Terminal na kojem se incident dogodio.
     * @param ucesniciSudara Plovila koja su učestvovala u sudaru.
     * @param incidentX X koordinata polja na kojem se sudar dogodio.
     * @param incidentY Y koordinata polja na kojem se sudar dogodio.
     */
    public KoordinatorUvidjaja(Luka luka, Terminal terminal, List<Plovilo> ucesniciSudara,
                                int incidentX, int incidentY) {
        this(luka, terminal, ucesniciSudara, incidentX, incidentY, null);
    }

    /**
     * Kreira koordinatora koji će zapis o incidentu upisati u zadati direktorijum, umjesto u
     * podrazumijevani.
     *
     * @param luka Luka u kojoj se incident dogodio.
     * @param terminal Terminal na kojem se incident dogodio.
     * @param ucesniciSudara Plovila koja su učestvovala u sudaru.
     * @param incidentX X koordinata polja na kojem se sudar dogodio.
     * @param incidentY Y koordinata polja na kojem se sudar dogodio.
     * @param direktorijumIncidenta Direktorijum u koji se upisuje zapis o incidentu.
     */
    public KoordinatorUvidjaja(Luka luka, Terminal terminal, List<Plovilo> ucesniciSudara,
                                int incidentX, int incidentY, File direktorijumIncidenta) {
        this.luka = luka;
        this.terminal = terminal;
        this.ucesniciSudara = new ArrayList<>(ucesniciSudara);
        this.incidentX = incidentX;
        this.incidentY = incidentY;
        this.direktorijumIncidenta = direktorijumIncidenta;
    }

    /**
     * Vodi cijeli tok uviđaja: blokira saobraćaj na terminalu, poziva patrole i čeka njihov
     * dolazak, "obavlja" uviđaj u trajanju izračunatom preko {@link #trajanjeUvidjaja()}, upisuje
     * zapis o incidentu, a zatim u svakom slučaju (uspješno ili prekinuto) skida blokadu saobraćaja
     * i vraća patrole u normalno stanje.
     */
    @Override
    public void run() {
        terminal.blokirajSaobracaj();
        List<BrodThread> odazvane = new ArrayList<>();
        try {
            odazvane = pozoviPatrole();
            sacekajDolazakPatrola(odazvane);

            long trajanje = trajanjeUvidjaja();
            Thread.sleep(trajanje);

            Incident incident = new Incident(ucesniciSudara, odazvanaPlovila(odazvane),
                    LocalDateTime.now(), trajanje, terminal.getIdTerminala());
            if (direktorijumIncidenta != null) {
                incident.sacuvaj(direktorijumIncidenta);
            } else {
                incident.sacuvaj();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } finally {
            oznaciUcesnikeSudaraZaNapustanje();
            terminal.odblokirajSaobracaj();
            raspetljajPatrole(odazvane);
            for (BrodThread patrola : odazvane) {
                if (patrola.getPlovilo() instanceof SluzbenoPlovilo sluzbeno) {
                    sluzbeno.setRotacija(false);
                }
            }
        }
    }

    /**
     * Pronalazi niti svih učesnika sudara koje su trenutno aktivne i obilježava ih da moraju
     * napustiti terminal umjesto da se privežu, preko {@link BrodThread#oznaciKaoUcesnikaSudara()}.
     */
    private void oznaciUcesnikeSudaraZaNapustanje() {
        for (Plovilo ucesnik : ucesniciSudara) {
            BrodThread nit = pronadjiNit(ucesnik);
            if (nit != null) {
                nit.oznaciKaoUcesnikaSudara();
            }
        }
    }

    /**
     * Pronalazi aktivnu nit koja upravlja zadatim plovilom, tražeći po registru aktivnih plovila
     * luke.
     *
     * @param p Plovilo čija se nit traži.
     * @return Nit koja upravlja plovilom {@code p}, ili {@code null} ako trenutno nije aktivno.
     */
    private BrodThread pronadjiNit(Plovilo p) {
        for (BrodThread kandidat : luka.getAktivnaPlovila()) {
            if (kandidat.getPlovilo() == p) {
                return kandidat;
            }
        }
        return null;
    }

    /**
     * Vraća sve odazvane patrole u normalno stanje nakon uviđaja: patrola koja je stigla na
     * terminal se ponovo privezuje na slobodan dok (ili napušta terminal ako nema slobodnog),
     * a patrola koja uopšte nije stigla na vrijeme je jednostavno oslobođena od zadatka bez
     * dodjele novog doka.
     *
     * @param odazvane Patrole koje su bile pozvane na ovaj incident.
     */
    private void raspetljajPatrole(List<BrodThread> odazvane) {
        for (BrodThread patrola : odazvane) {
            if (patrola.getTrenutniTerminal() != terminal) {
                LoggerUtil.logWarning("Patrola " + patrola.getPlovilo().getImoBroj()
                        + " nije stigla na incident, ne dobija vez u terminalu "
                        + terminal.getIdTerminala() + ".");
                patrola.zavrsiUvidjaj(null);
                continue;
            }
            Dok noviDok = terminal.rezervisiSlobodanDok(patrola.getPlovilo());
            if (!patrola.zavrsiUvidjaj(noviDok) && noviDok != null) {
                terminal.otkaziRezervaciju(noviDok);
                LoggerUtil.logWarning("Patrola " + patrola.getPlovilo().getImoBroj()
                        + " je odustala prije kraja uviđaja, rezervacija veza otkazana.");
            }
        }
    }

    /**
     * Poziva najbližu patrolu svake od tri službe (vatrogasci, obalska straža, carina) na mjesto
     * incidenta, po jednu po službi ako je dostupna.
     *
     * @return Lista svih patrola koje su uspješno pozvane, može biti prazna ako nijedna služba
     *         trenutno nema dostupno plovilo.
     */
    private List<BrodThread> pozoviPatrole() {
        List<BrodThread> odazvane = new ArrayList<>();
        dodajAkoPostoji(odazvane, PretragaPatrole.najblizaPatrola(luka, terminal, incidentX, incidentY, Vatrogasci.class));
        dodajAkoPostoji(odazvane, PretragaPatrole.najblizaPatrola(luka, terminal, incidentX, incidentY, ObalskaStraza.class));
        dodajAkoPostoji(odazvane, PretragaPatrole.najblizaPatrola(luka, terminal, incidentX, incidentY, Carina.class));
        return odazvane;
    }

    /**
     * Ako je pronađena patrola tražene službe, uključuje joj rotaciju i šalje je ka polju odmah
     * pored mjesta incidenta (ne na samo mjesto incidenta, jer je to polje zauzeto učesnicima
     * sudara), pa je dodaje na listu odazvanih patrola. Bez efekta, osim upisa upozorenja u log,
     * ako je {@code patrola null}, tj. ako služba trenutno nema dostupno plovilo.
     *
     * @param odazvane Lista odazvanih patrola u koju se nova patrola dodaje.
     * @param patrola Pronađena patrola tražene službe, ili {@code null} ako nema dostupne.
     */
    private void dodajAkoPostoji(List<BrodThread> odazvane, BrodThread patrola) {
        if (patrola == null) {
            LoggerUtil.logWarning("Nema dostupne patrole trazene sluzbe za incident u terminalu "
                    + terminal.getIdTerminala() + ".");
            return;
        }
        if (patrola.getPlovilo() instanceof SluzbenoPlovilo sluzbeno) {
            sluzbeno.setRotacija(true);
        }
        int ciljnoY = incidentY > 0 ? incidentY - 1 : incidentY + 1;
        patrola.pozoviNaIncident(terminal, incidentX, ciljnoY);
        odazvane.add(patrola);
    }

    /**
     * Čeka da sve pozvane patrole stignu do mjesta incidenta, provjeravajući periodično na svakih
     * {@link #INTERVAL_PROVJERE_DOLASKA_MS}, ali ne duže od {@link #MAX_CEKANJE_DOLASKA_MS} ukupno
     * — patrola koja do tada ne stigne se jednostavno preskače, uviđaj počinje bez nje.
     *
     * @param odazvane Patrole čiji se dolazak čeka.
     * @throws InterruptedException Ako je nit prekinuta tokom čekanja.
     */
    private void sacekajDolazakPatrola(List<BrodThread> odazvane) throws InterruptedException {
        long krajnjeVrijeme = System.currentTimeMillis() + MAX_CEKANJE_DOLASKA_MS;
        while (!sveStigle(odazvane) && System.currentTimeMillis() < krajnjeVrijeme) {
            Thread.sleep(INTERVAL_PROVJERE_DOLASKA_MS);
        }
    }

    /**
     * Provjerava da li su sve pozvane patrole stigle pored mjesta incidenta.
     *
     * @param odazvane Patrole čiji se dolazak provjerava.
     * @return {@code true} ako je svaka patrola iz liste stigla, po pravilu iz {@link #stiglaPored(BrodThread)}.
     */
    private boolean sveStigle(List<BrodThread> odazvane) {
        for (BrodThread patrola : odazvane) {
            if (!stiglaPored(patrola)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Provjerava da li je jedna patrola stigla dovoljno blizu mjesta incidenta da uviđaj može
     * početi — mora biti na istom terminalu, obavljati zadatak dolaska na incident, i biti
     * udaljena najviše jedno polje od tačne ciljne ćelije.
     *
     * @param patrola Patrola čiji se dolazak provjerava.
     * @return {@code true} ako je patrola stigla pored mjesta incidenta.
     */
    private boolean stiglaPored(BrodThread patrola) {
        if (patrola.getTrenutniTerminal() != terminal) {
            return false;
        }
        // Ne samo fizička pozicija, patrola mora stvarno biti u Zadatak.NA_INCIDENTU, inače
        // zavrsiUvidjaj() (koji upravo tu vrijednost provjerava kao svoj guard) može stići prije
        // nego što nit patrole izvrši taj prelaz (pozicija se ažurira nekoliko instrukcija ranije
        // nego zadatak), pa bi signal bio nečujno odbačen i patrola bi zauvijek čekala.
        if (patrola.getZadatak() != Zadatak.NA_INCIDENTU) {
            return false;
        }
        int px = patrola.getX();
        int py = patrola.getY();
        if (px < 0 || py < 0) {
            return false;
        }
        return Math.abs(px - incidentX) + Math.abs(py - incidentY) <= 1;
    }

    /**
     * Izračunava slučajno trajanje uviđaja, ravnomjerno raspoređeno između
     * {@link BrodThread#MIN_TRAJANJE_UVIDJAJA_MS} i {@link BrodThread#MAX_TRAJANJE_UVIDJAJA_MS}.
     *
     * @return Trajanje uviđaja, u milisekundama.
     */
    private long trajanjeUvidjaja() {
        long min = BrodThread.MIN_TRAJANJE_UVIDJAJA_MS;
        long max = BrodThread.MAX_TRAJANJE_UVIDJAJA_MS;
        if (max <= min) {
            return min;
        }
        return min + ThreadLocalRandom.current().nextLong(max - min + 1);
    }

    /**
     * Izvlači plovila iz liste odazvanih niti patrola, za upis u zapis o incidentu.
     *
     * @param odazvane Niti odazvanih patrola.
     * @return Lista plovila kojima te niti upravljaju.
     */
    private List<Plovilo> odazvanaPlovila(List<BrodThread> odazvane) {
        List<Plovilo> plovila = new ArrayList<>();
        for (BrodThread patrola : odazvane) {
            plovila.add(patrola.getPlovilo());
        }
        return plovila;
    }
}
