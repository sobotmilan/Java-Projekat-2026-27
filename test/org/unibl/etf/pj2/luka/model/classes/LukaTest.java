package org.unibl.etf.pj2.luka.model.classes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testovi agregatne klase {@link Luka}.
 */
@DisplayName("Luka — terminali i evidencija ulaska")
class LukaTest {

    @Test
    @DisplayName("Luka se kreira sa zadatim brojem terminala")
    void kreiranjeSaTerminalima() {
        Luka luka = TestFactory.luka(3);
        assertEquals(3, luka.getTerminali().size());

        for (int i = 0; i < 3; i++) {
            assertEquals(i, luka.getTerminali().get(i).getIdTerminala());
        }
    }

    @Test
    @DisplayName("Dodavanje i uklanjanje terminala mijenja listu")
    void dodavanjeIUklanjanjeTerminala() {
        Luka luka = TestFactory.luka(2);
        Terminal novi = new Terminal(2);

        assertTrue(luka.addTerminal(novi));
        assertEquals(3, luka.getTerminali().size());

        assertTrue(luka.removeTerminal(novi));
        assertEquals(2, luka.getTerminali().size());
    }

    @Test
    @DisplayName("Evidencija ulaska pamti IMO broj i vrijeme")
    void evidencijaUlaska() {
        Luka luka = TestFactory.luka(1);
        LocalDateTime vrijeme = LocalDateTime.of(2026, 8, 3, 10, 30);

        luka.addToEvidencija("9876543", vrijeme);

        assertTrue(luka.getEvidencijaUlaska().containsKey("9876543"));
        assertEquals(vrijeme, luka.getEvidencijaUlaska().get("9876543"));
    }

    @Test
    @DisplayName("Ukupan broj slobodnih vezova je zbir po terminalima")
    void ukupnoSlobodnihVezova() {
        Luka luka = TestFactory.luka(3);

        int ukupno = 0;
        for (Terminal t : luka.getTerminali()) {
            ukupno += t.getBrojSlobodnihVezova();
        }
        assertEquals(90, ukupno, "3 terminala x 30 vezova = 90.");

        TestFactory.popuniSveDokove(luka.getTerminali().get(0));

        ukupno = 0;
        for (Terminal t : luka.getTerminali()) {
            ukupno += t.getBrojSlobodnihVezova();
        }
        assertEquals(60, ukupno);
    }

    @Test
    @DisplayName("Registar aktivnih plovila je inicijalno prazan, ali nikad null")
    void aktivnaPlovilaJeInicijalnoPrazna() {
        Luka luka = TestFactory.luka(1);
        assertNotNull(luka.getAktivnaPlovila());
        assertTrue(luka.getAktivnaPlovila().isEmpty());
    }

    @Test
    @DisplayName("Luka je puna kada nijedan terminal nema slobodan vez")
    void punaLuka() {
        Luka luka = TestFactory.luka(2);
        for (Terminal t : luka.getTerminali()) {
            TestFactory.popuniSveDokove(t);
        }

        for (Terminal t : luka.getTerminali()) {
            assertEquals(0, t.getBrojSlobodnihVezova());
        }
    }

    // ------------------------------------------------------------------
    // BUCKET B
    // ------------------------------------------------------------------

