package org.unibl.etf.pj2.luka.simulation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.model.classes.Dok;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.model.classes.TankerVatrogasci;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KoordinatorUvidjaja — orkestracija uviđaja (D1/I2/I3/I4)")
class KoordinatorUvidjajaTest {

    private long staroMinTrajanje;
    private long staroMaxTrajanje;
    private long staroCekanjeDolaska;
    private long stariIntervalProvjere;
    private Path privremeniDirektorijum;

    @BeforeEach
    void postaviDeterministickoOkruzenje() throws IOException {
        staroMinTrajanje = BrodThread.MIN_TRAJANJE_UVIDJAJA_MS;
        staroMaxTrajanje = BrodThread.MAX_TRAJANJE_UVIDJAJA_MS;
        staroCekanjeDolaska = KoordinatorUvidjaja.MAX_CEKANJE_DOLASKA_MS;
        stariIntervalProvjere = KoordinatorUvidjaja.INTERVAL_PROVJERE_DOLASKA_MS;

        BrodThread.MIN_TRAJANJE_UVIDJAJA_MS = 50L;
        BrodThread.MAX_TRAJANJE_UVIDJAJA_MS = 50L;
        KoordinatorUvidjaja.INTERVAL_PROVJERE_DOLASKA_MS = 20L;

        privremeniDirektorijum = Files.createTempDirectory("koordinator-uvidjaja-test-");
    }

