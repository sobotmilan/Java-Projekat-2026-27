package org.unibl.etf.pj2.luka.simulation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.model.classes.TankerObalskaStraza;
import org.unibl.etf.pj2.luka.testutil.TestFactory;
import org.unibl.etf.pj2.luka.util.SpisakPotjeraUtil;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BrodThread — potjernica (I5): detekcija (Korak 2), pratnja ka izlazu (Korak 3) i evidencija (Korak 4)")
class BrodThreadPotjernicaTest {

    private long staroMinPotjernice;
    private long staroMaxPotjernice;
    private Path privremeniDirektorijum;

    @BeforeEach
    void spustiTrajanjePotjernice() throws IOException {
        staroMinPotjernice = BrodThread.MIN_TRAJANJE_UVIDJAJA_POTJERNICE_MS;
        staroMaxPotjernice = BrodThread.MAX_TRAJANJE_UVIDJAJA_POTJERNICE_MS;
        BrodThread.MIN_TRAJANJE_UVIDJAJA_POTJERNICE_MS = 50L;
        BrodThread.MAX_TRAJANJE_UVIDJAJA_POTJERNICE_MS = 50L;

        privremeniDirektorijum = Files.createTempDirectory("potjernica-incident-test-");
        BrodThread.DIREKTORIJUM_INCIDENTA_POTJERNICE = privremeniDirektorijum.toFile();
    }

    @AfterEach
    void vratiTrajanjePotjernice() throws IOException {
        BrodThread.MIN_TRAJANJE_UVIDJAJA_POTJERNICE_MS = staroMinPotjernice;
        BrodThread.MAX_TRAJANJE_UVIDJAJA_POTJERNICE_MS = staroMaxPotjernice;
        BrodThread.DIREKTORIJUM_INCIDENTA_POTJERNICE = null;

        if (privremeniDirektorijum != null && Files.exists(privremeniDirektorijum)) {
            try (Stream<Path> tok = Files.walk(privremeniDirektorijum)) {
                tok.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    private File[] fajloviIncidenta() {
        return privremeniDirektorijum.toFile().listFiles((dir, ime) -> ime.endsWith(".ser"));
    }

    private static void cekajUslov(BooleanSupplier uslov, long timeoutMs) throws InterruptedException {
        long krajnjeVrijeme = System.currentTimeMillis() + timeoutMs;
        while (!uslov.getAsBoolean() && System.currentTimeMillis() < krajnjeVrijeme) {
            Thread.sleep(20);
        }
    }

    private static TankerObalskaStraza obalskaStraza(String imo, File spisak) {
        return new TankerObalskaStraza("OS-" + imo, imo, "M-" + imo, "REG-" + imo,
                TestFactory.FOTO, 120000.0, spisak);
    }

    private static File napisiSpisak(String sadrzaj) throws Exception {
        Path fajl = Files.createTempFile("potjernica-test-", ".txt");
        Files.writeString(fajl, sadrzaj);
        fajl.toFile().deleteOnExit();
        return fajl.toFile();
    }

    @Test
    @DisplayName("Plovilo sa spiska na susjednom polju se detektuje")
    void ploviloSaSpiskaSeDetektuje() throws Exception {
        SpisakPotjeraUtil.resetujKes();
        File spisak = napisiSpisak("TRAZENI-1\n");

        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);

        BrodThread obalska = new BrodThread(obalskaStraza("OS-1", spisak), luka);
        assertTrue(obalska.pokusajUciUTerminal(t));
        assertTrue(obalska.pomjeriNaPolje(Terminal.KANAL_IZLAZ, 0));
        assertTrue(obalska.pomjeriNaPolje(Terminal.KANAL_IZLAZ, 1));

        Plovilo trazeno = TestFactory.kontejnerski("TRAZENI-1");
        t.getMatrica()[Terminal.KANAL_ULAZ][1].setTrenutnoPlovilo(trazeno);

        assertSame(trazeno, obalska.provjeriPotjernicu());
    }

    @Test
    @DisplayName("Plovilo van spiska na susjednom polju se ne detektuje")
    void ploviloVanSpiskaSeNeDetektuje() throws Exception {
        SpisakPotjeraUtil.resetujKes();
        File spisak = napisiSpisak("TRAZENI-1\n");

        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);

        BrodThread obalska = new BrodThread(obalskaStraza("OS-2", spisak), luka);
        assertTrue(obalska.pokusajUciUTerminal(t));
        assertTrue(obalska.pomjeriNaPolje(Terminal.KANAL_IZLAZ, 0));
        assertTrue(obalska.pomjeriNaPolje(Terminal.KANAL_IZLAZ, 1));

        Plovilo nedokazano = TestFactory.kontejnerski("NEVIN-1");
        t.getMatrica()[Terminal.KANAL_ULAZ][1].setTrenutnoPlovilo(nedokazano);

        assertNull(obalska.provjeriPotjernicu());
    }

    @Test
    @DisplayName("Obično plovilo ne detektuje ništa, čak ni kad mu je susjedov IMO na spisku")
    void obicnoPloviloNeDetektujeNistaIakoJeSusjedNaSpisku() throws Exception {
        SpisakPotjeraUtil.resetujKes();
        File spisak = napisiSpisak("TRAZENI-1\n");
        // Sam spisak se ovdje ne koristi (obično plovilo ga uopšte ne posjeduje) — fajl je tu
        // samo da IMO "TRAZENI-1" zaista postoji na nekom spisku, kao dokaz da provjera nikad
        // i ne dopre do njega za ovaj tip plovila.
        SpisakPotjeraUtil.ucitaj(spisak);

        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);

        BrodThread obicno = new BrodThread(TestFactory.kontejnerski("OBICNO-1"), luka);
        assertTrue(obicno.pokusajUciUTerminal(t));
        assertTrue(obicno.pomjeriNaPolje(Terminal.KANAL_IZLAZ, 0));
        assertTrue(obicno.pomjeriNaPolje(Terminal.KANAL_IZLAZ, 1));

        Plovilo trazeno = TestFactory.kontejnerski("TRAZENI-1");
        t.getMatrica()[Terminal.KANAL_ULAZ][1].setTrenutnoPlovilo(trazeno);

        assertNull(obicno.provjeriPotjernicu(),
                "Provjera se radi samo ako plovilo implementira ObalskaStraza.");
    }

