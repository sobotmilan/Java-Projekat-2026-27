package org.unibl.etf.pj2.luka.simulation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.model.classes.Dok;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BrodThread — buđenje privezanih patrola i prelasci ka incidentu (Korak 4/5)")
class BrodThreadIncidentTest {

    private long staroMinTrajanje;
    private long staroMaxTrajanje;

    @BeforeEach
    void spustiTrajanjaUvidjaja() {
        staroMinTrajanje = BrodThread.MIN_TRAJANJE_UVIDJAJA_MS;
        staroMaxTrajanje = BrodThread.MAX_TRAJANJE_UVIDJAJA_MS;
        BrodThread.MIN_TRAJANJE_UVIDJAJA_MS = 50L;
        BrodThread.MAX_TRAJANJE_UVIDJAJA_MS = 50L;
    }

    @AfterEach
    void vratiTrajanjaUvidjaja() {
        BrodThread.MIN_TRAJANJE_UVIDJAJA_MS = staroMinTrajanje;
        BrodThread.MAX_TRAJANJE_UVIDJAJA_MS = staroMaxTrajanje;
    }

    private static void cekajUslov(BooleanSupplier uslov, long timeoutMs) throws InterruptedException {
        long krajnjeVrijeme = System.currentTimeMillis() + timeoutMs;
        while (!uslov.getAsBoolean() && System.currentTimeMillis() < krajnjeVrijeme) {
            Thread.sleep(20);
        }
    }