    @AfterEach
    void vratiPodrazumijevaneVrijednosti() throws IOException {
        BrodThread.MIN_TRAJANJE_UVIDJAJA_MS = staroMinTrajanje;
        BrodThread.MAX_TRAJANJE_UVIDJAJA_MS = staroMaxTrajanje;
        KoordinatorUvidjaja.MAX_CEKANJE_DOLASKA_MS = staroCekanjeDolaska;
        KoordinatorUvidjaja.INTERVAL_PROVJERE_DOLASKA_MS = stariIntervalProvjere;

        if (privremeniDirektorijum != null && Files.exists(privremeniDirektorijum)) {
            try (Stream<Path> tok = Files.walk(privremeniDirektorijum)) {
                tok.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignore) {
                        // Čišćenje nije kritično za ispravnost testa.
                    }
                });
            }
        }
    }

    private static BrodThread postaviUKanalu(Plovilo p, Luka luka, Terminal terminal, int x, int y) {
        BrodThread bt = new BrodThread(p, luka);
        assertTrue(bt.pokusajUciUTerminal(terminal));
        if (x != 0 || y != Terminal.KOLONA_ULAZ) {
            assertTrue(bt.pomjeriNaPolje(x, y));
        }
        luka.getAktivnaPlovila().add(bt);
        return bt;
    }

    private File[] fajloviIncidenta() {
        return privremeniDirektorijum.toFile().listFiles((dir, ime) -> ime.startsWith("incident-"));
    }

    private static void cekajUslov(BooleanSupplier uslov, long timeoutMs) throws InterruptedException {
        long krajnjeVrijeme = System.currentTimeMillis() + timeoutMs;
        while (!uslov.getAsBoolean() && System.currentTimeMillis() < krajnjeVrijeme) {
            Thread.sleep(20);
        }
    }

    @Test
    @DisplayName("Za vrijeme uviđaja je blokiran samo ciljni terminal (I3), a susjedni radi normalno (I4); "
            + "obično plovilo ne napreduje, plovilo pod rotacijom napreduje")
    void terminalBlokiranSamoTokomUvidjajaIRotacijaOmogucavaProlaz() throws Exception {
        Luka luka = TestFactory.luka(2);
        Terminal t = luka.getTerminali().get(0);
        Terminal susjedni = luka.getTerminali().get(1);

        // Postaviti duže trajanje uviđaja samo za ovaj test, da imamo prostora za asertacije u toku njega.
        BrodThread.MIN_TRAJANJE_UVIDJAJA_MS = 1500L;
        BrodThread.MAX_TRAJANJE_UVIDJAJA_MS = 1500L;

        BrodThread obicno = postaviUKanalu(TestFactory.kontejnerski("OBICNO"), luka, t, Terminal.KANAL_ULAZ, 5);
        BrodThread patrola = postaviUKanalu(TestFactory.tankerVatrogasci("PATROLA"), luka, t, Terminal.KANAL_ULAZ, 4);
        BrodThread uSusjednom = postaviUKanalu(TestFactory.kontejnerski("SUSJED"), luka, susjedni, Terminal.KANAL_ULAZ, 5);

        Plovilo drugiUcesnikSudara = TestFactory.kontejnerski("DRUGI-UCESNIK");
        KoordinatorUvidjaja koordinator = new KoordinatorUvidjaja(
                luka, t, List.of(obicno.getPlovilo(), drugiUcesnikSudara),
                Terminal.KANAL_ULAZ, 5, privremeniDirektorijum.toFile());
        Thread nit = new Thread(koordinator);
        nit.start();

        Thread.sleep(200);

        assertTrue(t.isSaobracajBlokiran(), "Terminal na kojem je incident mora biti blokiran tokom uviđaja.");
        assertFalse(susjedni.isSaobracajBlokiran(), "Susjedni terminal ne smije biti pogođen (I4).");

        assertFalse(obicno.pomjeriNaPolje(Terminal.KANAL_ULAZ, 6),
                "Obično plovilo ne smije napredovati dok je terminal blokiran.");
        assertTrue(patrola.pomjeriNaPolje(Terminal.KANAL_ULAZ, 3),
                "Dispečovana patrola pod rotacijom mora napredovati i kroz blokadu.");
        assertTrue(uSusjednom.pomjeriNaPolje(Terminal.KANAL_ULAZ, 6),
                "Plovilo u susjednom terminalu se kreće nezavisno od blokade (I4).");

        nit.join(10_000);
        assertFalse(nit.isAlive(), "Koordinator mora završiti uviđaj u razumnom vremenu.");
        assertFalse(t.isSaobracajBlokiran(), "Nakon uviđaja blokada mora biti skinuta.");
    }

    @Test
    @DisplayName("Binarni fajl incidenta se upisuje i Incident.ucitaj() vraća iste učesnike i odazvanu patrolu")
    void binarniFajlNastajeIIncidentUcitajVracaIsteUcesnike() throws Exception {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);

        Plovilo ucesnik1 = TestFactory.kontejnerski("SUDAR-A");
        Plovilo ucesnik2 = TestFactory.tanker("SUDAR-B");
        postaviUKanalu(TestFactory.tankerVatrogasci("PATROLA-BIN"), luka, t, Terminal.KANAL_ULAZ, 4);

        KoordinatorUvidjaja koordinator = new KoordinatorUvidjaja(
                luka, t, List.of(ucesnik1, ucesnik2), Terminal.KANAL_ULAZ, 5, privremeniDirektorijum.toFile());
        Thread nit = new Thread(koordinator);
        nit.start();
        nit.join(10_000);
        assertFalse(nit.isAlive());

        File[] fajlovi = fajloviIncidenta();
        assertNotNull(fajlovi);
        assertEquals(1, fajlovi.length, "Tačno jedan fajl incidenta mora biti napisan.");

        Incident ucitan = Incident.ucitaj(fajlovi[0]);
        assertNotNull(ucitan);
        assertEquals(List.of(ucesnik1, ucesnik2), ucitan.getUcesniciSudara());
        assertEquals(1, ucitan.getOdazvanaSluzbenaPlovila().size());
        assertEquals(t.getIdTerminala(), ucitan.getIdTerminala());
    }

    @Test
    @DisplayName("Nedostatak sve tri patrolne službe ne ruši uviđaj — incident se i dalje pravi, bez odazvanih plovila")
    void nedostatakSvihSluzbiNeRusiUvidjaj() throws Exception {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);

        KoordinatorUvidjaja koordinator = new KoordinatorUvidjaja(
                luka, t, List.of(TestFactory.kontejnerski("SUDAR-C"), TestFactory.tanker("SUDAR-D")),
                Terminal.KANAL_ULAZ, 5, privremeniDirektorijum.toFile());
        Thread nit = new Thread(koordinator);
        nit.start();
        nit.join(10_000);

        assertFalse(nit.isAlive(), "Uviđaj se ne smije zaglaviti zbog nedostatka patrola.");
        assertFalse(t.isSaobracajBlokiran());

        File[] fajlovi = fajloviIncidenta();
        assertNotNull(fajlovi);
        assertEquals(1, fajlovi.length);
        Incident ucitan = Incident.ucitaj(fajlovi[0]);
        assertNotNull(ucitan);
        assertTrue(ucitan.getOdazvanaSluzbenaPlovila().isEmpty());
    }

    @Test
    @DisplayName("Patrola koja ne stigne do polja pored incidenta ne blokira uviđaj zauvijek — vremensko ograničenje")
    void patrolaKojaNeStizeNeBlokiraUvidjajZauvijek() throws Exception {
        KoordinatorUvidjaja.MAX_CEKANJE_DOLASKA_MS = 300L;

        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        // Patrola je registrovana, ali nikad ne stiže do polja pored incidenta (Korak 4 — buđenje i
        // preusmjeravanje privezanih patrola — tek dolazi), pa se čekanje mora ograničiti vremenom.
        postaviUKanalu(TestFactory.tankerVatrogasci("PATROLA-DALEKO"), luka, t, Terminal.KANAL_ULAZ, 16);

        long pocetak = System.currentTimeMillis();
        KoordinatorUvidjaja koordinator = new KoordinatorUvidjaja(
                luka, t, List.of(TestFactory.kontejnerski("SUDAR-E"), TestFactory.tanker("SUDAR-F")),
                Terminal.KANAL_ULAZ, 2, privremeniDirektorijum.toFile());
        Thread nit = new Thread(koordinator);
        nit.start();
        nit.join(10_000);
        long trajanje = System.currentTimeMillis() - pocetak;

        assertFalse(nit.isAlive(), "Koordinator se ne smije zaglaviti čekajući patrolu koja ne stiže.");
        assertTrue(trajanje < 10_000, "Čekanje na patrolu mora biti ograničeno vremenskim budžetom.");
    }

    // ------------------------------------------------------------------
    // Korak 5 — raspetljavanje nakon uviđaja
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Korak 5: službeno plovilo se ponovo privezuje na slobodan dok nakon uviđaja, rotacija ugašena")
    void sluzbenoPloviloSeVracaNaSlobodanDokNakonUvidjajaIRotacijaSeGasi() throws Exception {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);

        TankerVatrogasci patrolaPlovilo = TestFactory.tankerVatrogasci("PATROLA-REDOCK");
        BrodThread bt = new BrodThread(patrolaPlovilo, luka);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> future = exec.submit(bt);
        try {
            cekajUslov(() -> bt.getZadatak() == Zadatak.PRIVEZAN, 10_000);

            KoordinatorUvidjaja koordinator = new KoordinatorUvidjaja(
                    luka, t, List.of(TestFactory.kontejnerski("SUDAR-G"), TestFactory.tanker("SUDAR-H")),
                    Terminal.KANAL_ULAZ, 5, privremeniDirektorijum.toFile());
            Thread nitKoordinatora = new Thread(koordinator);
            nitKoordinatora.start();
            nitKoordinatora.join(15_000);
            assertFalse(nitKoordinatora.isAlive());

            cekajUslov(() -> bt.getZadatak() == Zadatak.PRIVEZAN, 10_000);
            assertTrue(bt.isPrivezan(), "Patrola se mora ponovo privezati nakon uviđaja.");
            assertEquals(Zadatak.PRIVEZAN, bt.getZadatak());
            assertFalse(patrolaPlovilo.isRotacija(), "Rotacija mora biti ugašena nakon uviđaja.");
            assertFalse(future.isDone(), "Nit ostaje živa, parkirana, spremna za naredni incident.");
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Korak 5: ako nema slobodnog doka, službeno plovilo napušta terminal umjesto povratka u PRIVEZAN")
    void sluzbenoPloviloNapustaTerminalAkoNemaSlobodnogDoka() throws Exception {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);

        TankerVatrogasci patrolaPlovilo = TestFactory.tankerVatrogasci("PATROLA-NODOCK");
        BrodThread bt = new BrodThread(patrolaPlovilo, luka);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> future = exec.submit(bt);
        try {
            cekajUslov(() -> bt.getZadatak() == Zadatak.PRIVEZAN, 10_000);

            Dok originalniDok = null;
            for (Dok d : t.getDokovi()) {
                if (!d.isSlobodan()) {
                    originalniDok = d;
                } else {
                    d.getLokacija().setTrenutnoPlovilo(TestFactory.kontejnerski("POPUNA-" + d.getOznakaVezova()));
                }
            }
            assertNotNull(originalniDok, "Patrolin originalni vez mora biti pronađen.");

            KoordinatorUvidjaja koordinator = new KoordinatorUvidjaja(
                    luka, t, List.of(TestFactory.kontejnerski("SUDAR-I"), TestFactory.tanker("SUDAR-J")),
                    Terminal.KANAL_ULAZ, 5, privremeniDirektorijum.toFile());
            Thread nitKoordinatora = new Thread(koordinator);
            nitKoordinatora.start();

            // Čim patrola napusti svoj originalni vez radi incidenta, odmah ga popuniti — simulira
            // scenario u kojem, do trenutka raspetljavanja, nijedan vez više nije slobodan za povratak.
            final Dok cekaniDok = originalniDok;
            boolean popunjen = false;
            long krajCekanja = System.currentTimeMillis() + 10_000;
            while (System.currentTimeMillis() < krajCekanja) {
                if (cekaniDok.isSlobodan()) {
                    cekaniDok.getLokacija().setTrenutnoPlovilo(TestFactory.kontejnerski("POPUNA-ORIG"));
                    popunjen = true;
                    break;
                }
                Thread.sleep(2);
            }
            assertTrue(popunjen, "Patrola nije napustila svoj originalni vez u razumnom vremenu.");

            nitKoordinatora.join(15_000);
            assertFalse(nitKoordinatora.isAlive());

            cekajUslov(future::isDone, 10_000);
            assertTrue(future.isDone(), "Bez slobodnog doka patrola mora napustiti terminal (nit se gasi).");
            assertEquals(Zadatak.NAPUSTA, bt.getZadatak());
            assertFalse(luka.getAktivnaPlovila().contains(bt),
                    "Nit koja je napustila terminal ne smije ostati u registru aktivnih plovila.");
            assertFalse(patrolaPlovilo.isRotacija());
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Korak 5: plovilo označeno kao učesnik sudara napušta terminal umjesto privezivanja")
    void ucesnikSudaraNapustaTerminalUmjestoPrivezivanja() throws Exception {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        Plovilo p = TestFactory.kontejnerski("UCESNIK-SUDARA");
        BrodThread bt = new BrodThread(p, luka);
        bt.oznaciKaoUcesnikaSudara();

        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> future = exec.submit(bt);
        try {
            cekajUslov(future::isDone, 15_000);
            assertTrue(future.isDone(),
                    "Plovilo obilježeno kao učesnik sudara mora završiti (napustiti luku), ne privezati se.");
            assertFalse(bt.isPrivezan(), "Učesnik sudara se ne smije privezati.");
            assertEquals(30, t.getBrojSlobodnihVezova(), "Nijedan vez ne smije ostati zauzet.");
            assertEquals(30, t.getBrojRaspolozivihVezova(), "Rezervacija veza mora biti oslobođena.");
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Korak 5: službeno plovilo koje se vratilo u PRIVEZAN se može ponovo poslati na naredni incident")
    void sluzbenoPloviloSeMozePonovoPoslatiNaNarredniIncident() throws Exception {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);

        TankerVatrogasci patrolaPlovilo = TestFactory.tankerVatrogasci("PATROLA-DVA-INCIDENTA");
        BrodThread bt = new BrodThread(patrolaPlovilo, luka);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> future = exec.submit(bt);
        try {
            cekajUslov(() -> bt.getZadatak() == Zadatak.PRIVEZAN, 10_000);

            KoordinatorUvidjaja prviIncident = new KoordinatorUvidjaja(
                    luka, t, List.of(TestFactory.kontejnerski("SUDAR-K"), TestFactory.tanker("SUDAR-L")),
                    Terminal.KANAL_ULAZ, 5, privremeniDirektorijum.toFile());
            Thread prvaNit = new Thread(prviIncident);
            prvaNit.start();
            prvaNit.join(15_000);
            cekajUslov(() -> bt.getZadatak() == Zadatak.PRIVEZAN, 10_000);
            assertTrue(bt.isPrivezan(), "Nakon prvog uviđaja patrola mora ponovo biti privezana.");

            KoordinatorUvidjaja drugiIncident = new KoordinatorUvidjaja(
                    luka, t, List.of(TestFactory.kontejnerski("SUDAR-M"), TestFactory.tanker("SUDAR-N")),
                    Terminal.KANAL_ULAZ, 6, privremeniDirektorijum.toFile());
            Thread drugaNit = new Thread(drugiIncident);
            drugaNit.start();
            drugaNit.join(15_000);
            cekajUslov(() -> bt.getZadatak() == Zadatak.PRIVEZAN, 10_000);

            assertTrue(bt.isPrivezan(), "Patrola mora moći odgovoriti i na drugi, uzastopni incident.");
            assertFalse(future.isDone());
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