    @Test
    @Tag("bug")
    @DisplayName("BUG: evidencija ulaska nije bezbjedna za konkurentan pristup")
    void evidencijaJeThreadSafe() throws InterruptedException {
        // Luka.addToEvidencija() nije sinhronizovana, a mapa je običan HashMap.
        // BrodThread sinhronizuje ručno na getEvidencijaUlaska(), ali svaki drugi
        // pozivalac (GUI, generator plovila) to lako zaboravi.
        final Luka luka = new Luka(new ArrayList<Terminal>(), new HashMap<String, LocalDateTime>());

        final int brojNiti = 50;
        final int poNiti = 200;
        ExecutorService exec = Executors.newFixedThreadPool(16);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch kraj = new CountDownLatch(brojNiti);

        for (int i = 0; i < brojNiti; i++) {
            final int id = i;
            exec.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        start.await();
                        for (int j = 0; j < poNiti; j++) {
                            luka.addToEvidencija("IMO-" + id + "-" + j, LocalDateTime.now());
                        }
                    } catch (Exception e) {
                        // Namjerno se guta — cilj testa je konačan broj upisa.
                    } finally {
                        kraj.countDown();
                    }
                }
            });
        }

        start.countDown();
        assertTrue(kraj.await(30, TimeUnit.SECONDS), "Niti nisu završile na vrijeme.");
        exec.shutdownNow();

        assertEquals(brojNiti * poNiti, luka.getEvidencijaUlaska().size(),
                "Izgubljeni upisi zbog trke — koristi ConcurrentHashMap ili sinhronizuj addToEvidencija().");
    }

    // ------------------------------------------------------------------
    // F6 — skaliranje vremena: pomjeranje evidencije za pauzu rada aplikacije
    // ------------------------------------------------------------------

    @Test
    @DisplayName("F6: pomjeriEvidencijuZaPauzu pomjera postojeći unos unaprijed")
    void pomjeriEvidencijuZaPauzuPomjeraUnos() {
        Luka luka = TestFactory.luka(1);
        LocalDateTime original = LocalDateTime.of(2026, 8, 3, 9, 0);
        luka.addToEvidencija("1", original);

        luka.pomjeriEvidencijuZaPauzu(Duration.ofHours(3));

        assertEquals(original.plusHours(3), luka.getEvidencijaUlaska().get("1"));
    }

    @Test
    @DisplayName("F6: pomjeriEvidencijuZaPauzu pomjera više unosa istovremeno")
    void pomjeriEvidencijuZaPauzuPomjeraViseUnosa() {
        Luka luka = TestFactory.luka(1);
        luka.addToEvidencija("1", LocalDateTime.of(2026, 8, 3, 9, 0));
        luka.addToEvidencija("2", LocalDateTime.of(2026, 8, 3, 10, 0));

        luka.pomjeriEvidencijuZaPauzu(Duration.ofHours(1));

        assertEquals(LocalDateTime.of(2026, 8, 3, 10, 0), luka.getEvidencijaUlaska().get("1"));
        assertEquals(LocalDateTime.of(2026, 8, 3, 11, 0), luka.getEvidencijaUlaska().get("2"));
    }

    @Test
    @DisplayName("F6: pomjeriEvidencijuZaPauzu ignoriše null, nultu i negativnu pauzu")
    void pomjeriEvidencijuZaPauzuIgnorisePraznuPauzu() {
        Luka luka = TestFactory.luka(1);
        LocalDateTime original = LocalDateTime.of(2026, 8, 3, 9, 0);
        luka.addToEvidencija("1", original);

        luka.pomjeriEvidencijuZaPauzu(null);
        luka.pomjeriEvidencijuZaPauzu(Duration.ZERO);
        luka.pomjeriEvidencijuZaPauzu(Duration.ofMinutes(-5));

        assertEquals(original, luka.getEvidencijaUlaska().get("1"));
    }

    @Test
    @DisplayName("F6: vrijemeZadnjegCuvanja je podrazumijevano null i može se postaviti")
    void vrijemeZadnjegCuvanjaGetterSetter() {
        Luka luka = TestFactory.luka(1);
        assertNull(luka.getVrijemeZadnjegCuvanja());

        LocalDateTime vrijeme = LocalDateTime.of(2026, 8, 3, 12, 0);
        luka.setVrijemeZadnjegCuvanja(vrijeme);
        assertEquals(vrijeme, luka.getVrijemeZadnjegCuvanja());
    }

    @Test
    @Tag("bug")
    @DisplayName("BUG: Luka nema serialVersionUID — luka.ser puca pri svakoj izmjeni klase")
    void lukaImaSerialVersionUID() throws Exception {
        // Bez eksplicitnog serialVersionUID, JVM ga računa iz strukture klase.
        // Čim se doda jedno polje u Luku, stari luka.ser postaje nečitljiv
        // (InvalidClassException), a to je fajl koji nosi cijelo stanje aplikacije.
        java.lang.reflect.Field f = Luka.class.getDeclaredField("serialVersionUID");
        assertTrue(java.lang.reflect.Modifier.isStatic(f.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isFinal(f.getModifiers()));
    }
}