    @Test
    @DisplayName("Privezana patrola se budi preko pozoviNaIncident() i mijenja zadatak KA_INCIDENTU → NA_INCIDENTU")
    void privezanaPatrolaSeBudiIMijenjaZadatak() throws Exception {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        BrodThread bt = new BrodThread(TestFactory.tankerVatrogasci("PATROLA-BUDI"), luka);

        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> future = exec.submit(bt);
        try {
            cekajUslov(() -> bt.getZadatak() == Zadatak.PRIVEZAN, 10_000);
            assertTrue(bt.isPrivezan());
            assertEquals(Zadatak.PRIVEZAN, bt.getZadatak());

            bt.pozoviNaIncident(t, Terminal.KANAL_ULAZ, 5);
            assertEquals(Zadatak.KA_INCIDENTU, bt.getZadatak(),
                    "Zadatak se mora promijeniti sinhrono, u samom pozivu pozoviNaIncident().");

            cekajUslov(() -> bt.getZadatak() == Zadatak.NA_INCIDENTU, 10_000);
            assertEquals(Zadatak.NA_INCIDENTU, bt.getZadatak(),
                    "Probuđena patrola mora naposljetku stići do mjesta incidenta.");
            assertFalse(future.isDone(), "Nit ostaje živa i čeka kraj uviđaja, ne završava se.");
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Vez koji patrola napušta radi incidenta postaje ponovo raspoloživ (rezervacija se oslobađa)")
    void vezKojiPatrolaNapustaPostajePonovoRaspoloziv() throws Exception {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        BrodThread bt = new BrodThread(TestFactory.tankerVatrogasci("PATROLA-VEZ"), luka);

        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> future = exec.submit(bt);
        try {
            cekajUslov(() -> bt.getZadatak() == Zadatak.PRIVEZAN, 10_000);
            assertEquals(29, t.getBrojSlobodnihVezova());
            assertEquals(29, t.getBrojRaspolozivihVezova());

            bt.pozoviNaIncident(t, Terminal.KANAL_ULAZ, 5);

            cekajUslov(() -> t.getBrojSlobodnihVezova() == 30, 10_000);
            assertEquals(30, t.getBrojSlobodnihVezova(), "Patrola je trebalo da napusti vez.");
            assertEquals(30, t.getBrojRaspolozivihVezova(),
                    "Rezervacija veza mora biti oslobođena — inače vez ostaje trajno "
                            + "\"rezervisan\" a prazan, iako je fizički slobodan.");
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Patrola koja je već u pokretu (na incidentu) ne biva ponovo probuđena ili preusmjerena")
    void patrolaKojaJeVecUPokretuNeBivaProbudjenaDvaput() throws Exception {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        BrodThread bt = new BrodThread(TestFactory.tankerVatrogasci("PATROLA-DVAPUT"), luka);

        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> future = exec.submit(bt);
        try {
            cekajUslov(() -> bt.getZadatak() == Zadatak.PRIVEZAN, 10_000);

            bt.pozoviNaIncident(t, Terminal.KANAL_ULAZ, 5);
            cekajUslov(() -> bt.getZadatak() == Zadatak.NA_INCIDENTU, 10_000);
            assertEquals(Zadatak.NA_INCIDENTU, bt.getZadatak());

            // Drugi poziv, sa drugačijim koordinatama, dok patrola VIŠE NIJE PRIVEZAN.
            assertDoesNotThrow(() -> bt.pozoviNaIncident(t, Terminal.KANAL_IZLAZ, 12));

            assertEquals(Zadatak.NA_INCIDENTU, bt.getZadatak(),
                    "Drugi poziv mora biti bez efekta — patrola nije PRIVEZAN, pa se ne budi ponovo.");
            assertFalse(future.isDone(), "Nit ne smije puknuti niti se prekinuto ponašati usljed drugog poziva.");
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("pozoviNaIncident() nema efekta na plovilo koje još nije PRIVEZAN (npr. u tranzitu ka svom doku)")
    void pozoviNaIncidentNemaEfektaAkoNijePrivezan() {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        BrodThread bt = new BrodThread(TestFactory.tankerVatrogasci("PATROLA-NIJE-PRIVEZAN"), luka);

        assertEquals(Zadatak.KA_DOKU, bt.getZadatak());
        bt.pozoviNaIncident(t, Terminal.KANAL_ULAZ, 5);

        assertEquals(Zadatak.KA_DOKU, bt.getZadatak(),
                "Plovilo koje nije privezano (nit nije ni pokrenuta) se ne preusmjerava.");
    }

    // ------------------------------------------------------------------
    // Korak 5 — raspetljavanje nakon uviđaja
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Korak 5: zavrsiUvidjaj(dok) vraća patrolu na zadati dok i u PRIVEZAN")
    void zavrsiUvidjajVracaPatroluNaZadatiDokIPrivezan() throws Exception {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        BrodThread bt = new BrodThread(TestFactory.tankerVatrogasci("PATROLA-ZAVRSI"), luka);

        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> future = exec.submit(bt);
        try {
            cekajUslov(() -> bt.getZadatak() == Zadatak.PRIVEZAN, 10_000);
            bt.pozoviNaIncident(t, Terminal.KANAL_ULAZ, 5);
            cekajUslov(() -> bt.getZadatak() == Zadatak.NA_INCIDENTU, 10_000);

            Dok noviDok = null;
            for (Dok d : t.getDokovi()) {
                if (d.isSlobodan()) {
                    noviDok = d;
                    break;
                }
            }
            assertNotNull(noviDok, "Terminal mora imati bar jedan slobodan dok za ovaj test.");

            bt.zavrsiUvidjaj(noviDok);
            cekajUslov(() -> bt.getZadatak() == Zadatak.PRIVEZAN, 10_000);

            assertTrue(bt.isPrivezan());
            assertEquals(noviDok.getLokacija().getX(), bt.getX());
            assertEquals(noviDok.getLokacija().getY(), bt.getY());
            assertFalse(future.isDone());
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Korak 5: patrola na incidentu odustaje i napušta terminal ako niko ne signalizira kraj uviđaja "
            + "(vremensko ograničenje, isti razlog kao KoordinatorUvidjaja.MAX_CEKANJE_DOLASKA_MS)")
    void patrolaOdustajeAkoNikoNeSignaliziraKrajUvidjaja() throws Exception {
        // BrodThread.maxCekanjeKrajaUvidjaja() se računa iz KoordinatorUvidjaja.MAX_CEKANJE_DOLASKA_MS
        // + BrodThread.MAX_TRAJANJE_UVIDJAJA_MS + 5000L (G1) — nema više sopstvenog hardkodovanog
        // polja, pa se ovdje spuštaju ta dva ulazna sastojka na 0 da bi ukupni budžet bio minimalan (5s).
        long staroCekanjeDolaska = KoordinatorUvidjaja.MAX_CEKANJE_DOLASKA_MS;
        long staroTrajanjeMax = BrodThread.MAX_TRAJANJE_UVIDJAJA_MS;
        KoordinatorUvidjaja.MAX_CEKANJE_DOLASKA_MS = 0L;
        BrodThread.MAX_TRAJANJE_UVIDJAJA_MS = 0L;
        try {
            Luka luka = TestFactory.luka(1);
            Terminal t = luka.getTerminali().get(0);
            BrodThread bt = new BrodThread(TestFactory.tankerVatrogasci("PATROLA-NAPUSTENA"), luka);

            ExecutorService exec = Executors.newSingleThreadExecutor();
            Future<?> future = exec.submit(bt);
            try {
                cekajUslov(() -> bt.getZadatak() == Zadatak.PRIVEZAN, 10_000);
                bt.pozoviNaIncident(t, Terminal.KANAL_ULAZ, 5);
                cekajUslov(() -> bt.getZadatak() == Zadatak.NA_INCIDENTU, 10_000);

                // Niko ne poziva zavrsiUvidjaj() — patrola mora sama odustati nakon budžeta
                // (minimalno 5000ms, vidi maxCekanjeKrajaUvidjaja()).
                cekajUslov(future::isDone, 15_000);
                assertTrue(future.isDone(),
                        "Patrola se ne smije zaglaviti zauvijek bez signala kraja uviđaja.");
                assertEquals(Zadatak.NAPUSTA, bt.getZadatak(),
                        "Bez signala, patrola mora sama pasti na podrazumijevano — napustiti terminal.");
                assertFalse(luka.getAktivnaPlovila().contains(bt),
                        "Nit koja je napustila terminal ne smije ostati u registru aktivnih plovila.");
            } finally {
                exec.shutdownNow();
                exec.awaitTermination(5, TimeUnit.SECONDS);
            }
        } finally {
            KoordinatorUvidjaja.MAX_CEKANJE_DOLASKA_MS = staroCekanjeDolaska;
            BrodThread.MAX_TRAJANJE_UVIDJAJA_MS = staroTrajanjeMax;
        }
    }

    @Test
    @DisplayName("G1: maxCekanjeKrajaUvidjaja() je uvijek veći od zbira koordinatorovih budžeta "
            + "(dolazak patrole + trajanje uviđaja), i računa se iznova pri svakoj promjeni")
    void maxCekanjeKrajaUvidjajaJeUvijekVeciOdKoordinatorovihBudzeta() {
        long staroCekanjeDolaska = KoordinatorUvidjaja.MAX_CEKANJE_DOLASKA_MS;
        long staroTrajanjeMax = BrodThread.MAX_TRAJANJE_UVIDJAJA_MS;
        try {
            assertTrue(BrodThread.maxCekanjeKrajaUvidjaja()
                            > KoordinatorUvidjaja.MAX_CEKANJE_DOLASKA_MS + BrodThread.MAX_TRAJANJE_UVIDJAJA_MS,
                    "Patrola mora čekati duže nego što koordinatoru maksimalno treba da dođe do "
                            + "raspetljavanja — inače istekne prije njega i rezervacija veza (G1) ostaje "
                            + "trajno zauzeta.");

            KoordinatorUvidjaja.MAX_CEKANJE_DOLASKA_MS = 1000L;
            BrodThread.MAX_TRAJANJE_UVIDJAJA_MS = 2000L;
            assertEquals(1000L + 2000L + 5000L, BrodThread.maxCekanjeKrajaUvidjaja(),
                    "Vrijednost se mora računati iznova pri svakom pozivu (isti obrazac kao "
                            + "maxBlokadaPokusaja()), ne keširati.");
        } finally {
            KoordinatorUvidjaja.MAX_CEKANJE_DOLASKA_MS = staroCekanjeDolaska;
            BrodThread.MAX_TRAJANJE_UVIDJAJA_MS = staroTrajanjeMax;
        }
    }
}