    @Test
    @DisplayName("Obalska straža bez dodijeljenog spiska (null) ne detektuje ništa")
    void obalskaStrazaBezSpiskaNeDetektujeNista() {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);

        BrodThread obalska = new BrodThread(obalskaStraza("OS-3", null), luka);
        assertTrue(obalska.pokusajUciUTerminal(t));
        assertTrue(obalska.pomjeriNaPolje(Terminal.KANAL_IZLAZ, 0));
        assertTrue(obalska.pomjeriNaPolje(Terminal.KANAL_IZLAZ, 1));

        Plovilo trazeno = TestFactory.kontejnerski("TRAZENI-1");
        t.getMatrica()[Terminal.KANAL_ULAZ][1].setTrenutnoPlovilo(trazeno);

        assertNull(obalska.provjeriPotjernicu());
    }

    // ------------------------------------------------------------------
    // Korak 3 — pratnja ka izlazu
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Korak 3: privezano traženo plovilo se budi preko pozoviNaPratnju() i napušta terminal")
    void privezanoPloviloSeBudiPrekoPozoviNaPratnjuINapusta() throws Exception {
        Luka luka = TestFactory.luka(1);
        BrodThread bt = new BrodThread(TestFactory.kontejnerski("TRAZENI-DIREKT"), luka);

        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> future = exec.submit(bt);
        try {
            cekajUslov(() -> bt.getZadatak() == Zadatak.PRIVEZAN, 10_000);
            assertTrue(bt.isPrivezan());

            bt.pozoviNaPratnju();
            assertEquals(Zadatak.POD_PRATNJOM, bt.getZadatak(),
                    "Zadatak se mora promijeniti sinhrono, u samom pozivu pozoviNaPratnju().");

            cekajUslov(future::isDone, 15_000);
            assertTrue(future.isDone(), "Probuđeno plovilo mora naposljetku napustiti terminal.");
            assertFalse(luka.getAktivnaPlovila().contains(bt),
                    "Nit koja je napustila terminal ne smije ostati u registru aktivnih plovila.");
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("pozoviNaPratnju() nema efekta na plovilo koje još nije PRIVEZAN")
    void pozoviNaPratnjuNemaEfektaAkoNijePrivezan() {
        Luka luka = TestFactory.luka(1);
        BrodThread bt = new BrodThread(TestFactory.kontejnerski("NIJE-PRIVEZAN"), luka);

        assertEquals(Zadatak.KA_DOKU, bt.getZadatak());
        bt.pozoviNaPratnju();

        assertEquals(Zadatak.KA_DOKU, bt.getZadatak(),
                "Plovilo koje nije privezano (nit nije ni pokrenuta) se ne preusmjerava.");
    }

    @Test
    @DisplayName("Korak 3: terminal ostaje neblokiran tokom cijele pratnje, treće plovilo se normalno "
            + "kreće, i oba učesnika (obalska straža i traženo plovilo) na kraju napuštaju terminal")
    void terminalOstajeNeblokiranTokomPratnjeIObaUcesnikaOdlaze() throws Exception {
        SpisakPotjeraUtil.resetujKes();
        String imoTrazeno = "TRAZENI-K3";
        File spisak = napisiSpisak(imoTrazeno + "\n");

        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);

        // Rezerviše prvi dok fiktivnim plovilom (nikad se ne otkazuje) da bi se kontrolisalo koji
        // dok stvarno dobija traženo plovilo — determinizam bez direktnog upisa u matricu.
        t.rezervisiSlobodanDok(TestFactory.kontejnerski("FILLER-K3"));

        BrodThread trazenaNit = new BrodThread(TestFactory.kontejnerski(imoTrazeno), luka);
        TankerObalskaStraza obalskoPlovilo = obalskaStraza("K3", spisak);
        BrodThread obalskaNit = new BrodThread(obalskoPlovilo, luka);
        BrodThread treceNit = new BrodThread(TestFactory.kontejnerski("TRECE-K3"), luka);

        ExecutorService exec = Executors.newFixedThreadPool(3);
        AtomicBoolean blokiranoIkad = new AtomicBoolean(false);
        AtomicBoolean rotacijaViđenaTrue = new AtomicBoolean(false);
        AtomicBoolean posmatracAktivan = new AtomicBoolean(true);
        Thread posmatrac = new Thread(() -> {
            while (posmatracAktivan.get()) {
                if (t.isSaobracajBlokiran()) {
                    blokiranoIkad.set(true);
                }
                if (obalskoPlovilo.isRotacija()) {
                    rotacijaViđenaTrue.set(true);
                }
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        posmatrac.setDaemon(true);
        posmatrac.start();

        try {
            Future<?> trazenaFuture = exec.submit(trazenaNit);
            cekajUslov(trazenaNit::isPrivezan, 15_000);
            assertTrue(trazenaNit.isPrivezan(), "Traženo plovilo se nije privezalo na vrijeme.");

            Future<?> treceFuture = exec.submit(treceNit);
            Future<?> obalskaFuture = exec.submit(obalskaNit);

            cekajUslov(() -> obalskaFuture.isDone() && trazenaFuture.isDone(), 30_000);

            posmatracAktivan.set(false);
            posmatrac.join(2_000);

            assertTrue(obalskaFuture.isDone(), "Obalska straža mora naposljetku napustiti terminal.");
            assertTrue(trazenaFuture.isDone(), "Traženo plovilo mora naposljetku napustiti terminal.");
            assertFalse(blokiranoIkad.get(),
                    "Terminal ne smije biti blokiran ni u jednom trenutku tokom potjernice — to je "
                            + "ključna razlika potjernice u odnosu na običan uviđaj.");
            assertTrue(rotacijaViđenaTrue.get(),
                    "Obalska straža mora upaliti rotaciju dok prati traženo plovilo.");
            assertFalse(obalskoPlovilo.isRotacija(),
                    "Rotacija mora biti ugašena nakon što se potjernica završi.");
            assertFalse(luka.getAktivnaPlovila().contains(obalskaNit));
            assertFalse(luka.getAktivnaPlovila().contains(trazenaNit));

            cekajUslov(treceNit::isPrivezan, 15_000);
            assertTrue(treceNit.isPrivezan(),
                    "Treće, nepovezano plovilo se mora normalno privezati — potjernica ne smije "
                            + "ometati saobraćaj ostalih plovila.");
            assertFalse(treceFuture.isDone(), "Treće plovilo ostaje privezano, ne napušta terminal.");
        } finally {
            posmatracAktivan.set(false);
            exec.shutdownNow();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    // ------------------------------------------------------------------
    // Korak 4 — evidencija
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Korak 4: potjernica upisuje binarni fajl incidenta sa oba učesnika i tipom POTJERNICA")
    void potjernicaUpisujeBinarniFajlSaObaUcesnika() throws Exception {
        SpisakPotjeraUtil.resetujKes();
        String imoTrazeno = "TRAZENI-K4";
        File spisak = napisiSpisak(imoTrazeno + "\n");

        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);

        t.rezervisiSlobodanDok(TestFactory.kontejnerski("FILLER-K4"));

        Plovilo trazenoPlovilo = TestFactory.kontejnerski(imoTrazeno);
        BrodThread trazenaNit = new BrodThread(trazenoPlovilo, luka);
        TankerObalskaStraza obalskoPlovilo = obalskaStraza("K4", spisak);
        BrodThread obalskaNit = new BrodThread(obalskoPlovilo, luka);

        ExecutorService exec = Executors.newFixedThreadPool(2);
        try {
            Future<?> trazenaFuture = exec.submit(trazenaNit);
            cekajUslov(trazenaNit::isPrivezan, 15_000);
            assertTrue(trazenaNit.isPrivezan(), "Traženo plovilo se nije privezalo na vrijeme.");

            Future<?> obalskaFuture = exec.submit(obalskaNit);

            cekajUslov(() -> obalskaFuture.isDone() && trazenaFuture.isDone(), 30_000);
            assertTrue(obalskaFuture.isDone());
            assertTrue(trazenaFuture.isDone());
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }

        File[] fajlovi = fajloviIncidenta();
        assertNotNull(fajlovi);
        assertEquals(1, fajlovi.length, "Tačno jedan fajl incidenta mora biti napisan za potjernicu.");

        Incident ucitan = Incident.ucitaj(fajlovi[0]);
        assertNotNull(ucitan);
        assertEquals(TipIncidenta.POTJERNICA, ucitan.getTip());
        assertEquals(List.of(trazenoPlovilo), ucitan.getUcesniciSudara());
        assertEquals(List.of(obalskoPlovilo), ucitan.getOdazvanaSluzbenaPlovila());
        assertEquals(t.getIdTerminala(), ucitan.getIdTerminala());
    }
}
