package org.unibl.etf.pj2.luka.simulation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.unibl.etf.pj2.luka.model.classes.Dok;
import org.unibl.etf.pj2.luka.model.classes.KontejnerskiBrodObalskaStraza;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.model.interfaces.SluzbenoPlovilo;
import org.unibl.etf.pj2.luka.testutil.TestFactory;
import org.unibl.etf.pj2.luka.util.SpisakPotjeraUtil;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integracioni testovi za I1–I8 (sudar) i I5 (potjernica) — vidi ZAHTJEVI.md za obrazloženje
 * zašto ručna provjera kroz GUI nije izvodljiva i zašto su ovi testovi glavni dokaz da sistem radi.
 */
@DisplayName("Integracija incidenata — sudar (I1-I8) i potjernica (I5), kraj-do-kraja")
class IntegracijaIncidentaTest {

    private boolean staroSudariOmoguceni;
    private double staraVjerovatnocaSudara;
    private long staroMinTrajanje;
    private long staroMaxTrajanje;
    private long staroMinTrajanjePotjernice;
    private long staroMaxTrajanjePotjernice;
    private File stariDirektorijumSudara;
    private File stariDirektorijumPotjernice;
    private long staroMaxCekanjeDolaska;
    private long stariIntervalProvjere;

    @TempDir
    File privremeniDirektorijum;

    @BeforeEach
    void postaviDeterministickoOkruzenje() {
        staroSudariOmoguceni = BrodThread.SUDARI_OMOGUCENI;
        staraVjerovatnocaSudara = BrodThread.VJEROVATNOCA_SUDARA;
        staroMinTrajanje = BrodThread.MIN_TRAJANJE_UVIDJAJA_MS;
        staroMaxTrajanje = BrodThread.MAX_TRAJANJE_UVIDJAJA_MS;
        staroMinTrajanjePotjernice = BrodThread.MIN_TRAJANJE_UVIDJAJA_POTJERNICE_MS;
        staroMaxTrajanjePotjernice = BrodThread.MAX_TRAJANJE_UVIDJAJA_POTJERNICE_MS;
        stariDirektorijumSudara = BrodThread.DIREKTORIJUM_INCIDENTA_SUDARA;
        stariDirektorijumPotjernice = BrodThread.DIREKTORIJUM_INCIDENTA_POTJERNICE;
        staroMaxCekanjeDolaska = KoordinatorUvidjaja.MAX_CEKANJE_DOLASKA_MS;
        stariIntervalProvjere = KoordinatorUvidjaja.INTERVAL_PROVJERE_DOLASKA_MS;

        BrodThread.SUDARI_OMOGUCENI = true;
        BrodThread.MIN_TRAJANJE_UVIDJAJA_MS = 50L;
        BrodThread.MAX_TRAJANJE_UVIDJAJA_MS = 50L;
        BrodThread.MIN_TRAJANJE_UVIDJAJA_POTJERNICE_MS = 50L;
        BrodThread.MAX_TRAJANJE_UVIDJAJA_POTJERNICE_MS = 50L;
        BrodThread.DIREKTORIJUM_INCIDENTA_SUDARA = privremeniDirektorijum;
        BrodThread.DIREKTORIJUM_INCIDENTA_POTJERNICE = privremeniDirektorijum;
        // Patrole u ovim testovima kreću sa dokova blizu ulaza, a incident nastaje daleko na
        // istoku — budžet čekanja dolaska mora biti dovoljan za tu stvarnu vožnju, ali ipak
        // ograničen da testovi ne kasne unedogled ako patrola stvarno ne stigne.
        KoordinatorUvidjaja.MAX_CEKANJE_DOLASKA_MS = 8000L;
        KoordinatorUvidjaja.INTERVAL_PROVJERE_DOLASKA_MS = 20L;

        SpisakPotjeraUtil.resetujKes();
    }

