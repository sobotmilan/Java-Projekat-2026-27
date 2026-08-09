package org.unibl.etf.pj2.luka.simulation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
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
}
