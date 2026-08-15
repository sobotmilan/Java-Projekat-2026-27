package org.unibl.etf.pj2.luka.gui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KlijentskiProzor — pokretanje simulacije, živi prikaz, dodavanje, kraj (C5/C7/C8/C9/E1/E2)")
class KlijentskiProzorTest {

    private static final Path SER = Path.of("luka.ser");
    private static final Path BACKUP = Path.of("luka.ser.klijenttestbackup");
    private boolean postojaoPrijeTesta;

    private TestKlijentskiProzor prozor;

    // JOptionPane.showMessageDialog() blokira pumpajući ugniježđenu petlju događaja dok se dijalog
    // ne zatvori — bezbjedno u pravoj upotrebi (korisnik klikne "OK" na EDT-u), ali poziv direktno
    // sa test niti (bez EDT-a, niko da klikne) blokira ZAUVIJEK. Ovaj testni podrazred hvata poruke
    // umjesto da ih zaista prikazuje.
    private static final class TestKlijentskiProzor extends KlijentskiProzor {
        final List<String> poruke = new ArrayList<>();

        TestKlijentskiProzor(Luka luka) {
            super(luka);
        }

        @Override
        void prikaziPoruku(String poruka, String naslov, int tip) {
            poruke.add(poruka);
        }
    }