    @AfterEach
    void vratiPodrazumijevaneVrijednosti() {
        BrodThread.SUDARI_OMOGUCENI = staroSudariOmoguceni;
        BrodThread.VJEROVATNOCA_SUDARA = staraVjerovatnocaSudara;
        BrodThread.MIN_TRAJANJE_UVIDJAJA_MS = staroMinTrajanje;
        BrodThread.MAX_TRAJANJE_UVIDJAJA_MS = staroMaxTrajanje;
        BrodThread.MIN_TRAJANJE_UVIDJAJA_POTJERNICE_MS = staroMinTrajanjePotjernice;
        BrodThread.MAX_TRAJANJE_UVIDJAJA_POTJERNICE_MS = staroMaxTrajanjePotjernice;
        BrodThread.DIREKTORIJUM_INCIDENTA_SUDARA = stariDirektorijumSudara;
        BrodThread.DIREKTORIJUM_INCIDENTA_POTJERNICE = stariDirektorijumPotjernice;
        KoordinatorUvidjaja.MAX_CEKANJE_DOLASKA_MS = staroMaxCekanjeDolaska;
        KoordinatorUvidjaja.INTERVAL_PROVJERE_DOLASKA_MS = stariIntervalProvjere;
    }

    private static void cekajUslov(BooleanSupplier uslov, long timeoutMs) throws InterruptedException {
        long krajnjeVrijeme = System.currentTimeMillis() + timeoutMs;
        while (!uslov.getAsBoolean() && System.currentTimeMillis() < krajnjeVrijeme) {
            Thread.sleep(10);
        }
    }

    private File[] fajloviIncidenta() {
        File[] rezultat = privremeniDirektorijum.listFiles((dir, ime) -> ime.startsWith("incident-"));
        return rezultat != null ? rezultat : new File[0];
    }

