package org.unibl.etf.pj2.luka.simulation;

import org.unibl.etf.pj2.luka.model.classes.Dok;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Polje;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.model.interfaces.ObalskaStraza;
import org.unibl.etf.pj2.luka.model.interfaces.SluzbenoPlovilo;
import org.unibl.etf.pj2.luka.util.LoggerUtil;
import org.unibl.etf.pj2.luka.util.PokretacIzvjestaja;
import org.unibl.etf.pj2.luka.util.SpisakPotjeraUtil;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Klasa koja predstavlja nit koja simulira kretanje jednog objekta klase {@link Plovilo} kroz luku.
 *
 * <p>Logika kretanja: ulazak kroz kanal terminala,
 * vezivanje na dok, parkirano čekanje dok je privezan, i konačno napuštanje terminala.</p>
 *
 * <p>Plovilo silazi niz ulaznu kolonu (prva kolona, iliti {@link Terminal#KOLONA_ULAZ}) do
 * reda {@link Terminal#KANAL_ULAZ} (istočni), plovi njime ka istoku, a ulazak u dok
 * je pomjeraj za jedan red gore/dolje sa kanala, nikad kretanje kroz redove dokova (0. red i 3). Red
 * {@link Terminal#KANAL_IZLAZ} služi i za preticanje i za izlazak iz terminala.</p>
 *
 * <p>Plovilo pod rotacijom (pri čemu mu je prioritet manji od {@link #PRIORITET_BEZ_ROTACIJE})
 * pokušava preticanje čim je blokirano,
 * umjesto da čeka {@link #PRAG_PRETICANJA} neuspjeha kao obično plovilo,
 * obično plovilo dodatno provjerava {@link #ustupaProlaz(Terminal, int, int, Plovilo)}
 * i stoji u mjestu ako je plovilo pod rotacijom neposredno iza njega u istoj traci.</p>
 *
 * <p>Nit se ne gasi kad se plovilo priveže, već ulazi u
 * {@link Zadatak#PRIVEZAN} i parkira se preko {@link #cekajNapustanje()} na posebnom
 * {@link #parkLock} objektu (nikad na {@code synchronized(terminal)}), sve dok je neko ne
 * pozove preko {@link #zatraziNapustanje()} da napusti terminal.</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see Terminal
 * @see Zadatak
 * @see Luka
 */
public class BrodThread implements Runnable {
    /** Maksimalan broj pokušaja pomjeranja za jedan korak prije nego što se pokušaj kretanja smatra neuspjesnim. */
    private static final int MAX_POKUSAJA = 100;

    /** Pauza između neuspjelih pokušaja pomjeranja, izrazeno u milisekundama. */
    private static final long CEKANJE_MS = 100L;

    /** Broj uzastopnih neuspjeha pomjeranja nakon kog obično plovilo (bez specijalnog prioriteta) pokušava preticanje. */
    private static final int PRAG_PRETICANJA = 3;

    /** Prioritet svakog plovila bez upaljene rotacije (podrazumijevana vrijednost iz {@link Plovilo} klase). */
    private static final int PRIORITET_BEZ_ROTACIJE = 10;

    /** Globalni prekidač koji omogućava ili potpuno isključuje mogućnost sudara tokom preticanja u cijeloj simulaciji. */
    public static volatile boolean SUDARI_OMOGUCENI = true;

    /** Vjerovatnoća sudara. Nije final, moguće modifikovanje vjerovatnoće sudara radi lakšeg demonstriranja funkcionalnosti vezanih za sudare. */
    public static volatile double VJEROVATNOCA_SUDARA = 0.02;

    /** Trajanje uviđaja za incident, u milisekundama. */
    public static volatile long MIN_TRAJANJE_UVIDJAJA_MS = 3000L;
    public static volatile long MAX_TRAJANJE_UVIDJAJA_MS = 10000L;

    /** Trajanje uviđaja kad je u pitanju plovilo sa potjernice, max vrijednost je kraća od opšteg incidenta. */
    public static volatile long MIN_TRAJANJE_UVIDJAJA_POTJERNICE_MS = 3000L;
    public static volatile long MAX_TRAJANJE_UVIDJAJA_POTJERNICE_MS = 5000L;

    /** Direktorijum u koji se upisuje evidencija potjernice, {@code null} znači podrazumijevano (user.home). */
    public static volatile File DIREKTORIJUM_INCIDENTA_POTJERNICE = null;

    /** Direktorijum u koji se upisuje binarni fajl obicnog incidenta, {@code null} znači podrazumijevano (user.home). */
    public static volatile File DIREKTORIJUM_INCIDENTA_SUDARA = null;

    /** Plovilo kojim ova nit upravlja. */
    private final Plovilo plovilo;

    /** Luka kojoj dato plovilo pripada, daje plovilu pristup terminalima i evidenciji ulaska. */
    private final Luka luka;

    /** Zaključavanje isključivo za parkirano čekanje (PRIVEZAN), NIKAD SE NE KORISTI synchronized(terminal), da render ne bi blokirao za privezano plovilo. */
    private final Object parkLock = new Object();

    /** Terminal u kojem se plovilo trenutno nalazi, ili {@code null} ako se ne nalazi ni u jednom. */
    private Terminal trenutniTerminal;

    /** Trenutna pozicija (red, kolona) plovila u matrici {@link #trenutniTerminal}-a, ili -1 ako nije pozicionirano. */
    private volatile int x, y;

    /** Logički podataka koji govori da li je plovilo trenutno privezano na dok. */
    private volatile boolean isPrivezan;

    /** Ukoliko je vrijednost {@link #isPrivezan} {@code true}, ovaj atribut čuva referencu na dok na koji je plovilo privezano. */
    private volatile Dok trenutniDok;

    /** Flag atribut koju postavlja {@link #zatraziNapustanje()} da probudi parkiranu nit. */
    private volatile boolean moraNapustiti;

    /** Trenutni zadatak niti. */
    private volatile Zadatak zadatak;

    /** Terminal na kojem se desio incident ka kojem je plovilo trenutno pozvano, postavlja ga {@link #pozoviNaIncident(Terminal, int, int)}. */
    private volatile Terminal ciljniTerminalIncidenta;

    /** X koordinata ciljne ćelije incidenta kojoj plovilo treba da dođe. */
    private volatile int ciljXIncidenta;

    /** Y koordinata ciljne ćelije incidenta kojoj plovilo treba da dođe. */
    private volatile int ciljYIncidenta;

    /** Dok na koji se plovilo treba privezati nakon završetka uviđaja, ili {@code null} ako nema slobodnog i plovilo mora napustiti terminal. */
    private volatile Dok dokPoUvidjaju;

    /** Postavlja se preko {@link #oznaciKaoUcesnikaSudara()} kad je plovilo učestvovalo u sudaru, nagoni plovilo da napusti terminal umjesto da se priveže. */
    private volatile boolean sudarMoraNapustiti;

    /** Postavlja se kad ovo plovilo obalske straže krene u potjeru za nekim drugim plovilom. */
    private volatile boolean naPratnji;

    /** Plovilo za kojim ova obalska straža trenutno vrši potjeru, postavlja ga {@link #pokreniPotjernicu(ObalskaStraza, Plovilo)}. */
    private volatile Plovilo trazenoPlovilo;

    /** Izvor nasumičnosti za provjeru sudara. */
    private volatile Random generatorSudara;

    {
        this.x = this.y = -1;
        this.isPrivezan = false;
        this.moraNapustiti = false;
        this.zadatak = Zadatak.KA_DOKU;
    }

    /**
     * Konstruktor za plovilo koje tek treba ući u luku kroz ulazni kanal, nit kreće iz
     * {@link Zadatak#KA_DOKU}, nepozicionirana (dakle {@code x == y == -1}).
     *
     * @param plovilo Plovilo kojim nit upravlja.
     * @param luka Luka u koju plovilo ulazi.
     */
    public BrodThread(Plovilo plovilo, Luka luka) {
        this.plovilo = plovilo;
        this.luka = luka;
    }

    /**
     * Konstruktor za plovilo koje je već fizički postavljeno na dok (početno postavljanje
     * flote pri pokretanju simulacije), umjesto da prolazi kroz kanal do njega.
     * Pozivalac je odgovoran da prethodno postavi {@code plovilo} u celiju matrice na
     * {@code dok.getLokacija()}.
     *
     * @param plovilo Plovilo koje se već nalazi na doku.
     * @param luka Luka kojoj terminal pripada.
     * @param terminal Terminal na kojem se dok nalazi.
     * @param dok Dok na kojem je plovilo već usidreno.
     */
    public BrodThread(Plovilo plovilo, Luka luka, Terminal terminal, Dok dok) {
        this(plovilo, luka);
        this.trenutniTerminal = terminal;
        this.x = dok.getLokacija().getX();
        this.y = dok.getLokacija().getY();
        this.isPrivezan = true;
        this.trenutniDok = dok;
    }

    /**
     * Životni ciklus niti: registruje se u {@link Luka#getAktivnaPlovila()}, ulazi u luku
     * (ili je već privezana upotrebom predokovanog konstruktora), potom parkira dok se ne pozove da napusti
     * terminal i napušta ga preko {@link #napustiTerminal()}. Uvijek se odjavljuje iz
     * {@link Luka#getAktivnaPlovila()} u {@code finally}, bez obzira na to kako se nit završila
     * (uspješno privezivanje, neuspjeh pri ulasku, ili prekid).
     */
    @Override
    public void run() {
        luka.getAktivnaPlovila().add(this);
        try {
            if(isPrivezan) {
                evidentirajUlazak();
            }
            boolean usidren = this.isPrivezan || udjiULuku();

            if (usidren) {
                boolean krajBoravka = false;
                while (!krajBoravka) {
                    this.zadatak = Zadatak.PRIVEZAN;
                    cekajNapustanje();

                    if (this.zadatak == Zadatak.KA_INCIDENTU) {
                        otidjiNaIncident();
                    } else if (this.zadatak == Zadatak.POD_PRATNJOM) {
                        napustiZbogPratnje();
                    }
                    if (this.zadatak == Zadatak.KA_DOKU && vratiSeNaDok()) {
                        continue;
                    }
                    krajBoravka = true;
                }
                this.zadatak = Zadatak.NAPUSTA;
                obracunajIZabiljeziTaksu();
                napustiTerminal();
            } else if (this.sudarMoraNapustiti) {
                log("Napustio luku (ucestvovao je u sudaru).");
            } else if (this.naPratnji) {
                log("Napustio luku (na pratnji potjernice).");
            } else {
                log("Napustio luku (obisao sve terminale, nema slobodnih vezova).");
            }

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LoggerUtil.logError("Kriticna greska u kretanju broda: " + plovilo.getNaziv(), e);
        } finally {
            luka.getAktivnaPlovila().remove(this);
        }
    }

    /**
     * Obilazi terminale luke redom tražeći slobodan dok: rezerviše {@link Dok} atomarno preko
     * {@link Terminal#rezervisiSlobodanDok(Plovilo)}, pokušava fizički ući u terminal i
     * doploviti do rezervisanog doka, a ako bilo koji od tih koraka ne uspije, otkazuje rezervaciju
     * i ide dalje ka narednom terminalu. Ako nijedan terminal nema bar jedan slobodan i
     * dostižan dok, plovilo napušta luku bez pristajanja na ijednom doku ijednog terminala.
     *
     * @return {@code true} ako je plovilo uspješno privezano na neki dok, a {@code false} ako je
     * bezuspješno obišlo sve terminale.
     * @throws InterruptedException Ako je nit prekinuta tokom čekanja.
     */
    private boolean udjiULuku() throws InterruptedException {
        int idx = 0;

        while (idx < luka.getTerminali().size()) {
            Terminal t = luka.getTerminali().get(idx);

            Dok rezervisan = t.rezervisiSlobodanDok(plovilo);
            if (rezervisan == null) {
                log("Terminal " + (idx + 1) + " je pun, nastavlja pravo.");
                idx++;
                continue;
            }

            if (!udjiUTerminal(t)) {
                t.otkaziRezervaciju(rezervisan);
                log("Nije uspio ući u terminal " + (idx + 1) + ", nastavlja pravo.");
                idx++;
                continue;
            }
            evidentirajUlazak();
            log("Ušao u terminal " + (idx + 1) + ".");

            if (doploviDoDoka(rezervisan)) {
                if (this.sudarMoraNapustiti) {
                    t.otkaziRezervaciju(rezervisan);
                    log("Učesnik sudara — napušta terminal " + (idx + 1) + " umjesto privezivanja.");
                    this.zadatak = Zadatak.NAPUSTA;
                    obracunajIZabiljeziTaksu();
                    napustiTerminal();
                    return false;
                }
                t.otkaziRezervaciju(rezervisan);
                this.isPrivezan = true;
                this.trenutniDok = rezervisan;
                log("Usidren na vezu " + rezervisan.getOznakaVezova()
                        + " (" + this.x + "," + this.y + ") u terminalu " + (idx + 1) + ".");
                return true;
            } else {
                t.otkaziRezervaciju(rezervisan);
                if (this.naPratnji) {
                    // pokreniPotjernicu() je već odradio cijeli izlazak (uključujući napustiTerminal())
                    // ovdje se samo prekida petlja, inače bi idx++ pokušao naredni terminal kao da je ovaj
                    // bio privremeno pun, umjesto da prihvati da je plovilo trajno napustilo luku.
                    log("Obalska straža na pratnji — napušta terminal " + (idx + 1) + ".");
                    return false;
                }
                log("Ne može doći do veza u terminalu " + (idx + 1) + ", nastavlja dalje.");
                napustiTerminal();
                idx++;
            }
        }
        return false;
    }

    /**
     * Parkira nit dok se plovilo ne pozove da napusti terminal ({@link #zatraziNapustanje()}).
     * NAPOMENA: {@code wait()} se poziva na {@link #parkLock}, nikad na terminalu, jer inače bi
     * {@code PrikazTerminala.render()}, koji uzima isti ključ, blokirao GUI za trajanje čekanja,
     * i tako za svako plovilo koje se priveže radi čekanja na datom terminalu.
     *
     * @throws InterruptedException Ako je nit prekinuta tokom čekanja.
     */
    private void cekajNapustanje() throws InterruptedException {
        synchronized (parkLock) {
            while (!moraNapustiti && zadatak != Zadatak.KA_INCIDENTU && zadatak != Zadatak.POD_PRATNJOM) {
                parkLock.wait();
            }
        }
    }

    /**
     * Budi parkiranu nit i pokreće je ka izlazu iz terminala. Može ga pozvati ili uviđaj ili
     * odlazak/dopuna nad plovilom koje je trenutno u stanju {@link Zadatak#PRIVEZAN}.
     */
    public void zatraziNapustanje() {
        synchronized (parkLock) {
            this.moraNapustiti = true;
            parkLock.notifyAll();
        }
    }

    /**
     * Poziva posmatranu nit plovila na incident koji se desio na ciljnom terminalu na koordinati (x,y).
     * Sinhronizovano na {@link #parkLock} jer je službeno plovilo privezano kad nema zadatak odlaska na mjesto incidenta radi uviđaja.
     *
     * @param ciljniTerminal terminal na kom se incident za koji se plovilo <i>dispatchuje</i> desio.
     * @param ciljX x koordinata ciljne ćelije u ciljnom terminalu.
     * @param ciljY y koordinata ciljne ćelije u ciljnom terminalu.
     */
    public void pozoviNaIncident(Terminal ciljniTerminal, int ciljX, int ciljY) {
        synchronized (parkLock) {
            if (this.zadatak != Zadatak.PRIVEZAN) {
                return;
            }
            this.ciljniTerminalIncidenta = ciljniTerminal;
            this.ciljXIncidenta = ciljX;
            this.ciljYIncidenta = ciljY;
            this.zadatak = Zadatak.KA_INCIDENTU;
            parkLock.notifyAll();
        }
    }

    /**
     * Postavlja trenutni zadatak posmatranog plovila na POD_PRATNJOM.
     */
    public void pozoviNaPratnju() {
        synchronized (parkLock) {
            if (this.zadatak != Zadatak.PRIVEZAN) {
                return;
            }
            this.zadatak = Zadatak.POD_PRATNJOM;
            parkLock.notifyAll();
        }
    }

    /**
     * Ukoliko je posmatrano plovilo učestvovalo u sudaru, poziv ove metode je obavezan kako bi se naznačilo da plovilo mora napustiti svoj trenutni terminal.
     */
    void oznaciKaoUcesnikaSudara() {
        this.sudarMoraNapustiti = true;
    }

    /**
     * Oznacava kraj uviđaja za posmatrano službeno plovila i postavlja ga na novi dok (ukoliko ima slobodnih u tekućem terminalu), ili napušta terminal.
     *
     * @param noviDok Referenca na dok na koji se posmatrano plovilo privezuje nakon završetka uviđaja, ukoliko nema slobodnih ovaj parametar je {@code null}.
     * @return {@code true} ako je uviđaj uspješno završen, {@code false} ako i dalje traje.
     */
    boolean zavrsiUvidjaj(Dok noviDok) {
        synchronized (parkLock) {
            if (this.zadatak != Zadatak.NA_INCIDENTU) {
                return false;
            }
            this.dokPoUvidjaju = noviDok;
            this.zadatak = noviDok != null ? Zadatak.KA_DOKU : Zadatak.NAPUSTA;
            parkLock.notifyAll();
            return true;
        }
    }

    /**
     * Pokušava jednokratno zauzeti ulaznu ćeliju terminala ({@code [0][KOLONA_ULAZ]}). Ako je
     * slobodna, plovilo se postavlja na nju i postaje pozicionirano u tom terminalu.
     *
     * @param terminal Terminal u koji plovilo pokušava ući.
     * @return {@code true} ako je ulazna ćelija bila slobodna i plovilo je uspješno ušlo, {@code false} u suprotnom.
     */
    public boolean pokusajUciUTerminal(Terminal terminal) {
        if (!terminal.smijeProci(this.plovilo)) {
            return false;
        }
        synchronized (terminal) {
            if (terminal.getMatrica()[0][Terminal.KOLONA_ULAZ].getTrenutnoPlovilo() == null) {
                terminal.getMatrica()[0][Terminal.KOLONA_ULAZ].setTrenutnoPlovilo(this.plovilo);
                this.trenutniTerminal = terminal;
                this.x = 0;
                this.y = Terminal.KOLONA_ULAZ;
                return true;
            }
        }
        return false;
    }

    /**
     * Ponavlja {@link #pokusajUciUTerminal(Terminal)} do {@link #MAX_POKUSAJA} puta, čekajući
     * {@link #CEKANJE_MS} između pokušaja, dok se ulazna ćelija terminala ne oslobodi.
     *
     * @param t Terminal u koji plovilo pokušava ući.
     * @return {@code true} ako je ulazak uspio u okviru dozvoljenog broja pokušaja ({@link #MAX_POKUSAJA}).
     * @throws InterruptedException Ako je nit prekinuta tokom čekanja.
     */
    private boolean udjiUTerminal(Terminal t) throws InterruptedException {
        for (int i = 0; i < MAX_POKUSAJA; i++) {
            if (pokusajUciUTerminal(t)) {
                return true;
            }
            Thread.sleep(CEKANJE_MS);
        }
        return false;
    }

    /**
     * Evidentira trenutno vrijeme kao vrijeme ulaska plovila u luku, preko
     * {@link Luka#addToEvidencija(String, java.time.LocalDateTime)}.
     */
    private void evidentirajUlazak() {
        luka.addToEvidencija(plovilo.getImoBroj(), LocalDateTime.now());
    }

    /**
     * Računa i bilježi taksu za konačan izlazak plovila iz luke, koristeći
     * {@link Luka#getEvidencijaUlaska()} kao vrijeme ulaska. Bez efekta ako plovilo nema zapis u
     * evidenciji (npr. nikad nije uspjelo ući ni u jedan terminal). U tom slučaju ova metoda ne baca izuzetak,
     * samo preskače obračun. Uklanja zapis nakon obračuna: čim je taksa obračunata, dalje čuvanje tog
     * IMO broja u evidenciji ima jedinu svrhu (sprečavanje kolizije IMO brojača), a admin GUI oslobađa IMO odmah po
     * brisanju plovila, što je ispravno ponašanje za bilo koji način na koji plovilo definitivno
     * napušta luku.
     */
    void obracunajIZabiljeziTaksu() {
        LocalDateTime vrijemeUlaska = luka.getEvidencijaUlaska().remove(plovilo.getImoBroj());
        if (vrijemeUlaska == null) {
            return;
        }
        LocalDateTime vrijemeIzlaska = LocalDateTime.now();
        double iznos = PokretacIzvjestaja.izracunajTaksuZaPlovilo(plovilo, vrijemeUlaska, vrijemeIzlaska);
        PokretacIzvjestaja.evidentirajUCSV(plovilo, vrijemeUlaska, vrijemeIzlaska, iznos);
    }

    /**
     * Vodi plovilo od ulazne ćelije terminala do rezervisanog doka.
     * Silazi do kanala (upotrebom {@link #sidjiDoKanala(long)}), plovi njime istočno do kolone doka
     * (upotrebom {@link #ploviIstocno(int, long)}), pa se pomjera na sam dok (direktno ako je dok u redu 3,
     * ili preko privremenog prolaska kroz {@link Terminal#KANAL_IZLAZ} ako je dok u redu 0,
     * ulazak u dok je uvijek pomjeraj za jedan red sa kanala, nikad kretanje kroz red dokova).
     *
     * @param cilj Rezervisani dok ka kojem plovilo plovi.
     * @return {@code true} ako je plovilo uspješno stiglo do doka, {@code false} ako je odustalo
     *         u nekom od koraka.
     * @throws InterruptedException Ako je nit prekinuta tokom čekanja.
     */
    private boolean doploviDoDoka(Dok cilj) throws InterruptedException {
        int ciljX = cilj.getLokacija().getX();
        int ciljY = cilj.getLokacija().getY();
        long korak = trajanjeKoraka();

        if (!sidjiDoKanala(korak)) {
            return false;
        }

        if (!ploviIstocno(ciljY, korak)) {
            return false;
        }

        if (ciljX == 3) {
            return pomjeriSaCekanjem(3, ciljY, korak);
        }

        if (!pomjeriSaCekanjem(Terminal.KANAL_IZLAZ, ciljY, korak)) {
            return false;
        }
        Thread.sleep(korak);
        return pomjeriSaCekanjem(0, ciljY, korak);
    }

    /**
     * Vodi plovilo niz ulaznu kolonu ({@link Terminal#KOLONA_ULAZ}), red po red, dok ne stigne do
     * reda {@link Terminal#KANAL_ULAZ}.
     *
     * @param korak Trajanje jednog koraka kretanja, u milisekundama.
     * @return {@code true} ako je plovilo uspješno stiglo do kanala, {@code false} u suprotnom.
     * @throws InterruptedException Ako je nit prekinuta tokom čekanja.
     */
    private boolean sidjiDoKanala(long korak) throws InterruptedException {
        while (this.x < Terminal.KANAL_ULAZ) {
            if (!pomjeriSaCekanjem(this.x + 1, Terminal.KOLONA_ULAZ, korak)) {
                return false;
            }
            Thread.sleep(korak);
        }
        return true;
    }

    /**
     * Plovi istočnim trakom kanala ({@link Terminal#KANAL_ULAZ}) do zadate kolone, primjenjujući
     * pravila prioriteta i preticanja na svakom koraku:
     * <p>
     *
     * -Prije pomjeranja naprijed, provjerava {@link #ustupaProlaz(Terminal, int, int, Plovilo)}
     * ako plovilo pod rotacijom stoji neposredno iza, tada obično plovilo u ovom koraku stoji u mjestu.<br>
     *
     * -Ako pomjeraj naprijed nije moguć (npr. blokirano), preticanje se pokušava preko
     * {@link Terminal#KANAL_IZLAZ} čim je ispunjen prag: odmah za plovilo pod rotacijom
     * (prioritet ispod {@link #PRIORITET_BEZ_ROTACIJE}), ili nakon {@link #PRAG_PRETICANJA}
     * uzastopnih neuspjeha za obično plovilo.<br>
     *
     * -Plovilo koje trenutno pretiče (nalazi se u {@link Terminal#KANAL_IZLAZ}) pomjera se
     * naprijed pa se odmah vraća u svoju traku čim to postane moguće.<br>
     *
     * </p>
     *
     * Prioritet se čita iznova na početku svake iteracije (ne kešira se prije petlje), tako da
     * plovilo kojem bi se upalila rotacija usred tranzita odmah dobija veći prioritet.
     *
     * @param ciljY Kolona do koje plovilo treba doploviti.
     * @param korak Trajanje jednog koraka kretanja, u milisekundama.
     * @return {@code true} ako je plovilo stiglo do ciljne kolone, {@code false} ako je premašen
     *         maksimalan broj pokušaja.
     * @throws InterruptedException Ako je nit prekinuta tokom čekanja.
     */
    private boolean ploviIstocno(int ciljY, long korak) throws InterruptedException {
        int neuspjesi = 0;
        int ukupnoPokusaja = 0;
        int blokada = 0;

        while (this.y < ciljY) {
            if (cekaZbogBlokade()) {
                if (++blokada > maxBlokadaPokusaja()) {
                    odustajemZbogBlokade();
                    return false;
                }
                Thread.sleep(CEKANJE_MS);
                continue;
            }
            blokada = 0;

            if (++ukupnoPokusaja > MAX_POKUSAJA * 4) {
                return false;
            }

            boolean imamPrioritet = plovilo.getPrioritet() < PRIORITET_BEZ_ROTACIJE;
            boolean pomjeren = false;
            boolean preticanje = false;

            if (this.x == Terminal.KANAL_ULAZ) {
                boolean moraUstupitiProlaz = ustupaProlaz(this.trenutniTerminal, this.x, this.y, this.plovilo);

                if (!moraUstupitiProlaz) {
                    pomjeren = pomjeriNaPolje(Terminal.KANAL_ULAZ, this.y + 1);
                }

                boolean pragZaPreticanjeIspunjen = imamPrioritet || neuspjesi >= PRAG_PRETICANJA;
                if (!pomjeren && pragZaPreticanjeIspunjen && smijePreticati(this.y + 1)) {
                    pomjeren = pomjeriNaPolje(Terminal.KANAL_IZLAZ, this.y);
                    preticanje = pomjeren;
                    if (pomjeren) {
                        log("Zapocinje preticanje" + (imamPrioritet ? " (prioritet pod rotacijom)." : "."));
                    }
                }
            } else {
                pomjeren = pomjeriNaPolje(Terminal.KANAL_IZLAZ, this.y + 1);
                preticanje = pomjeren;
                if (pomjeren) {
                    Thread.sleep(korak);
                    pomjeriNaPolje(Terminal.KANAL_ULAZ, this.y);
                }
            }

            if (pomjeren) {
                neuspjesi = 0;
                if (preticanje) {
                    Plovilo[] ucesniciSudara = provjeriSudar();
                    if (ucesniciSudara != null) {
                        pokreniUvidjaj(ucesniciSudara);
                    }
                }
                Plovilo trazeno = provjeriPotjernicu();
                if (trazeno != null) {
                    pokreniPotjernicu((ObalskaStraza) this.plovilo, trazeno);
                    return false;
                }
                Thread.sleep(korak);
            } else {
                neuspjesi++;
                Thread.sleep(CEKANJE_MS);
            }
        }


        if (this.x == Terminal.KANAL_IZLAZ) {
            pomjeriSaCekanjem(Terminal.KANAL_ULAZ, this.y, korak);
        }
        return true;
    }

    /**
     * Provjerava da li je suprotna traka kanala ({@link Terminal#KANAL_IZLAZ}) slobodna i na
     * trenutnoj i na sljedećoj koloni. Ovo je preduslov za preticanje (preko jednog polja lijevo,
     * ako nema suprotnog smjera).
     *
     * @param sljedeciY Kolona u koju bi plovilo prešlo nakon preticanja.
     * @return {@code true} ako je preticanje bezbjedno (oba polja u suprotnoj traci slobodna).
     */
    private boolean smijePreticati(int sljedeciY) {
        Terminal t = this.trenutniTerminal;
        if (t == null) {
            return false;
        }
        synchronized (t) {
            Polje[][] m = t.getMatrica();
            return m[Terminal.KANAL_IZLAZ][this.y].getTrenutnoPlovilo() == null
                    && m[Terminal.KANAL_IZLAZ][sljedeciY].getTrenutnoPlovilo() == null;
        }
    }

    /**
     * Provjerava da li plovilo na zadatoj poziciji treba ustupiti prolaz (stati u mjestu) plovilu
     * neposredno iza sebe u istoj traci:<br>
     *
     * Ustupa ako je ono iza prisutno i ima viši prioritet (nižu brojčanu vrijednost prioriteta) od trenutnog plovila.
     * Poređenje je uvijek {@code iza.getPrioritet() < trenutni.getPrioritet()},
     * bez posebnog slučaja za "plovilo pod rotacijom",
     * pa redoslijed vatrogasci > obalska straža > carina > komercijalno ispada prirodno iz poređenja
     * brojčanih vrijednosti prioriteta.
     *
     * @param terminal Terminal u kojem se provjera vrši. Ako je {@code null}, vraća {@code false}.
     * @param x Red trenutnog plovila u matrici terminala.
     * @param y Kolona trenutnog plovila u matrici terminala. Ako je {@code y <= 0}, nema polja
     *          iza, pa se vraća {@code false}.
     * @param trenutni Plovilo za koje se provjerava da li treba ustupiti prolaz.
     * @return {@code true} ako plovilo neposredno iza ima viši prioritet od {@code trenutni}.
     */
    static boolean ustupaProlaz(Terminal terminal, int x, int y, Plovilo trenutni) {
        if (terminal == null || y <= 0) {
            return false;
        }
        synchronized (terminal) {
            Plovilo iza = terminal.getMatrica()[x][y - 1].getTrenutnoPlovilo();
            return iza != null && iza.getPrioritet() < trenutni.getPrioritet();
        }
    }

    /**
     * Vodi plovilo od trenutne pozicije ka izlazu iz terminala: prebacuje se u zapadnu traku
     * kanala ({@link Terminal#KANAL_IZLAZ}) ako već nije u njemu, plovi njime do izlazne kolone
     * ({@link Terminal#KOLONA_IZLAZ}), pa se penje uz nju do reda 0 i oslobađa svoju posljednju
     * ćeliju ({@link #oslobodiTrenutnoPolje()}). Ako terminal nije postavljen (plovilo nikad nije
     * ušlo), metoda odmah vraća bez efekta zahvaljujući provjeri {@code t == null}, gdje je t parametar metode,
     * odnosno referenca na tekući {@link Terminal}.
     *
     * @throws InterruptedException Ako je nit prekinuta tokom čekanja.
     */
    private void napustiTerminal() throws InterruptedException {
        Terminal t = this.trenutniTerminal;
        if (t == null) {
            return;
        }
        long korak = trajanjeKoraka();

        if (this.y > Terminal.KOLONA_IZLAZ) {
            if (this.x == 3) {
                if (!pomjeriSaCekanjem(Terminal.KANAL_ULAZ, this.y, korak)) {
                    LoggerUtil.logWarning("Plovilo " + plovilo.getImoBroj()
                            + " ne moze uci u kanal sa (" + this.x + "," + this.y + ").");
                    oslobodiTrenutnoPolje();
                    log("Napustio terminal.");
                    return;
                }
                Thread.sleep(korak);
            }
            if (this.x != Terminal.KANAL_IZLAZ) {
                if (!pomjeriSaCekanjem(Terminal.KANAL_IZLAZ, this.y, korak)) {
                    LoggerUtil.logWarning("Plovilo " + plovilo.getImoBroj()
                            + " ne moze preci u izlazni kanal sa (" + this.x + "," + this.y + ").");
                    oslobodiTrenutnoPolje();
                    log("Napustio terminal.");
                    return;
                }
                Thread.sleep(korak);
            }

            int pokusaja = 0;
            int blokada = 0;
            while (this.y > Terminal.KOLONA_IZLAZ && pokusaja < MAX_POKUSAJA * 4) {
                if (pomjeriNaPolje(Terminal.KANAL_IZLAZ, this.y - 1)) {
                    Thread.sleep(korak);
                } else {
                    if (cekaZbogBlokade()) {
                        if (++blokada > maxBlokadaPokusaja()) {
                            odustajemZbogBlokade();
                            break;
                        }
                    } else {
                        blokada = 0;
                        pokusaja++;
                    }
                    Thread.sleep(CEKANJE_MS);
                }
            }
        }

        if (this.y > Terminal.KOLONA_IZLAZ) {
            LoggerUtil.logWarning("Plovilo " + plovilo.getImoBroj()
                    + " nije stiglo do izlazne kolone, ostaje na (" + this.x + "," + this.y + ").");
            oslobodiTrenutnoPolje();
            log("Napustio terminal.");
            return;
        }

        int pokusaja = 0;
        int blokada = 0;
        while (this.x > 0 && pokusaja < MAX_POKUSAJA * 2) {
            if (pomjeriNaPolje(this.x - 1, this.y)) {
                Thread.sleep(korak);
            } else {
                if (cekaZbogBlokade()) {
                    if (++blokada > maxBlokadaPokusaja()) {
                        odustajemZbogBlokade();
                        break;
                    }
                } else {
                    blokada = 0;
                    pokusaja++;
                }
                Thread.sleep(CEKANJE_MS);
            }
        }

        oslobodiTrenutnoPolje();
        log("Napustio terminal.");
    }
    
    /**
     * Pomoćna metoda koja čisti referencu na trenutni dok (jer ga posmatrano plovilo napušta, tj. odvezalo se), i potom poziva {@link Terminal#otkaziRezervaciju(Dok)}.
     */
    private void napustiZbogPratnje() {
        Terminal t = this.trenutniTerminal;
        Dok dok = this.trenutniDok;
        this.trenutniDok = null;
        if (t != null && dok != null) {
            t.otkaziRezervaciju(dok);
        }
    }

    /**
     * Otkazuje rezervaciju doka koji se napušta zarad odlaska posmatranog plovila na mjesto incidenta u ciljnom terminalu
     * (ne mora nužno biti isti terminal u kom se plovilo nalazi),
     * te vrši premještanje plovila do ciljnog terminala na kom se desio incident (upotrebom {@link #predjiLogickiUTerminal(Terminal, long)} i {@link #napredujKaPolju(int, int, long)}),
     * te vrši čekanje do kraja uviđaja incidenta.
     *
     * @throws InterruptedException ako je nit prekinuta tokom čekanja na kraj uviđaja, klauzula dodana zbog postojanja istih klauzula u metodama {@link #cekajKrajUvidjaja()}, {@link #napredujKaPolju(int, int, long)} i {@link Terminal#otkaziRezervaciju(Dok)}.
     */
    private void otidjiNaIncident() throws InterruptedException {
        Terminal staviTerminal = this.trenutniTerminal;
        Dok dokKojiNapustam = this.trenutniDok;
        this.trenutniDok = null;
        if (staviTerminal != null && dokKojiNapustam != null) {
            staviTerminal.otkaziRezervaciju(dokKojiNapustam);
        }

        Terminal ciljniTerminal = this.ciljniTerminalIncidenta;
        int ciljX = this.ciljXIncidenta;
        int ciljY = this.ciljYIncidenta;
        long korak = trajanjeKoraka();

        if (this.trenutniTerminal != ciljniTerminal) {
            predjiLogickiUTerminal(ciljniTerminal, korak);
        }

        if (this.trenutniTerminal == ciljniTerminal) {
            napredujKaPolju(ciljX, ciljY, korak);
        }

        this.zadatak = Zadatak.NA_INCIDENTU;
        cekajKrajUvidjaja();
        if (this.zadatak != Zadatak.KA_DOKU) {
            this.zadatak = Zadatak.NAPUSTA;
        }
    }

    /**
     * Pomoćna metoda koja vrši logičko premještanje plovila iz starog u novi terminal (obavezno različiti) tako što:<br>
     * -oslobađa trenutno polje na kom se plovilo nalazi<br>
     * -poziva {@link #pokusajUciUTerminal(Terminal)} (odnosno {@link #udjiUTerminal(Terminal)}) <br>
     * -u slučaju neuspješnog izvršavanja ijedne od te dvije metode, loguje odgovarajuću grešku upotrebom {@link LoggerUtil}.
     *
     * @param ciljniTerminal Terminal u koji nit plovila prelazi.
     * @param korak Trajanje jednog koraka kretanja, izraženo u milisekundama.
     *
     * @throws InterruptedException ako je nit prekinuta tokom čekanja.
     */
    private void predjiLogickiUTerminal(Terminal ciljniTerminal, long korak) throws InterruptedException {
        Terminal stariTerminal = this.trenutniTerminal;
        int stariX = this.x;
        int stariY = this.y;

        oslobodiTrenutnoPolje();

        if (!pokusajUciUTerminal(ciljniTerminal) && !udjiUTerminal(ciljniTerminal)) {
            LoggerUtil.logWarning("Patrola " + plovilo.getImoBroj() + " ne moze uci u terminal "
                    + ciljniTerminal.getIdTerminala() + ".");
            if (!vratiNaPolje(stariTerminal, stariX, stariY)) {
                LoggerUtil.logError("Patrola " + plovilo.getImoBroj()
                        + " je izgubila poziciju u luci.",
                        new IllegalStateException("Plovilo bez terminala"));
            }
            return;
        }
        sidjiDoKanala(korak);
    }


    /**
     * Pokušava vratiti tekuće plovilo na polje na kom je prethodno boravio prije napuštanja
     * (zarad odlaska na mjesto incidenta koji se desio u nekom od terminala, ne nužno isti terminal na kom je bio privezan).
     *
     * @param t Tekući terminal.
     * @param px X koordinata polja na koje se plovilo vraća
     * @param py Y koordinata polja na koje se plovilo vraća
     *
     * @return {@code true} ako je plovilo uspješno vraćeno na polje, {@code false} u suprotnom.
     */
    private boolean vratiNaPolje(Terminal t, int px, int py) {
        if (t == null || px < 0 || py < 0) {
            return false;
        }
        synchronized (t) {
            Polje p = t.getMatrica()[px][py];
            if (p.getTrenutnoPlovilo() != null) {
                return false;
            }
            p.setTrenutnoPlovilo(this.plovilo);
        }
        this.trenutniTerminal = t;
        this.x = px;
        this.y = py;
        return true;
    }

    /**
     * Metoda koja vrši pomjeranje plovila u ciljni red, pri čemu se logika pomjeranja pozajmljuje iz {@link #pomjeriSaCekanjem(int, int, long)}.
     *
     * @param ciljniRed Red u koji se plovilo premješta.
     * @param korak Vremenski interval između uzastopnih pokušaja, izražen u milisekundama.
     *
     * @return {@code true} ako je pomjeranje uspješno, {@code false} inače.
     *
     * @throws InterruptedException ako je nit prekinuta tokom čekanja između pokušaja.
     */
    private boolean pomjeriSeURed(int ciljniRed, long korak) throws InterruptedException {
        while (this.x != ciljniRed) {
            int sljedeciX = this.x < ciljniRed ? this.x + 1 : this.x - 1;
            if (!pomjeriSaCekanjem(sljedeciX, this.y, korak)) {
                return false;
            }
            Thread.sleep(korak);
        }
        return true;
    }

    /**
     * Vrši inkrementalno pomjeranje ka ciljnom polju (ćeliji matrice) tako što prvo vrši pomjeranje u ciljnu traku (upotrebom {@link #pomjeriSeURed(int, long)},
     * potom u ciljnu kolonu (upotrebom {@link #pomjeriSaCekanjem(int, int, long)}), i konačno u ciljni red (ponovo {@link #pomjeriSeURed(int, long)}).
     *
     * @param ciljX Ciljna x koordinata.
     * @param ciljY Ciljna y koordinata.
     * @param korak interval između uzastopnih pokušaja pomjeranja, izražen u milisekundama.
     *
     * @return {@code true} ako su koordinate plovila promijenjene u odnosu na prije poziva ove metode, {@code false} inače.
     *
     * @throws InterruptedException ako je nit prekinuta u toku čekanja.
     */
    private boolean napredujKaPolju(int ciljX, int ciljY, long korak) throws InterruptedException {
        int traka = (ciljY > this.y) ? Terminal.KANAL_ULAZ : Terminal.KANAL_IZLAZ;
        if (!pomjeriSeURed(traka, korak)) {
            return false;
        }

        while (this.y != ciljY) {
            int sljedeciY = this.y < ciljY ? this.y + 1 : this.y - 1;
            if (!pomjeriSaCekanjem(this.x, sljedeciY, korak)) {
                return false;
            }
            Thread.sleep(korak);
        }

        return pomjeriSeURed(ciljX, korak);
    }

    /**
     * Vraća posmatrano plovilo na dok terminala na kom je izvršen uviđaj incidenta koji se desio.
     *
     * @return {@code true} ako je plovilo uspješno privezano na dok, {@code false} inače.
     *
     * @throws InterruptedException ako je nit prekinuta tokom čekanja.
     */
    private boolean vratiSeNaDok() throws InterruptedException {
        Dok noviDok = this.dokPoUvidjaju;
        this.dokPoUvidjaju = null;
        Terminal t = this.trenutniTerminal;
        if (noviDok == null || t == null) {
            return false;
        }

        long korak = trajanjeKoraka();
        int ciljX = noviDok.getLokacija().getX();
        int ciljY = noviDok.getLokacija().getY();

        if (!napredujKaPolju(ciljX, ciljY, korak)) {
            t.otkaziRezervaciju(noviDok);
            return false;
        }

        this.isPrivezan = true;
        this.trenutniDok = noviDok;
        return true;
    }

    /**
     * Metoda zaključava monitor tekuće niti {@link #parkLock}, potom unutar {@code synchronized(parkLock} bloka vrši čekanje do isteka krajnjeg vremena predviđenog za čekanje na kraj uviđaja incidenta koji se desio.
     *
     * @throws InterruptedException ako je nit prekinuta u toku čekanja na neočekivan način.
     */
    private void cekajKrajUvidjaja() throws InterruptedException {
        long krajnjeVrijeme = System.currentTimeMillis() + maxCekanjeKrajaUvidjaja();
        synchronized (parkLock) {
            while (this.zadatak == Zadatak.NA_INCIDENTU) {
                long preostalo = krajnjeVrijeme - System.currentTimeMillis();
                if (preostalo <= 0) {
                    return;
                }
                parkLock.wait(preostalo);
            }
        }
    }

    /**
     * Oslobađa trenutnu ćeliju matrice terminala koju plovilo zauzima (postavlja je na
     * {@code null}) i resetuje poziciju niti na "nepozicionirano" ({@code x == y == -1}).
     * Provjerava referentnim identitetom ({@code ==}, ne {@code equals()}) da polje zaista sadrži
     * baš ovo plovilo prije oslobađanja, jer dva različita plovila sa istim IMO brojevima se
     * inače ne bi smjela pomješati u matrici terminala (a {@code equals()} je upravo zasnovan na jednakosti isključivo IMO brojeva).
     */
    private void oslobodiTrenutnoPolje() {
        Terminal t = this.trenutniTerminal;
        if (t == null || this.x < 0 || this.y < 0) {
            return;
        }
        synchronized (t) {
            Polje p = t.getMatrica()[this.x][this.y];
            if (p.getTrenutnoPlovilo() == this.plovilo) {
                p.setTrenutnoPlovilo(null);
            }
        }
        this.trenutniTerminal = null;
        this.x = -1;
        this.y = -1;
    }

    /**
     * Pokušava premjestiti plovilo na ciljnu ćeliju matrice terminala, u jednoj
     * {@code synchronized} operaciji (kako bi se obezbijedilo da se nikad dva plovila ne nađu na istom polju).
     * Uspijeva samo ako je ciljna ćelija trenutno slobodna,
     * i u tom slučaju zauzima ciljnu ćeliju a oslobađa staru
     * (provjerom referentnog identiteta {@code ==}, namjerno ne {@code equals()} jer je {@code equals()} zasnovan na jednakosti IMO brojeva)
     * i ažurira {@link #x}/{@link #y}.
     *
     * <p>Ovo je jedina fizička primitiva kretanja kroz koju
     * prolaze sve metode kretanja ({@link #sidjiDoKanala}, {@link #ploviIstocno},
     * {@link #napustiTerminal}, {@link #doploviDoDoka}), pa je ovo mjesto na kojem se provjerava
     * {@link Terminal#smijeProci(Plovilo)}:
     * Ako je terminal pod blokadom, pomjeranje ne uspijeva osim za plovilo sa aktivnom (uključenom) rotacijom.
     * Provjera je čitanje jednog {@code volatile} flag-a,
     * van {@code synchronized(t)} bloka i bez čekanja, što ne krši pravilo da se nikad ne poziva {@code wait()}/
     * {@code sleep()} dok se {@code synchronized(terminal)} drži.</p>
     *
     * @param targetX Ciljni red u matrici terminala.
     * @param targetY Ciljna kolona u matrici terminala.
     * @return {@code true} ako je pomjeranje uspjelo, {@code false} ako je ciljna ćelija zauzeta,
     *         terminal blokira ovo plovilo, ili plovilo trenutno nije pozicionirano ni u jednom
     *         terminalu.
     */
    boolean pomjeriNaPolje(int targetX, int targetY) {
        Terminal t = this.trenutniTerminal;
        if (t == null || this.x < 0 || this.y < 0) {
            return false;
        }
        if (Math.abs(targetX - this.x) + Math.abs(targetY - this.y) != 1) {
            LoggerUtil.logError("Pokusaj skoka sa (" + this.x + "," + this.y + ") na ("
                    + targetX + "," + targetY + "), plovilo " + plovilo.getImoBroj(),
                    new IllegalArgumentException("Polja nisu susjedna"));
            return false;
        }
        if (!t.smijeProci(this.plovilo)) {
            return false;
        }

        synchronized (t) {
            Polje[][] matrica = t.getMatrica();
            Polje staro = matrica[this.x][this.y];
            Polje novo = matrica[targetX][targetY];

            if (novo.getTrenutnoPlovilo() == null) {
                novo.setTrenutnoPlovilo(this.plovilo);
                if (staro.getTrenutnoPlovilo() == this.plovilo) {
                    staro.setTrenutnoPlovilo(null);
                }
                this.x = targetX;
                this.y = targetY;
                return true;
            }
        }
        return false;
    }

    /**
     * Ponavlja {@link #pomjeriNaPolje(int, int)} do {@link #MAX_POKUSAJA} puta, čekajući
     * {@link #CEKANJE_MS} između pokušaja, dok se ciljna ćelija ne oslobodi.
     *
     * <p><b>Blokada saobraćaja ne troši budžet pokušaja:</b> {@link #MAX_POKUSAJA} *
     * {@link #CEKANJE_MS} = 10_000ms, što je tačno {@link #MAX_TRAJANJE_UVIDJAJA_MS} (podrazumijevana
     * vrijednost). Da neuspjeh izazvan {@link Terminal#smijeProci(Plovilo)} broji isto kao neuspjeh
     * izazvan zauzetom ćelijom, plovilo koje čeka baš na posljednjem koraku ulaska u dok bi moglo
     * iscrpiti čitav budžet pokušaja samo zato što je uviđaj potrajao maksimalno dugo, što bi dovelo do otkazivanja
     * rezervacije veza koji je plovilo legitimno dobilo i natjeralo bi ga da produži dalje ka narednom terminalu, iako ničim
     * nije "zaslužio" taj neuspjeh (nije postojala trajno zauzeta ćelija, samo privremena blokada zbog uviđaja incidenta).
     * Zato se pokušaj koji propadne zbog blokade ne broji, tj. nit i dalje ceka i ponovo pokušava svakih
     * {@link #CEKANJE_MS}, ali brojac {@code i} se ne inkrementira dok terminal ostaje blokiran za ovo
     * plovilo. Ovo odgovara namjeri specifikacije: plovilo je samo zaustavljeno, a ne i neuspješno u traženju
     * rute.</p>
     *
     * @param targetX Ciljni red u matrici terminala.
     * @param targetY Ciljna kolona u matrici terminala.
     * @param korak Trajanje jednog koraka kretanja, izraženo u milisekundama (parametar se ovdje ne
     *              koristi za pauzu, jer pauza je uvijek {@link #CEKANJE_MS}, nego zadržava
     *              dosljedan potpis sa ostalim metodama kretanja).
     * @return {@code true} ako je pomjeranje uspjelo u okviru dozvoljenog broja pokušaja.
     * @throws InterruptedException Ako je nit prekinuta tokom čekanja.
     */
    boolean pomjeriSaCekanjem(int targetX, int targetY, long korak) throws InterruptedException {
        int i = 0;
        int blokada = 0;

        while (i < MAX_POKUSAJA) {
            if (pomjeriNaPolje(targetX, targetY)) {
                return true;
            }
            if (cekaZbogBlokade()) {
                if (++blokada > maxBlokadaPokusaja()) {
                    odustajemZbogBlokade();
                    return false;
                }
            } else {
                blokada = 0;
                i++;
            }
            Thread.sleep(CEKANJE_MS);
        }
        return false;
    }

    /**
     * Najveći broj uzastopnih pokušaja pomjeranja koji smiju propasti zbog blokade saobraćaja
     * prije nego što plovilo odustane od pomjeranja. Izvedeno iz {@link #MAX_TRAJANJE_UVIDJAJA_MS} (dvostruko,
     * kao nekakva sigurnosna margina), računa se pri svakom pozivu.
     *
     * @return broj maksimalnih dozvoljenih pokušaja blokiranja terminala.
     */
    private static int maxBlokadaPokusaja() {
        return (int) Math.max(1, (MAX_TRAJANJE_UVIDJAJA_MS * 2) / CEKANJE_MS);
    }

    public static long maxCekanjeKrajaUvidjaja() {
        return KoordinatorUvidjaja.MAX_CEKANJE_DOLASKA_MS + MAX_TRAJANJE_UVIDJAJA_MS + 5000L;
    }

    /**
     * Evidentira odustajanje zbog predugačke blokade.
     *
     */
    private void odustajemZbogBlokade() {
        LoggerUtil.logWarning("Blokada terminala traje predugo, plovilo "
                + plovilo.getImoBroj() + " odustaje.");
    }

    /**
     * Provjerava da li bi posljednji neuspjeh {@link #pomjeriNaPolje(int, int)} mogao biti
     * posljedica blokade saobraćaja na terminalu, a ne trajno zauzete ciljne ćelije, koristi
     * {@link #pomjeriSaCekanjem} da takve neuspjehe izuzme iz budžeta pokušaja.
     * Terminal koji nije postavljen (plovilo nikad nije ušlo) se tretira kao "nije blokada"
     * , premda taj slučaj već rezultuje trajnim neuspjehom preko {@link #pomjeriNaPolje(int, int)}, pa ne
     * smije zaobići budžet pokušaja (inače bi nit čekala beskrajno bez ikakvog terminala).
     *
     * @return {@code true} ako je terminal postavljen i trenutno blokira ovo plovilo.
     */
    private boolean cekaZbogBlokade() {
        Terminal t = this.trenutniTerminal;
        return t != null && !t.smijeProci(this.plovilo);
    }

    /**
     * Metoda izvodi trajanje jednog koraka kretanja iz brzine plovila (jedinstvena slučajna brzina),
     * ograničeno na interval [20ms, 400ms] (zadovoljava napomenu iz .pdf specifikacije koja kaže da simulacija ne smije biti ni prebrza ni prespora),
     * brže plovilo ima kraći korak.
     *
     * @return Trajanje jednog koraka kretanja, u milisekundama.
     */
    private long trajanjeKoraka() {
        long korak = (long) (1000.0 / plovilo.getBrzina());
        return Math.max(20L, Math.min(korak, 400L));
    }

    /**
     * Metoda koja vraća niz referenci na objekte tipa {@link Plovilo} koji predstavlja učesnike sudara.
     *
     * @return Niz koji predstavlja učesnike u sudaru.
     */
    Plovilo[] provjeriSudar() {
        if (!SUDARI_OMOGUCENI) {
            return null;
        }
        Plovilo drugi = drugoPloviloUPreticanju();
        boolean pogodak = generator().nextDouble() < VJEROVATNOCA_SUDARA;
        if (drugi == null || !pogodak) {
            return null;
        }
        return new Plovilo[]{this.plovilo, drugi};
    }

    /**
     * Vraća plovilo koje se trenutno nalazi u suprotnoj traci kanala, na istoj koloni kao ovo
     * plovilo, odnosno kandidata za sudar u trenutku preticanja.
     *
     * @return Plovilo u suprotnoj traci na istoj koloni, ili {@code null} ako je ta ćelija
     *         slobodna ili plovilo trenutno nije pozicionirano ni u jednom terminalu.
     */
    private Plovilo drugoPloviloUPreticanju() {
        Terminal t = this.trenutniTerminal;
        if (t == null) {
            return null;
        }
        int suprotniRed = this.x == Terminal.KANAL_ULAZ ? Terminal.KANAL_IZLAZ : Terminal.KANAL_ULAZ;
        synchronized (t) {
            return t.getMatrica()[suprotniRed][this.y].getTrenutnoPlovilo();
        }
    }

    /**
     * Metoda koja pokreće uviđajni proces za proslijeđeni niz učesnika sudara.
     *
     * @param ucesnici Niz učesnika sudara.
     */
    private void pokreniUvidjaj(Plovilo[] ucesnici) {
        KoordinatorUvidjaja koordinator = new KoordinatorUvidjaja(
                luka, this.trenutniTerminal, List.of(ucesnici[0], ucesnici[1]), this.x, this.y,
                DIREKTORIJUM_INCIDENTA_SUDARA);
        Thread nit = new Thread(koordinator, "koordinator-uvidjaja-" + plovilo.getImoBroj());
        nit.setDaemon(true);
        nit.start();
    }

    /**
     * Ako je ovo plovilo plovilo obalske straže, vrši se provjera prvog IMO broja potjernice i njegovog prisustva u trenutnom terminalu.
     *
     * @return Referenca na objekat klase {@link Plovilo} koji predstavlja konkretnu referencu na plovilo čiji se IMO broj nalazi na potjernici, {@code null} u suprotnom.
     */
    Plovilo provjeriPotjernicu() {
        if (!(this.plovilo instanceof ObalskaStraza obalskaStraza)) {
            return null;
        }
        File spisak = obalskaStraza.getSpisakPotjera();
        if (spisak == null) {
            return null;
        }
        Set<String> potjernice = SpisakPotjeraUtil.ucitaj(spisak);
        if (potjernice.isEmpty()) {
            return null;
        }
        Terminal t = this.trenutniTerminal;
        if (t == null) {
            return null;
        }
        int[][] pomjeraji = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        synchronized (t) {
            Polje[][] m = t.getMatrica();
            for (int[] pom : pomjeraji) {
                int nx = this.x + pom[0];
                int ny = this.y + pom[1];
                if (nx < 0 || nx >= m.length || ny < 0 || ny >= m[nx].length) {
                    continue;
                }
                Plovilo kandidat = m[nx][ny].getTrenutnoPlovilo();
                if (kandidat != null && kandidat != this.plovilo && potjernice.contains(kandidat.getImoBroj())) {
                    return kandidat;
                }
            }
        }
        return null;
    }

    /**
     * Metoda koja za plovilo obalske straže (ne mora biti pozivalac) uključuje rotaciju, i postavlja vrijednost polja {@link #trazenoPlovilo} tako da čuva
     * referencu na plovilo dobijeno metodom {@link #provjeriPotjernicu()}, nakon čega nit datog plovila poziva na pratnju (izlaz iz terminala, ukoliko je prisutno u datom terminalu), i potom gasi rotaciju.
     *
     *
     * @param obalskaStraza plovilo obalske straže koje vrši potjeru za traženim plovilom.
     * @param trazeno referenca na traženo plovilo.
     *
     * @throws InterruptedException ako je plovilo obalske straže prekinuto u čekanju za vrijeme trajanje potjere.
     */
    void pokreniPotjernicu(ObalskaStraza obalskaStraza, Plovilo trazeno) throws InterruptedException {
        obalskaStraza.setRotacija(true);
        this.naPratnji = true;
        this.trazenoPlovilo = trazeno;

        BrodThread trazenaNit = pronadjiNit(trazeno);
        if (trazenaNit != null) {
            trazenaNit.pozoviNaPratnju();
        }

        long trajanje = trajanjePotjernice();
        Thread.sleep(trajanje);
        sacuvajEvidencijuPotjernice(trajanje);

        this.zadatak = Zadatak.NAPUSTA;
        obracunajIZabiljeziTaksu();
        napustiTerminal();

        if (this.plovilo instanceof SluzbenoPlovilo sluzbeno) {
            sluzbeno.setRotacija(false);
        }
    }

    /**
     * Pronalazi aktivnu nit koja upravlja zadatim plovilom, tražeći po registru aktivnih plovila
     * luke kojoj ova nit pripada.
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
     * Izračunava slučajno trajanje potjere, ravnomjerno raspoređeno između
     * {@link #MIN_TRAJANJE_UVIDJAJA_POTJERNICE_MS} i {@link #MAX_TRAJANJE_UVIDJAJA_POTJERNICE_MS}.
     *
     * @return Trajanje potjere, u milisekundama.
     */
    private long trajanjePotjernice() {
        long min = MIN_TRAJANJE_UVIDJAJA_POTJERNICE_MS;
        long max = MAX_TRAJANJE_UVIDJAJA_POTJERNICE_MS;
        if (max <= min) {
            return min;
        }
        return min + ThreadLocalRandom.current().nextLong(max - min + 1);
    }

    /**
     * Konstruiše zapis o potjeri (traženo plovilo kao jedini učesnik, obalska straža kao jedino
     * odazvano plovilo) i upisuje ga kao binarni fajl preko {@link Incident#sacuvaj()}, u
     * {@link #DIREKTORIJUM_INCIDENTA_POTJERNICE} ako je postavljen, inače u podrazumijevani
     * direktorijum.
     *
     * @param trajanje Trajanje potjere, u milisekundama.
     */
    private void sacuvajEvidencijuPotjernice(long trajanje) {
        // Mora se pozvati prije napustiTerminal() — poslije njega je trenutniTerminal null i idTerminala bi ispao -1.
        Terminal t = this.trenutniTerminal;
        int idTerminala = t != null ? t.getIdTerminala() : -1;
        Incident incident = new Incident(List.of(this.trazenoPlovilo), List.of(this.plovilo),
                LocalDateTime.now(), trajanje, idTerminala, TipIncidenta.POTJERNICA);

        File dir = DIREKTORIJUM_INCIDENTA_POTJERNICE;
        if (dir != null) {
            incident.sacuvaj(dir);
        } else {
            incident.sacuvaj();
        }
    }

    /**
     * Injektuje izvor slučajnosti koji {@link #provjeriSudar()} koristi za sudare.
     * Podrazumijevano je svaka nit svoj {@link ThreadLocalRandom}, nepredvidljiv po dizajnu.
     *
     * @param generatorSudara Izvor slučajnosti koji mijenja podrazumijevani.
     */
    public void setGeneratorSudara(Random generatorSudara) {
        this.generatorSudara = generatorSudara;
    }

    /**
     * Omogućava dobijanje plovila kojim ova nit upravlja.
     *
     * @return Plovilo koje nit vodi kroz luku.
     */
    public Plovilo getPlovilo() {
        return plovilo;
    }

    /**
     * Provjerava da li je plovilo trenutno privezano na dok.
     *
     * @return {@code true} ako je plovilo privezano.
     */
    public boolean isPrivezan() {
        return isPrivezan;
    }

    /**
     * Provjerava da li je nit trenutno signalizirana da napusti terminal (parkirana čeka na
     * {@link #zatraziNapustanje()}, ili je poziv u toku).
     *
     * @return Trenutna vrijednost zastavice {@link #moraNapustiti}.
     */
    public boolean isMoraNapustiti() {
        return moraNapustiti;
    }

    /**
     * Postavlja zastavicu napuštanja. Prosljeđivanje {@code true} deleguje na
     * {@link #zatraziNapustanje()} (budi parkiranu nit); prosljeđivanje {@code false} vraća
     * zastavicu nazad, čime se poništava prethodni zahtjev ako nit još nije stigla da ga obradi.
     *
     * @param moraNapustiti Nova vrijednost zastavice napuštanja.
     */
    public void setMoraNapustiti(boolean moraNapustiti) {
        if(moraNapustiti) {
            zatraziNapustanje();
        }else {
            synchronized (parkLock) {
                this.moraNapustiti = false;
            }
        }
    }

    /**
     * Omogućava dobijanje trenutnog zadatka niti.
     *
     * @return Trenutni {@link Zadatak}.
     */
    public Zadatak getZadatak() {
        return zadatak;
    }

    /**
     * Postavlja trenutni zadatak niti direktno, bez ikakve dodatne provjere ili sinhronizacije,
     * paket-privatna vidljivost namijenjena isključivo direktnom postavljanju stanja u testovima.
     *
     * @param zadatak Novi zadatak niti.
     */
    void setZadatak(Zadatak zadatak) {
        this.zadatak = zadatak;
    }

    /**
     * Omogućava dobijanje trenutnog reda (X koordinate) plovila u matrici terminala.
     *
     * @return Trenutni red, ili -1 ako plovilo nije pozicionirano ni u jednom terminalu.
     */
    public int getX() {
        return x;
    }

    /**
     * Omogućava dobijanje trenutne kolone (Y koordinate) plovila u matrici terminala.
     *
     * @return Trenutna kolona, ili -1 ako plovilo nije pozicionirano ni u jednom terminalu.
     */
    public int getY() {
        return y;
    }

    /**
     * Omogućava dobijanje terminala u kojem se plovilo trenutno nalazi.
     *
     * @return Trenutni terminal, ili {@code null} ako plovilo nije ni u jednom.
     */
    public Terminal getTrenutniTerminal() {
        return trenutniTerminal;
    }

    /**
     * Ispisuje poruku o kretanju plovila na standardni izlaz, sa nazivom plovila kao prefiksom.
     *
     * @param poruka Poruka koja se ispisuje.
     */
    private void log(String poruka) {
        System.out.println("[" + plovilo.getNaziv() + "] " + poruka);
    }

    /**
     * Omogućava dobijanje izvora slučajnosti za provjeru sudara — ubrizganog preko
     * {@link #setGeneratorSudara(Random)}, ili podrazumijevanog {@link ThreadLocalRandom} ako
     * ništa nije ubrizgano.
     *
     * @return Izvor slučajnosti koji {@link #provjeriSudar()} koristi.
     */
    private Random generator() {
        Random r = this.generatorSudara;
        return r != null ? r : ThreadLocalRandom.current();
    }
}