    @BeforeEach
    void sacuvajSer() throws Exception {
        postojaoPrijeTesta = Files.exists(SER);
        if (postojaoPrijeTesta) {
            Files.move(SER, BACKUP, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.deleteIfExists(SER);
    }

    @AfterEach
    void vratiSer() throws Exception {
        if (prozor != null) {
            prozor.dispose();
        }
        Files.deleteIfExists(SER);
        if (postojaoPrijeTesta) {
            Files.move(BACKUP, SER, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void cekaj(BooleanSupplier uslov, long timeoutMs) throws InterruptedException {
        long krajnjeVrijeme = System.currentTimeMillis() + timeoutMs;
        while (!uslov.getAsBoolean() && System.currentTimeMillis() < krajnjeVrijeme) {
            Thread.sleep(50);
        }
    }

    // ------------------------------------------------------------------
    // Korak 1 — validacija i pokretanje
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Neispravan minimum ne pokreće simulaciju, dugme ostaje omogućeno")
    void neispravanMinimumNePokrecuSimulaciju() {
        Luka pocetna = TestFactory.luka(1);
        prozor = new TestKlijentskiProzor(pocetna);

        prozor.postaviMinimumZaTest("0");
        prozor.pokreniSimulacijuZaTest();

        assertSame(pocetna, prozor.getLukaZaTest(), "Luka se ne smije mijenjati na neuspjeloj validaciji.");
        assertFalse(prozor.jeDodajDugmeOmoguceno());
        assertFalse(prozor.poruke.isEmpty(), "Korisnik mora biti obaviješten o neispravnom unosu.");
    }

    @Test
    @DisplayName("Ispravan minimum priprema stanje, pokreće niti, i omogućava dodavanje")
    void ispravanMinimumPokrecuSimulaciju() throws InterruptedException {
        Luka pocetna = TestFactory.luka(1);
        prozor = new TestKlijentskiProzor(pocetna);

        prozor.postaviMinimumZaTest("1");
        prozor.pokreniSimulacijuZaTest();

        cekaj(() -> prozor.getLukaZaTest() != pocetna, 10_000);
        assertNotSame(pocetna, prozor.getLukaZaTest(),
                "PokretacSimulacije.pripremiPocetnoStanje() mora zamijeniti luku novom pripremljenom instancom.");

        Luka nova = prozor.getLukaZaTest();
        cekaj(() -> !nova.getAktivnaPlovila().isEmpty(), 10_000);
        assertFalse(nova.getAktivnaPlovila().isEmpty(), "Niti moraju biti pokrenute za privezana plovila.");

        assertTrue(prozor.jeDodajDugmeOmoguceno(), "Dodavanje mora biti omogućeno nakon pokretanja.");
    }

    // ------------------------------------------------------------------
    // Korak 2 — živi prikaz (C5)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Promjena odabranog terminala mijenja prikazani tekst")
    void promjenaTerminalaMijenjaPrikaz() {
        Luka pocetna = TestFactory.luka(2);
        prozor = new TestKlijentskiProzor(pocetna);

        String prikaz0 = prozor.getPrikazPoljeZaTest().getText();

        Terminal t1 = pocetna.getTerminali().get(1);
        pocetna.getTerminali().get(1).getDokovi().get(0).getLokacija()
                .setTrenutnoPlovilo(TestFactory.kontejnerski("SWITCH-1"));
        prozor.postaviOdabraniTerminalZaTest(t1);

        String prikaz1 = prozor.getPrikazPoljeZaTest().getText();
        assertNotEquals(prikaz0, prikaz1, "Prikaz se mora promijeniti nakon promjene odabranog terminala.");
    }

    // ------------------------------------------------------------------
    // Korak 3 — odlazak 15% (C7), ožičenje
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Nakon pokretanja, bar jedno plovilo po terminalu je označeno za odlazak")
    void oznaceniZaOdlazakNakonPokretanja() throws InterruptedException {
        Luka pocetna = TestFactory.luka(1);
        prozor = new TestKlijentskiProzor(pocetna);

        prozor.postaviMinimumZaTest("3");
        prozor.pokreniSimulacijuZaTest();

        cekaj(() -> !prozor.getImoZaOdlazakZaTest().isEmpty(), 10_000);
        assertFalse(prozor.getImoZaOdlazakZaTest().isEmpty());
    }

    // ------------------------------------------------------------------
    // Korak 4 — dodavanje tokom simulacije (C8/C9)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Dodavanje tokom simulacije registruje IMO i na kraju privezuje novo plovilo")
    void dodavanjeTokomSimulacijeRadi() throws InterruptedException {
        Luka pocetna = TestFactory.luka(1);
        prozor = new TestKlijentskiProzor(pocetna);

        prozor.postaviMinimumZaTest("1");
        prozor.pokreniSimulacijuZaTest();
        cekaj(() -> prozor.getLukaZaTest() != pocetna, 10_000);

        Luka luka = prozor.getLukaZaTest();
        var kandidat = TestFactory.kontejnerski("DODATO-KP-1");
        List<String> greske = KlijentskaSimulacijaService.dodajTokomSimulacije(luka, kandidat);
        assertTrue(greske.isEmpty());
        prozor.getImoDodataTokomSimulacijeZaTest().add("DODATO-KP-1");

        cekaj(() -> {
            var bt = KlijentskaSimulacijaService.pronadjiAktivnuNit(luka, "DODATO-KP-1");
            return bt != null && bt.isPrivezan();
        }, 10_000);

        var bt = KlijentskaSimulacijaService.pronadjiAktivnuNit(luka, "DODATO-KP-1");
        assertNotNull(bt);
        assertTrue(bt.isPrivezan());
    }

    // ------------------------------------------------------------------
    // Korak 5 — kraj simulacije i serijalizacija (E1/E2)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Kad sva označena plovila napuste luku, simulacija se završava i luka.ser postoji")
    void krajSimulacijeSerijalizuje() throws InterruptedException {
        Luka pocetna = TestFactory.luka(1);
        prozor = new TestKlijentskiProzor(pocetna);

        // Minimum 1 po terminalu -> tačno jedno plovilo po terminalu, ceil(0.15*1)=1 -> svako se
        // odmah označava za odlazak, pa test brzo stigne do kraja bez obzira na stvaran broj
        // terminala iz luka.properties.
        prozor.postaviMinimumZaTest("1");
        prozor.pokreniSimulacijuZaTest();

        cekaj(() -> prozor.getLukaZaTest() != pocetna, 10_000);
        cekaj(() -> !prozor.getImoZaOdlazakZaTest().isEmpty(), 10_000);
        assertFalse(prozor.getImoZaOdlazakZaTest().isEmpty());

        // tikZaTest() poziva istu logiku koju Timer poziva svakih INTERVAL_RENDEROVANJA_MS —
        // pozivamo je direktno u petlji umjesto da čekamo stvaran Timer, radi determinizma testa.
        cekaj(() -> {
            prozor.tikZaTest();
            return prozor.jeSimulacijaZavrsenaZaTest();
        }, 30_000);

        assertTrue(prozor.jeSimulacijaZavrsenaZaTest(), "Simulacija se mora završiti kad sva označena plovila odu.");

        cekaj(() -> Files.exists(SER), 10_000);
        assertTrue(Files.exists(SER), "luka.ser mora postojati nakon završetka simulacije.");
        cekaj(() -> !prozor.poruke.isEmpty(), 5_000);
        assertFalse(prozor.poruke.isEmpty(), "Korisnik mora biti obaviješten o kraju simulacije.");
    }
}