    /**
     * Čeka da se pojavi fajl incidenta I da se stvarno uspješno učita — {@code sacuvaj()} prvo
     * kreira fajl pa tek onda upisuje sadržaj, pa {@link Incident#ucitaj(File)} pozvan taman kad se
     * fajl pojavi može zateći nedovršen upis i vratiti {@code null}.
     */
    private Incident cekajIUcitajIncident(long timeoutMs) throws InterruptedException {
        long krajnjeVrijeme = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < krajnjeVrijeme) {
            File[] fajlovi = fajloviIncidenta();
            if (fajlovi.length >= 1) {
                Incident ucitan = Incident.ucitaj(fajlovi[0]);
                if (ucitan != null) {
                    return ucitan;
                }
            }
            Thread.sleep(10);
        }
        return null;
    }

    /**
     * Popunjava sve dokove terminala fiktivnim plovilima OSIM kolone {@code slobodnaKolona}, koja
     * ostaje jedina slobodna — svaki brod koji uđe je time prisiljen da putuje cijelom dužinom
     * kanala do te kolone, dajući dovoljno prostora da brži brod stigne sporiji prije pristajanja.
     * Ne dira dokove koji su već zauzeti (npr. ranije ručno postavljena patrola) — samo one koji su
     * u tom trenutku slobodni.
     */
    private static void popuniOsimKolone(Terminal terminal, int slobodnaKolona) {
        for (Dok d : terminal.getDokovi()) {
            if (d.getLokacija().getY() == slobodnaKolona) {
                d.getLokacija().setTrenutnoPlovilo(null);
            } else if (d.isSlobodan()) {
                d.getLokacija().setTrenutnoPlovilo(
                        TestFactory.kontejnerski("FILL-" + terminal.getIdTerminala() + "-" + d.getOznakaVezova()));
            }
        }
    }

    /**
     * Pokreće dva plovila "tokom simulacije" (konstruktor {@code BrodThread(Plovilo, Luka)}, ne
     * predokovani) u istom terminalu, jedno sporo i jedno brzo, oba ciljajući isti (jedini
     * slobodan) daleki kraj kanala — brzo pretiče sporo i time genuinski izaziva sudar (I1), bez
     * ikakvog ručnog pozicioniranja niti direktnog poziva na koordinatora. Terminal mora već biti
     * pripremljen (npr. {@link #popuniOsimKolone}) prije poziva.
     */
    private BrodThread[] pokreniSudar(Luka luka, Terminal terminal, ExecutorService exec, String sufiksImo)
            throws InterruptedException {
        Plovilo sporoPlovilo = TestFactory.kontejnerski("SPORO-" + sufiksImo);
        sporoPlovilo.setBrzina(2.0);
        Plovilo brzoPlovilo = TestFactory.tanker("BRZO-" + sufiksImo);
        brzoPlovilo.setBrzina(500.0);

        BrodThread sporo = new BrodThread(sporoPlovilo, luka);
        exec.submit(sporo);
        cekajUslov(() -> sporo.getY() >= 2, 10_000);

        BrodThread brzo = new BrodThread(brzoPlovilo, luka);
        exec.submit(brzo);

        return new BrodThread[]{sporo, brzo};
    }

    // ------------------------------------------------------------------
    // TEST 1 — sudar tokom preticanja stvara Incident (I1)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("I1: sudar tokom stvarnog preticanja dva plovila dodata tokom simulacije stvara Incident sa tačno dva učesnika")
    void sudarTokomPreticanjaStvaraIncidentSaTacnoDvaUcesnika() throws Exception {
        BrodThread.VJEROVATNOCA_SUDARA = 1.0;

        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        popuniOsimKolone(t, 16);

        ExecutorService exec = Executors.newCachedThreadPool();
        try {
            BrodThread[] par = pokreniSudar(luka, t, exec, "T1");

            Incident incident = cekajIUcitajIncident(15_000);
            assertNotNull(incident, "Fajl incidenta mora nastati i biti čitljiv.");
            assertEquals(1, fajloviIncidenta().length, "Tačno jedan fajl incidenta mora nastati.");
            assertEquals(TipIncidenta.SUDAR, incident.getTip());
            assertEquals(2, incident.getUcesniciSudara().size(),
                    "Incident mora imati tačno dva učesnika sudara.");
            assertTrue(incident.getUcesniciSudara().contains(par[0].getPlovilo()));
            assertTrue(incident.getUcesniciSudara().contains(par[1].getPlovilo()));
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    // ------------------------------------------------------------------
    // TEST 2 — blokada saobraćaja i odziv službi (I2/I3/I4)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("I2/I3/I4: sudar blokira samo svoj terminal i dispečuje službena plovila, susjedni terminal radi nesmetano")
    void sudarBlokiraSamoSvojTerminalIDispecujeSluzbe() throws Exception {
        BrodThread.VJEROVATNOCA_SUDARA = 1.0;
        BrodThread.MIN_TRAJANJE_UVIDJAJA_MS = 2000L;
        BrodThread.MAX_TRAJANJE_UVIDJAJA_MS = 2000L;

        Luka luka = TestFactory.luka(2);
        Terminal t1 = luka.getTerminali().get(0);
        Terminal t2 = luka.getTerminali().get(1);
        popuniOsimKolone(t1, 16);

        ExecutorService exec = Executors.newCachedThreadPool();
        try {
            List<Dok> slobodniZaPatrole = new ArrayList<>();
            for (Dok d : t1.getDokovi()) {
                if (d.getLokacija().getY() != 16) {
                    slobodniZaPatrole.add(d);
                }
            }

            Plovilo vatrogasciPlovilo = TestFactory.tankerVatrogasci("VATROGASCI-T2");
            slobodniZaPatrole.get(0).getLokacija().setTrenutnoPlovilo(vatrogasciPlovilo);
            BrodThread vatrogasci = new BrodThread(vatrogasciPlovilo, luka, t1, slobodniZaPatrole.get(0));

            Plovilo obalskaPlovilo = TestFactory.kontejnerskiOS("OBALSKA-T2");
            slobodniZaPatrole.get(1).getLokacija().setTrenutnoPlovilo(obalskaPlovilo);
            BrodThread obalska = new BrodThread(obalskaPlovilo, luka, t1, slobodniZaPatrole.get(1));

            Plovilo carinaPlovilo = TestFactory.tankerCarina("CARINA-T2");
            slobodniZaPatrole.get(2).getLokacija().setTrenutnoPlovilo(carinaPlovilo);
            BrodThread carina = new BrodThread(carinaPlovilo, luka, t1, slobodniZaPatrole.get(2));

            List<BrodThread> patrole = List.of(vatrogasci, obalska, carina);
            for (BrodThread p : patrole) {
                exec.submit(p);
            }
            for (BrodThread p : patrole) {
                cekajUslov(() -> p.getZadatak() == Zadatak.PRIVEZAN, 10_000);
            }

            // Obično plovilo koje se stvarno kreće u susjednom terminalu (I4). Terminal 1 se
            // privremeno potpuno popuni (uključujući kolonu 16) da udjiULuku() sigurno preskoči
            // terminal 1 i ovo plovilo zaista uđe u terminal 2 — inače bi (pošto se terminali
            // obilaze po redoslijedu) moglo slučajno pristati u terminalu 1 i potrošiti vez
            // predviđen za sudar.
            List<Dok> privremenoZauzetiKolona16 = new ArrayList<>();
            for (Dok d : t1.getDokovi()) {
                if (d.getLokacija().getY() == 16 && d.isSlobodan()) {
                    d.getLokacija().setTrenutnoPlovilo(TestFactory.kontejnerski("TEMP-FILL-" + d.getOznakaVezova()));
                    privremenoZauzetiKolona16.add(d);
                }
            }

            Plovilo uSusjednomPlovilo = TestFactory.kontejnerski("SUSJEDNI-T2");
            uSusjednomPlovilo.setBrzina(300.0);
            BrodThread uSusjednom = new BrodThread(uSusjednomPlovilo, luka);
            exec.submit(uSusjednom);
            cekajUslov(() -> uSusjednom.getTrenutniTerminal() == t2, 10_000);
            assertSame(t2, uSusjednom.getTrenutniTerminal(),
                    "Susjedno plovilo mora ući baš u terminal 2, ne u terminal 1.");

            for (Dok d : privremenoZauzetiKolona16) {
                d.getLokacija().setTrenutnoPlovilo(null);
            }

            // Izazvati sudar u terminalu 1.
            BrodThread[] par = pokreniSudar(luka, t1, exec, "T2");

            cekajUslov(t1::isSaobracajBlokiran, 15_000);
            assertTrue(t1.isSaobracajBlokiran(), "Terminal sa sudarom mora biti blokiran (I3).");
            assertFalse(t2.isSaobracajBlokiran(), "Susjedni terminal ne smije biti pogođen (I4).");

            Plovilo obicnoBezVeze = TestFactory.kontejnerski("KONTROLA-T2");
            assertFalse(t1.smijeProci(obicnoBezVeze),
                    "Obično plovilo ne smije proći kroz blokirani terminal (I3).");

            // Bar jedno službeno plovilo mora biti pozvano na incident i dobiti rotaciju.
            cekajUslov(() -> patrole.stream().anyMatch(p -> p.getZadatak() == Zadatak.KA_INCIDENTU
                    || p.getZadatak() == Zadatak.NA_INCIDENTU), 10_000);
            boolean bardJednaOdazvana = false;
            for (BrodThread p : patrole) {
                Zadatak z = p.getZadatak();
                if (z == Zadatak.KA_INCIDENTU || z == Zadatak.NA_INCIDENTU) {
                    bardJednaOdazvana = true;
                    assertTrue(((SluzbenoPlovilo) p.getPlovilo()).isRotacija(),
                            "Odazvano službeno plovilo mora imati uključenu rotaciju.");
                    assertTrue(t1.smijeProci(p.getPlovilo()),
                            "Plovilo pod rotacijom smije proći kroz blokirani terminal (I3).");
                }
            }
            assertTrue(bardJednaOdazvana, "Bar jedno službeno plovilo mora biti pozvano na incident (I2).");

            // Susjedni terminal i dalje radi nesmetano — plovilo se pomjera.
            int yPrije = uSusjednom.getY();
            cekajUslov(() -> uSusjednom.getY() != yPrije || uSusjednom.getZadatak() == Zadatak.PRIVEZAN, 10_000);
            assertTrue(uSusjednom.getY() != yPrije || uSusjednom.getZadatak() == Zadatak.PRIVEZAN,
                    "Plovilo u susjednom terminalu mora nastaviti kretanje dok je terminal 1 blokiran (I4).");
            assertFalse(t2.isSaobracajBlokiran());

            // Nakon uviđaja, blokada mora biti skinuta. Budžet mora pokriti i eventualno čekanje
            // dolaska patrole (KoordinatorUvidjaja.MAX_CEKANJE_DOLASKA_MS) prije nego što uopšte
            // počne trajanje uviđaja.
            cekajUslov(() -> !t1.isSaobracajBlokiran(), 20_000);
            assertFalse(t1.isSaobracajBlokiran(), "Nakon uviđaja blokada mora biti skinuta.");
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    // ------------------------------------------------------------------
    // TEST 3 — binarni fajl incidenta (I6/I7)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("I6/I7: sudar upisuje binarni fajl incidenta sa istim učesnicima, vremenom, trajanjem i fotografijama")
    void sudarUpisujeBinarniFajlSaIstimPodacima() throws Exception {
        BrodThread.VJEROVATNOCA_SUDARA = 1.0;

        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        popuniOsimKolone(t, 16);

        ExecutorService exec = Executors.newCachedThreadPool();
        try {
            BrodThread[] par = pokreniSudar(luka, t, exec, "T3");

            Incident ucitan = cekajIUcitajIncident(15_000);
            assertNotNull(ucitan, "Fajl incidenta mora nastati i biti čitljiv.");
            assertEquals(1, fajloviIncidenta().length);
            assertEquals(2, ucitan.getUcesniciSudara().size());
            assertTrue(ucitan.getUcesniciSudara().contains(par[0].getPlovilo()));
            assertTrue(ucitan.getUcesniciSudara().contains(par[1].getPlovilo()));
            assertNotNull(ucitan.getVrijeme());
            assertTrue(ucitan.getTrajanjeUvidjajaMs() >= BrodThread.MIN_TRAJANJE_UVIDJAJA_MS);
            assertEquals(t.getIdTerminala(), ucitan.getIdTerminala());

            List<String> fotografije = ucitan.getApsolutnePutanjeFotografija();
            assertFalse(fotografije.isEmpty(), "Fotografije učesnika moraju biti zabilježene (D6).");
            assertTrue(fotografije.contains(par[0].getPlovilo().getFotografija().getAbsolutePath()));
            assertTrue(fotografije.contains(par[1].getPlovilo().getFotografija().getAbsolutePath()));
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    // ------------------------------------------------------------------
    // TEST 4 — raspetljavanje nakon uviđaja (I8)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("I8: nakon uviđaja učesnici sudara napuštaju terminal, patrola se raspetljava, broj raspoloživih vezova se ne mijenja")
    void raspetljavanjeNakonUvidjajaVracaStanjeTerminala() throws Exception {
        BrodThread.VJEROVATNOCA_SUDARA = 1.0;
        // Duže trajanje uviđaja (umjesto podrazumijevanih 50ms iz @BeforeEach) ostavlja učesnicima
        // sudara dovoljan budžet blokiranih pokušaja (maxBlokadaPokusaja() se izvodi baš iz ovog
        // intervala) da dočekaju kraj blokade i legitimno završe kroz sudarMoraNapustiti, umjesto
        // da odustanu od sopstvenog kretanja usljed prevremenog isteka budžeta.
        BrodThread.MIN_TRAJANJE_UVIDJAJA_MS = 1000L;
        BrodThread.MAX_TRAJANJE_UVIDJAJA_MS = 1000L;

        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        popuniOsimKolone(t, 16);

        ExecutorService exec = Executors.newCachedThreadPool();
        try {
            Dok dokPatrole = null;
            for (Dok d : t.getDokovi()) {
                if (d.getLokacija().getY() != 16) {
                    dokPatrole = d;
                    break;
                }
            }
            assertNotNull(dokPatrole);
            Plovilo patrolaPlovilo = TestFactory.tankerVatrogasci("PATROLA-T4");
            dokPatrole.getLokacija().setTrenutnoPlovilo(patrolaPlovilo);
            BrodThread patrola = new BrodThread(patrolaPlovilo, luka, t, dokPatrole);
            exec.submit(patrola);
            cekajUslov(() -> patrola.getZadatak() == Zadatak.PRIVEZAN, 10_000);

            int vezoviPrije = t.getBrojRaspolozivihVezova();

            BrodThread[] par = pokreniSudar(luka, t, exec, "T4");

            // Sačekati da koordinator dispečuje i završi uviđaj — praćeno preko fajla incidenta,
            // koji nastaje tek nakon što je cijeli uviđaj gotov (posljednji korak koordinatora).
            cekajUslov(() -> fajloviIncidenta().length >= 1, 20_000);
            assertEquals(1, fajloviIncidenta().length);

            // Rezervacija veza se otkazuje ODMAH čim je plovilo obilježeno kao učesnik sudara —
            // mnogo prije nego što stvarno fizički napusti terminal (napustiTerminal() hoda korak
            // po korak). Zato se broj raspoloživih vezova ne smije koristiti kao signal da su
            // učesnici zaista otišli — čeka se da obje niti stvarno nestanu iz registra aktivnih
            // plovila (uklanjaju se u finally bloku BrodThread.run(), bez obzira kako su završile).
            cekajUslov(() -> !luka.getAktivnaPlovila().contains(par[0])
                    && !luka.getAktivnaPlovila().contains(par[1]), 30_000);
            assertFalse(luka.getAktivnaPlovila().contains(par[0]), "Prvi učesnik sudara mora napustiti luku.");
            assertFalse(luka.getAktivnaPlovila().contains(par[1]), "Drugi učesnik sudara mora napustiti luku.");

            cekajUslov(() -> patrola.getZadatak() == Zadatak.PRIVEZAN || patrola.getZadatak() == Zadatak.NAPUSTA,
                    20_000);
            cekajUslov(() -> t.getBrojRaspolozivihVezova() == vezoviPrije, 10_000);

            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < t.getMatrica()[x].length; y++) {
                    Plovilo naPolju = t.getMatrica()[x][y].getTrenutnoPlovilo();
                    assertNotSame(par[0].getPlovilo(), naPolju, "Prvi učesnik sudara ne smije ostati u terminalu.");
                    assertNotSame(par[1].getPlovilo(), naPolju, "Drugi učesnik sudara ne smije ostati u terminalu.");
                }
            }

            assertTrue(patrola.getZadatak() == Zadatak.PRIVEZAN || patrola.getZadatak() == Zadatak.NAPUSTA,
                    "Nakon uviđaja patrola mora biti ili ponovo privezana ili je napustila terminal.");
            // Koordinator gasi rotaciju TEK nakon što raspetljajPatrole() vrati patrolu (koja
            // asinhrono, u svojoj niti, može stići do PRIVEZAN i prije nego što koordinatorova nit
            // izvrši gašenje) — zadatak==PRIVEZAN sam po sebi ne garantuje da je rotacija već ugašena.
            cekajUslov(() -> !((SluzbenoPlovilo) patrolaPlovilo).isRotacija(), 10_000);
            assertFalse(((SluzbenoPlovilo) patrolaPlovilo).isRotacija(), "Rotacija patrole mora biti ugašena nakon uviđaja.");

            assertEquals(vezoviPrije, t.getBrojRaspolozivihVezova(),
                    "Broj raspoloživih vezova mora se vratiti na vrijednost prije incidenta — bez procurjelih rezervacija.");
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    // ------------------------------------------------------------------
    // TEST 5 — potjernica bez blokade terminala (I5)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("I5: potjernica obalske straže pronalazi traženo plovilo i nikad ne blokira terminal")
    void potjernicaNikadNeBlokiraTerminal() throws Exception {
        BrodThread.VJEROVATNOCA_SUDARA = 0.0;
        BrodThread.MIN_TRAJANJE_UVIDJAJA_POTJERNICE_MS = 300L;
        BrodThread.MAX_TRAJANJE_UVIDJAJA_POTJERNICE_MS = 300L;

        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        popuniOsimKolone(t, 16);

        File spisakFajl = new File(privremeniDirektorijum, "spisak-potjera-T5.txt");
        String trazeniImo = "TRAZENO-T5";
        Files.writeString(spisakFajl.toPath(), trazeniImo + System.lineSeparator());

        ExecutorService exec = Executors.newCachedThreadPool();
        try {
            // Traženo plovilo, predokovano u redu 3 (dokovi), na koloni kojom će obalska straža
            // proći kroz red 2 (KANAL_ULAZ) — ta susjedna ćelija je tačka na kojoj provjeriPotjernicu()
            // detektuje poklapanje.
            Dok dokTrazenog = null;
            for (Dok d : t.getDokovi()) {
                if (d.getLokacija().getX() == 3 && d.getLokacija().getY() == 10) {
                    dokTrazenog = d;
                    break;
                }
            }
            assertNotNull(dokTrazenog);
            Plovilo trazenoPlovilo = TestFactory.kontejnerski(trazeniImo);
            dokTrazenog.getLokacija().setTrenutnoPlovilo(trazenoPlovilo);
            BrodThread trazenaNit = new BrodThread(trazenoPlovilo, luka, t, dokTrazenog);
            exec.submit(trazenaNit);
            cekajUslov(() -> trazenaNit.getZadatak() == Zadatak.PRIVEZAN, 10_000);

            // Treće, obično plovilo, koje se kreće nezavisno tokom potjere.
            Plovilo obicnoPlovilo = TestFactory.kruzer("OBICNO-T5");
            obicnoPlovilo.setBrzina(2.0);
            BrodThread obicno = new BrodThread(obicnoPlovilo, luka);
            exec.submit(obicno);
            cekajUslov(() -> obicno.getY() >= 2, 10_000);

            // Obalska straža sa spiskom potjera koji sadrži IMO traženog plovila, cilja daleki
            // slobodan dok (kolona 16) — usput prolazi kroz kolonu 10 u redu 2, tik pored traženog.
            KontejnerskiBrodObalskaStraza obalskaPlovilo = new KontejnerskiBrodObalskaStraza(
                    "ObalskaStraza-T5", "OBALSKA-T5", "M-OBALSKA-T5", "REG-OBALSKA-T5",
                    TestFactory.FOTO, 1500, spisakFajl);
            obalskaPlovilo.setBrzina(30.0);
            BrodThread obalskaNit = new BrodThread(obalskaPlovilo, luka);

            AtomicBoolean nikadaNijeBioBlokiran = new AtomicBoolean(true);
            AtomicBoolean posmatracGotov = new AtomicBoolean(false);
            Future<?> posmatrac = exec.submit(() -> {
                while (!posmatracGotov.get()) {
                    if (t.isSaobracajBlokiran()) {
                        nikadaNijeBioBlokiran.set(false);
                    }
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });

            exec.submit(obalskaNit);

            // Potjera je počela: traženo plovilo je pozvano da napusti (POD_PRATNJOM), ili je
            // obalska straža uključila rotaciju.
            cekajUslov(() -> trazenaNit.getZadatak() == Zadatak.POD_PRATNJOM
                    || obalskaPlovilo.isRotacija(), 15_000);
            assertTrue(trazenaNit.getZadatak() == Zadatak.POD_PRATNJOM || obalskaPlovilo.isRotacija(),
                    "Potjera mora biti pokrenuta — traženo plovilo pozvano ili rotacija obalske straže uključena.");

            // Obično plovilo mora nastaviti kretanje tokom potjere.
            int yPrije = obicno.getY();
            cekajUslov(() -> obicno.getY() != yPrije || obicno.getZadatak() == Zadatak.PRIVEZAN, 10_000);
            assertTrue(obicno.getY() != yPrije || obicno.getZadatak() == Zadatak.PRIVEZAN,
                    "Treće, obično plovilo mora nastaviti kretanje tokom potjere.");

            Incident incident = cekajIUcitajIncident(20_000);
            posmatracGotov.set(true);
            posmatrac.get(5, TimeUnit.SECONDS);

            assertTrue(nikadaNijeBioBlokiran.get(),
                    "Terminal nikad ne smije biti blokiran tokom potjere (I5) — to je ključna razlika u odnosu na uviđaj sudara.");

            assertNotNull(incident, "Fajl incidenta mora nastati i biti čitljiv.");
            assertEquals(1, fajloviIncidenta().length);
            assertEquals(TipIncidenta.POTJERNICA, incident.getTip());
            assertEquals(1, incident.getUcesniciSudara().size());
            assertEquals(trazenoPlovilo, incident.getUcesniciSudara().get(0));
            assertEquals(1, incident.getOdazvanaSluzbenaPlovila().size());
            assertEquals(obalskaPlovilo, incident.getOdazvanaSluzbenaPlovila().get(0));
            assertEquals(300L, incident.getTrajanjeUvidjajaMs(),
                    "Trajanje potjere mora doći iz konstanti specifičnih za potjernicu, ne opšteg uviđaja.");
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
