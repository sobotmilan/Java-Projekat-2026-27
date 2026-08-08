package org.unibl.etf.pj2.luka.simulation;

import org.unibl.etf.pj2.luka.model.classes.Dok;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Polje;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.util.LoggerUtil;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Nit koja upravlja kretanjem jednog {@link Plovilo}-a kroz luku (T10): ulazak kroz kanal
 * terminala, vezivanje na dok, parkirano čekanje dok je privezan, i konačno napuštanje terminala.
 *
 * <p><b>Kanal (T3/R0):</b> plovilo silazi niz ulaznu kolonu ({@link Terminal#KOLONA_ULAZ}) do
 * reda {@link Terminal#KANAL_ULAZ} (istočni, dolazni trak), plovi njime ka istoku, a ulazak u dok
 * je pomjeraj za jedan red gore/dolje sa kanala — nikad kretanje kroz redove dokova (0 i 3). Red
 * {@link Terminal#KANAL_IZLAZ} (zapadni trak) služi i za preticanje (T4) i za izlazak iz terminala.</p>
 *
 * <p><b>Prioritet i preticanje (M5/R5/T4):</b> plovilo pod rotacijom (prioritet manji od
 * {@link #PRIORITET_BEZ_ROTACIJE}) pokušava preticanje čim je blokirano, umjesto da čeka
 * {@link #PRAG_PRETICANJA} neuspjeha kao obično plovilo; obično plovilo dodatno provjerava
 * {@link #ustupaProlaz(Terminal, int, int, Plovilo)} i stoji u mjestu ako je plovilo pod
 * rotacijom neposredno iza njega u istoj traci.</p>
 *
 * <p><b>Zadatak i parkiranje (D3):</b> nit se ne gasi kad se plovilo priveže — ulazi u
 * {@link Zadatak#PRIVEZAN} i parkira se preko {@link #cekajNapustanje()} na posebnom
 * {@link #parkLock} objektu (nikad na {@code synchronized(terminal)}, D4), sve dok je neko ne
 * pozove preko {@link #zatraziNapustanje()} da napusti terminal.</p>
 *
 * <p><b>Sudari (I1/D5):</b> {@link #provjeriSudar()} je i dalje placeholder za R4 — vraća ishod
 * "bacanja kockice" ({@link #VJEROVATNOCA_SUDARA}), ali ne pokreće nikakvu obradu incidenta.
 * {@link #SUDARI_OMOGUCENI} je trenutno {@code false} (namjerno odstupanje od specifikacije radi
 * determinizma testova nakon R0 — mora se vratiti na {@code true} kad R4 bude gotov).</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see Terminal
 * @see Zadatak
 * @see Luka
 */
public class BrodThread implements Runnable {
    /** Maksimalan broj pokušaja pomjeranja za jedan korak prije nego što se pokušaj kretanja smatra neuspjelim. */
    private static final int MAX_POKUSAJA = 100;

    /** Pauza između neuspjelih pokušaja pomjeranja, u milisekundama. */
    private static final long CEKANJE_MS = 100L;

    /** Broj uzastopnih neuspjeha nakon kojeg obično plovilo (bez prioriteta) pokušava preticanje (T4). */
    private static final int PRAG_PRETICANJA = 3;

    /** Prioritet svakog plovila bez upaljene rotacije (podrazumijevana vrijednost iz {@link Plovilo}). */
    private static final int PRIORITET_BEZ_ROTACIJE = 10;

    /**
     * Globalni prekidač za sudare (I1). Trenutno {@code false} — namjerno odstupanje od
     * specifikacije, uvedeno radi determinizma testova poslije R0. Mora se vratiti na
     * {@code true} kad logika uviđaja (R4) bude implementirana.
     */
    public static volatile boolean SUDARI_OMOGUCENI = false;

    /** Vjerovatnoća sudara po provjeri (I1: 2%). Nije final — testovi je postavljaju na 1.0/0.0 radi determinizma (D5). */
    public static volatile double VJEROVATNOCA_SUDARA = 0.02;

    /** Trajanje uviđaja za opšti incident (I3), u milisekundama. Testovi spuštaju na ~50ms da paket ne traje minutama. */
    public static volatile long MIN_TRAJANJE_UVIDJAJA_MS = 3000L;
    public static volatile long MAX_TRAJANJE_UVIDJAJA_MS = 10000L;

    /** Trajanje uviđaja kad je u pitanju plovilo sa potjernice (I5) — uže od opšteg incidenta. */
    public static volatile long MIN_TRAJANJE_UVIDJAJA_POTJERNICE_MS = 3000L;
    public static volatile long MAX_TRAJANJE_UVIDJAJA_POTJERNICE_MS = 5000L;

    /** Plovilo kojim ova nit upravlja. */
    private final Plovilo plovilo;

    /** Luka kojoj plovilo pripada — daje pristup terminalima i evidenciji ulaska. */
    private final Luka luka;
    /** Zaključavanje isključivo za parkirano čekanje (PRIVEZAN) — NIKAD synchronized(terminal), da render ne blokira. */
    private final Object parkLock = new Object();

    /** Terminal u kojem se plovilo trenutno nalazi, ili {@code null} ako nije ni u jednom. */
    private Terminal trenutniTerminal;

    /** Trenutna pozicija (red, kolona) plovila u matrici {@link #trenutniTerminal}-a, ili -1 ako nije pozicionirano. */
    private volatile int x, y;

    /** Da li je plovilo trenutno privezano na dok. */
    private volatile boolean isPrivezan;

    /** Zastavica koju postavlja {@link #zatraziNapustanje()} da probudi parkiranu nit. */
    private volatile boolean moraNapustiti;

    /** Trenutni zadatak niti (D3) — vidi {@link Zadatak}. */
    private volatile Zadatak zadatak;
    /** Izvor slučajnosti za provjeru sudara — injektabilan preko {@link #setGeneratorSudara(Random)} radi ponovljivih testova (D5). */
    private volatile Random generatorSudara;

    {
        this.x = this.y = -1;
        this.isPrivezan = false;
        this.moraNapustiti = false;
        this.zadatak = Zadatak.KA_DOKU;
    }

    /**
     * Konstruktor za plovilo koje tek treba ući u luku kroz ulazni kanal — nit kreće iz
     * {@link Zadatak#KA_DOKU}, nepozicionirana ({@code x == y == -1}).
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
     * flote pri pokretanju simulacije — C3/C4), umjesto da prolazi kroz kanal do njega.
     * Pozivalac je odgovoran da prethodno postavi {@code plovilo} u ćeliju matrice na
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
    }

    /**
     * Životni ciklus niti (T10): registruje se u {@link Luka#getAktivnaPlovila()}, ulazi u luku
     * (ili je već privezana — predokovani konstruktor), potom parkira dok se ne pozove da napusti
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
                this.zadatak = Zadatak.PRIVEZAN;
                cekajNapustanje();
                this.zadatak = Zadatak.NAPUSTA;
                napustiTerminal();
            } else {
                log("Obišao sve terminale i napustio luku — nema slobodnih vezova.");
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
     * Obilazi terminale luke redom (T1) tražeći slobodan dok: rezerviše dok atomarno preko
     * {@link Terminal#rezervisiSlobodanDok(Plovilo)} (R2), pokušava fizički ući u terminal i
     * doploviti do rezervisanog doka, a ako bilo koji od tih koraka ne uspije, otkazuje rezervaciju
     * i nastavlja pravo ka narednom terminalu (T7/T8). Ako nijedan terminal nema slobodan i
     * dostižan dok, plovilo napušta luku bez pristajanja.
     *
     * @return {@code true} ako je plovilo uspješno privezano na neki dok, {@code false} ako je
     *         obišlo sve terminale bez uspjeha.
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
                this.isPrivezan = true;
                log("Usidren na vezu " + rezervisan.getOznakaVezova()
                        + " (" + this.x + "," + this.y + ") u terminalu " + (idx + 1) + ".");
                return true;
            } else {
                t.otkaziRezervaciju(rezervisan);
                log("Ne može doći do veza u terminalu " + (idx + 1) + ", nastavlja dalje.");
                napustiTerminal();
                idx++;
            }
        }
        return false;
    }

    /**
     * Parkira nit dok se plovilo ne pozove da napusti terminal ({@link #zatraziNapustanje()}).
     * KRITIČNO: {@code wait()} se poziva na {@link #parkLock}, nikad na terminalu — inače bi
     * {@code PrikazTerminala.render()}, koji uzima isti ključ, blokirao GUI za trajanje čekanja.
     */
    private void cekajNapustanje() throws InterruptedException {
        synchronized (parkLock) {
            while (!moraNapustiti) {
                parkLock.wait();
            }
        }
    }

    /**
     * Budi parkiranu nit i pokreće je ka izlazu iz terminala. Poziva ga R4 (uviđaj) ili
     * C7/C8 (odlazak/dopuna) nad plovilom koje je trenutno u stanju {@link Zadatak#PRIVEZAN}.
     */
    public void zatraziNapustanje() {
        synchronized (parkLock) {
            this.moraNapustiti = true;
            parkLock.notifyAll();
        }
    }

    /**
     * Pokušava jednokratno zauzeti ulaznu ćeliju terminala ({@code [0][KOLONA_ULAZ]}). Ako je
     * slobodna, plovilo se postavlja na nju i postaje pozicionirano u tom terminalu.
     *
     * @param terminal Terminal u koji plovilo pokušava ući.
     * @return {@code true} ako je ulazna ćelija bila slobodna i plovilo je uspješno ušlo.
     */
    public boolean pokusajUciUTerminal(Terminal terminal) {
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
     * @return {@code true} ako je ulazak uspio u okviru dozvoljenog broja pokušaja.
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
     * Evidentira trenutno vrijeme kao vrijeme ulaska plovila u luku (F1), preko
     * {@link Luka#addToEvidencija(String, java.time.LocalDateTime)}.
     */
    private void evidentirajUlazak() {
        luka.addToEvidencija(plovilo.getImoBroj(), LocalDateTime.now());
    }

    /**
     * Vodi plovilo od ulazne ćelije terminala do rezervisanog doka: silazi do kanala
     * ({@link #sidjiDoKanala(long)}), plovi njime istočno do kolone doka
     * ({@link #ploviIstocno(int, long)}), pa se pomjera na sam dok — direktno ako je dok u redu 3,
     * ili preko privremenog prolaska kroz {@link Terminal#KANAL_IZLAZ} ako je dok u redu 0 (R0:
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
     * reda {@link Terminal#KANAL_ULAZ} (istočnog traka kanala). Provjerava sudar
     * ({@link #provjeriSudar()}) nakon svakog uspješnog koraka (D5 — placeholder, R4 ga tek
     * treba obraditi).
     *
     * @param korak Trajanje jednog koraka kretanja, u milisekundama.
     * @return {@code true} ako je plovilo uspješno stiglo do kanala.
     * @throws InterruptedException Ako je nit prekinuta tokom čekanja.
     */
    private boolean sidjiDoKanala(long korak) throws InterruptedException {
        while (this.x < Terminal.KANAL_ULAZ) {
            if (!pomjeriSaCekanjem(this.x + 1, Terminal.KOLONA_ULAZ, korak)) {
                return false;
            }
            provjeriSudar();
            Thread.sleep(korak);
        }
        return true;
    }

    /**
     * Plovi istočnim trakom kanala ({@link Terminal#KANAL_ULAZ}) do zadate kolone, primjenjujući
     * pravila prioriteta i preticanja (M5/T4/R5) na svakom koraku:
     * <ul>
     *     <li>Prije pomjeranja naprijed, provjerava {@link #ustupaProlaz(Terminal, int, int, Plovilo)}
     *     — ako plovilo pod rotacijom stoji neposredno iza, obično plovilo ovaj korak stoji u mjestu.</li>
     *     <li>Ako pomjeraj naprijed nije moguć (blokirano), pokušava preticanje preko
     *     {@link Terminal#KANAL_IZLAZ} čim je ispunjen prag: odmah za plovilo pod rotacijom
     *     (prioritet ispod {@link #PRIORITET_BEZ_ROTACIJE}), ili nakon {@link #PRAG_PRETICANJA}
     *     uzastopnih neuspjeha za obično plovilo.</li>
     *     <li>Plovilo koje trenutno pretiče (nalazi se u {@link Terminal#KANAL_IZLAZ}) pomjera se
     *     naprijed pa se odmah vraća u svoj trak čim to postane moguće.</li>
     * </ul>
     * Prioritet se čita iznova na početku svake iteracije (ne kešira se prije petlje), tako da
     * plovilo kojem bi R4 upalilo rotaciju usred tranzita odmah dobija prioritet.
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

        while (this.y < ciljY) {
            if (++ukupnoPokusaja > MAX_POKUSAJA * 4) {
                return false;
            }

            boolean imamPrioritet = plovilo.getPrioritet() < PRIORITET_BEZ_ROTACIJE;
            boolean pomjeren = false;

            if (this.x == Terminal.KANAL_ULAZ) {
                boolean moraUstupitiProlaz = ustupaProlaz(this.trenutniTerminal, this.x, this.y, this.plovilo);

                if (!moraUstupitiProlaz) {
                    pomjeren = pomjeriNaPolje(Terminal.KANAL_ULAZ, this.y + 1);
                }

                boolean pragZaPreticanjeIspunjen = imamPrioritet || neuspjesi >= PRAG_PRETICANJA;
                if (!pomjeren && pragZaPreticanjeIspunjen && smijePreticati(this.y + 1)) {
                    pomjeren = pomjeriNaPolje(Terminal.KANAL_IZLAZ, this.y);
                    if (pomjeren) {
                        log("Započinje preticanje" + (imamPrioritet ? " (prioritet pod rotacijom)." : "."));
                    }
                }
            } else {
                pomjeren = pomjeriNaPolje(Terminal.KANAL_IZLAZ, this.y + 1);
                if (pomjeren) {
                    Thread.sleep(korak);
                    pomjeriNaPolje(Terminal.KANAL_ULAZ, this.y);
                }
            }

            if (pomjeren) {
                neuspjesi = 0;
                provjeriSudar();
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
     * Provjerava da li je suprotni trak kanala ({@link Terminal#KANAL_IZLAZ}) slobodan i na
     * trenutnoj i na sljedećoj koloni — preduslov za preticanje (T4: preko jednog polja lijevo,
     * ako nema suprotnog smjera).
     *
     * @param sledeciY Kolona u koju bi plovilo prešlo nakon preticanja.
     * @return {@code true} ako je preticanje bezbjedno (oba polja u suprotnom traku slobodna).
     */
    private boolean smijePreticati(int sledeciY) {
        Terminal t = this.trenutniTerminal;
        if (t == null) {
            return false;
        }
        synchronized (t) {
            Polje[][] m = t.getMatrica();
            return m[Terminal.KANAL_IZLAZ][this.y].getTrenutnoPlovilo() == null
                    && m[Terminal.KANAL_IZLAZ][sledeciY].getTrenutnoPlovilo() == null;
        }
    }

    /**
     * Provjerava da li plovilo na zadatoj poziciji treba ustupiti prolaz (stati u mjestu) plovilu
     * neposredno iza sebe u istoj traci (R5): ustupa ako je ono iza prisutno i ima viši prioritet
     * (nižu brojčanu vrijednost) od trenutnog plovila. Poređenje je uvijek {@code iza.getPrioritet()
     * < trenutni.getPrioritet()} — bez posebnog slučaja za "plovilo pod rotacijom" — pa redoslijed
     * vatrogasci &gt; obalska straža &gt; carina &gt; komercijalno ispada prirodno iz poređenja
     * brojčanih vrijednosti prioriteta.
     *
     * <p>Paket-privatna vidljivost namjerno, da bi testovi u paketu {@code simulation} mogli
     * pozvati metodu direktno, bez pokretanja cijele niti.</p>
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
     * Vodi plovilo od trenutne pozicije ka izlazu iz terminala: prebacuje se u zapadni trak
     * kanala ({@link Terminal#KANAL_IZLAZ}) ako već nije u njemu, plovi njime do izlazne kolone
     * ({@link Terminal#KOLONA_IZLAZ}), pa se penje uz nju do reda 0 i oslobađa svoju posljednju
     * ćeliju ({@link #oslobodiTrenutnoPolje()}). Ako terminal nije postavljen (plovilo nikad nije
     * ušlo), metoda odmah vraća bez efekta.
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
                pomjeriSaCekanjem(Terminal.KANAL_ULAZ, this.y, korak);
                Thread.sleep(korak);
            }
            if (this.x != Terminal.KANAL_IZLAZ) {
                pomjeriSaCekanjem(Terminal.KANAL_IZLAZ, this.y, korak);
                Thread.sleep(korak);
            }

            int pokusaja = 0;
            while (this.y > Terminal.KOLONA_IZLAZ && pokusaja++ < MAX_POKUSAJA * 4) {
                if (pomjeriNaPolje(Terminal.KANAL_IZLAZ, this.y - 1)) {
                    Thread.sleep(korak);
                } else {
                    Thread.sleep(CEKANJE_MS);
                }
            }
        }

        int pokusaja = 0;
        while (this.x > 0 && pokusaja++ < MAX_POKUSAJA * 2) {
            if (pomjeriNaPolje(this.x - 1, Terminal.KOLONA_IZLAZ)) {
                Thread.sleep(korak);
            } else {
                Thread.sleep(CEKANJE_MS);
            }
        }

        oslobodiTrenutnoPolje();
        log("Napustio terminal.");
    }

    /**
     * Oslobađa trenutnu ćeliju matrice terminala koju plovilo zauzima (postavlja je na
     * {@code null}) i resetuje poziciju niti na "nepozicionirano" ({@code x == y == -1}).
     * Provjerava referentnim identitetom ({@code ==}, ne {@code equals()}) da polje zaista sadrži
     * baš ovo plovilo prije oslobađanja — dva različita plovila sa istim IMO brojem (S6) se
     * inače ne bi smjela pomiješati u matrici terminala.
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
     * {@code synchronized} operaciji (T5: nikad dva plovila na istom polju). Uspijeva samo ako je
     * ciljna ćelija trenutno slobodna; u tom slučaju zauzima ciljnu ćeliju i oslobađa staru
     * (provjerom referentnog identiteta {@code ==}, namjerno ne {@code equals()} — vidi napomenu
     * uz {@link #oslobodiTrenutnoPolje()}) i ažurira {@link #x}/{@link #y}.
     *
     * <p><b>Blokada saobraćaja (I3/I4):</b> ovo je jedina fizička primitiva kretanja kroz koju
     * prolaze sve metode kretanja ({@link #sidjiDoKanala}, {@link #ploviIstocno},
     * {@link #napustiTerminal}, {@link #doploviDoDoka}), pa je ovo mjesto na kojem se provjerava
     * {@link Terminal#smijeProci(Plovilo)} — ako je terminal pod blokadom, pomjeranje ne uspijeva
     * osim za plovilo pod aktivnom rotacijom. Provjera je čitanje jedne {@code volatile} zastavice,
     * van {@code synchronized(t)} bloka i bez čekanja — ne krši D4 (nikad {@code wait()}/
     * {@code sleep()} dok je {@code synchronized(terminal)} držan).</p>
     *
     * <p>Paket-privatna vidljivost namjerno (isti obrazac kao {@link #ustupaProlaz} i
     * {@link #provjeriSudar}) — testovi u paketu {@code simulation} provjeravaju efekat blokade
     * direktno, bez pokretanja cijele niti.</p>
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
     * <p><b>Blokada saobraćaja ne troši budžet pokušaja (R4a):</b> {@link #MAX_POKUSAJA} ×
     * {@link #CEKANJE_MS} = 10000ms, što je tačno {@link #MAX_TRAJANJE_UVIDJAJA_MS} (podrazumijevana
     * vrijednost). Da neuspjeh izazvan {@link Terminal#smijeProci(Plovilo)} broji isto kao neuspjeh
     * izazvan zauzetom ćelijom, plovilo koje čeka baš na posljednjem koraku ulaska u dok bi moglo
     * iscrpiti čitav budžet pokušaja samo zato što je uviđaj potrajao maksimalno dugo — otkazati
     * rezervaciju veza koji je legitimno dobilo i produžiti dalje ka narednom terminalu, iako ničim
     * nije "zaslužilo" taj neuspjeh (nije postojala trajno zauzeta ćelija, samo privremena blokada).
     * Zato se pokušaj koji propadne zbog blokade ne broji — nit i dalje čeka i ponovo pokušava svaki
     * {@link #CEKANJE_MS}, ali {@code i} se ne inkrementira dok terminal ostaje blokiran za ovo
     * plovilo. Ovo odgovara namjeri specifikacije: plovilo je zaustavljeno, ne neuspješno u traženju
     * rute (I3).</p>
     *
     * @param targetX Ciljni red u matrici terminala.
     * @param targetY Ciljna kolona u matrici terminala.
     * @param korak Trajanje jednog koraka kretanja, u milisekundama (parametar se ovdje ne
     *              koristi za pauzu — pauza je uvijek {@link #CEKANJE_MS} — nego zadržava
     *              dosljedan potpis sa ostalim metodama kretanja).
     * @return {@code true} ako je pomjeranje uspjelo u okviru dozvoljenog broja pokušaja.
     * @throws InterruptedException Ako je nit prekinuta tokom čekanja.
     */
    boolean pomjeriSaCekanjem(int targetX, int targetY, long korak) throws InterruptedException {
        int i = 0;
        while (i < MAX_POKUSAJA) {
            if (pomjeriNaPolje(targetX, targetY)) {
                return true;
            }
            if (!cekaZbogBlokade()) {
                i++;
            }
            Thread.sleep(CEKANJE_MS);
        }
        return false;
    }

    /**
     * Provjerava da li bi posljednji neuspjeh {@link #pomjeriNaPolje(int, int)} mogao biti
     * posljedica blokade saobraćaja na terminalu (I3), a ne trajno zauzete ciljne ćelije — koristi
     * {@link #pomjeriSaCekanjem} da takve neuspjehe izuzme iz budžeta pokušaja (vidi napomenu uz tu
     * metodu). Terminal koji nije postavljen (plovilo nikad nije ušlo) se tretira kao "nije blokada"
     * — taj slučaj već rezultuje trajnim neuspjehom preko {@link #pomjeriNaPolje(int, int)}, pa ne
     * smije zaobići budžet pokušaja (inače bi nit čekala unedogled bez ikakvog terminala).
     *
     * @return {@code true} ako je terminal postavljen i trenutno blokira ovo plovilo.
     */
    private boolean cekaZbogBlokade() {
        Terminal t = this.trenutniTerminal;
        return t != null && !t.smijeProci(this.plovilo);
    }

    /**
     * Izvodi trajanje jednog koraka kretanja iz brzine plovila (M7: jedinstvena slučajna brzina),
     * ograničeno na interval [20ms, 400ms] (T11: simulacija ni prebrza ni prespora) — brže
     * plovilo ima kraći korak.
     *
     * @return Trajanje jednog koraka kretanja, u milisekundama.
     */
    private long trajanjeKoraka() {
        long korak = (long) (1000.0 / plovilo.getBrzina());
        return Math.max(20L, Math.min(korak, 400L));
    }

    /**
     * Provjerava da li je u ovom koraku kretanja došlo do sudara. I dalje samo placeholder za
     * R4: vraća ishod "bacanja kockice", ali ne pokreće nikakvu obradu (nema {@code Incident}
     * objekta, dispečovanja službenih plovila ni blokade terminala) — to je predmet R4.
     * Paket-privatna vidljivost namjerno, po uzoru na {@link #ustupaProlaz}, da bi testovi mogli
     * direktno provjeriti determinizam bez pokretanja cijele niti.
     *
     * @return true ako je slučajno izvučena vrijednost pogodila prag {@link #VJEROVATNOCA_SUDARA},
     *         inače false. Uvijek false dok je {@link #SUDARI_OMOGUCENI} isključeno (I1).
     */
    boolean provjeriSudar() {
        if (!SUDARI_OMOGUCENI) {
            return false;
        }
        return generator().nextDouble() < VJEROVATNOCA_SUDARA;
    }

    /**
     * Ubrizgava izvor slučajnosti koji {@link #provjeriSudar()} koristi za sudare — testovi ga
     * sjeme radi ponovljivog niza ishoda (D5). Podrazumijevano je svaka nit svoj
     * {@link ThreadLocalRandom}, nepredvidiv po dizajnu.
     *
     * @param generatorSudara Izvor slučajnosti koji zamjenjuje podrazumijevani.
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
     * Omogućava dobijanje trenutnog zadatka niti (D3).
     *
     * @return Trenutni {@link Zadatak}.
     */
    public Zadatak getZadatak() {
        return zadatak;
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