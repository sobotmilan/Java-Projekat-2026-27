package org.unibl.etf.pj2.luka.model.classes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testovi strukture terminala prema šemi iz specifikacije:
 *
 * <pre>
 * red 0:  ↓ ↑ D D D D D D D D D D D D D D D
 * red 1:  ↓ ↑ ←
 * red 2:  ↓ ↑ →
 * red 3:  ↓ ↑ D D D D D D D D D D D D D D D
 * </pre>
 */
@DisplayName("Terminal — struktura i evidencija vezova")
class TerminalTest {

    private Terminal t;

    @BeforeEach
    void setUp() {
        t = new Terminal(0);
    }

    // ------------------------------------------------------------------
    // BUCKET A — geometrija
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Matrica ima dimenzije 4 x 17")
    void dimenzijeMatrice() {
        assertEquals(4, t.getMatrica().length);
        for (int i = 0; i < 4; i++) {
            assertEquals(17, t.getMatrica()[i].length, "Red " + i + " nema 17 kolona.");
        }
    }

    @Test
    @DisplayName("Nijedno polje matrice nije null nakon inicijalizacije")
    void nemaNullPolja() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 17; j++) {
                assertNotNull(t.getMatrica()[i][j], "Polje (" + i + "," + j + ") je null.");
            }
        }
    }

    @Test
    @DisplayName("Svako polje zna svoje stvarne koordinate u matrici")
    void poljaZnajuSvojeKoordinate() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 17; j++) {
                Polje p = t.getMatrica()[i][j];
                assertEquals(i, p.getX(), "Pogrešan x na (" + i + "," + j + ")");
                assertEquals(j, p.getY(), "Pogrešan y na (" + i + "," + j + ")");
            }
        }
    }

    @Test
    @DisplayName("Kolona 0 je ulazni (silazni) kanal, kolona 1 izlazni (uzlazni)")
    void vertikalniKanali() {
        for (int i = 0; i < 4; i++) {
            assertEquals("v", t.getMatrica()[i][0].getOznaka(), "Red " + i + ", kolona 0 nije ulazni kanal.");
            assertEquals("^", t.getMatrica()[i][1].getOznaka(), "Red " + i + ", kolona 1 nije izlazni kanal.");
        }
    }

    @Test
    @DisplayName("Redovi 0 i 3 su dokovi od kolone 2 do 16")
    void redoviDokova() {
        for (int j = 2; j < 17; j++) {
            assertEquals("D", t.getMatrica()[0][j].getOznaka(), "Polje (0," + j + ") nije dok.");
            assertEquals("D", t.getMatrica()[3][j].getOznaka(), "Polje (3," + j + ") nije dok.");
        }
    }

    @Test
    @DisplayName("Terminal ima tačno 30 vezova sa jedinstvenim oznakama")
    void brojIJedinstvenostVezova() {
        assertEquals(30, t.getDokovi().size());

        Set<Integer> oznake = new HashSet<>();
        for (Dok d : t.getDokovi()) {
            assertTrue(oznake.add(d.getOznakaVezova()), "Duplirana oznaka veza: " + d.getOznakaVezova());
        }
        assertEquals(30, oznake.size());
    }

    @Test
    @DisplayName("Horizontalni kanali su označeni: red 1 ulijevo, red 2 udesno")
    void horizontalniKanaliOznaceni() {
        assertEquals("<-", t.getMatrica()[1][2].getOznaka());
        assertEquals("->", t.getMatrica()[2][2].getOznaka());
    }

    @Test
    @DisplayName("Id terminala se čuva")
    void idTerminala() {
        assertEquals(0, new Terminal(0).getIdTerminala());
        assertEquals(7, new Terminal(7).getIdTerminala());
    }

    // ------------------------------------------------------------------
    // BUCKET A — evidencija slobodnih vezova
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Novi terminal ima svih 30 vezova slobodnih")
    void noviTerminalJePrazan() {
        assertEquals(30, t.getBrojSlobodnihVezova());
    }

    @Test
    @DisplayName("Zauzimanje veza smanjuje broj slobodnih")
    void zauzimanjeVezaSmanjujeBroj() {
        t.getDokovi().get(0).getLokacija().setTrenutnoPlovilo(TestFactory.kontejnerski("1"));
        assertEquals(29, t.getBrojSlobodnihVezova());

        t.getDokovi().get(1).getLokacija().setTrenutnoPlovilo(TestFactory.tanker("2"));
        assertEquals(28, t.getBrojSlobodnihVezova());
    }

    @Test
    @DisplayName("Pun terminal ima nula slobodnih vezova")
    void punTerminal() {
        TestFactory.popuniSveDokove(t);
        assertEquals(0, t.getBrojSlobodnihVezova());
        assertNull(TestFactory.prviSlobodanDok(t));
    }

    @Test
    @DisplayName("Oslobađanje veza vraća broj slobodnih")
    void oslobadjanjeVeza() {
        TestFactory.popuniSveDokove(t);
        t.getDokovi().get(5).getLokacija().setTrenutnoPlovilo(null);

        assertEquals(1, t.getBrojSlobodnihVezova());
        assertSame(t.getDokovi().get(5), TestFactory.prviSlobodanDok(t));
    }

    // ------------------------------------------------------------------
    // BUCKET B — struktura koja nedostaje za ispravno kretanje
    // ------------------------------------------------------------------

    @Test
    @Tag("bug")
    @DisplayName("BUG: horizontalni plovni kanal mora postojati cijelom dužinom terminala")
    void horizontalniKanalMoraBitiOznacenCijelomDuzinom() {
        // Prema šemi, redovi 1 i 2 su plovni kanal kojim brodovi idu do dokova.
        // Trenutno su označeni samo na koloni 2, a od kolone 3 do 16 imaju praznu oznaku.
        // Posljedica: BrodThread nema način da razlikuje kanal od nedefinisanog prostora,
        // pa se brodovi kreću redom 3 (kroz dokove) umjesto redom 2.
        for (int j = 2; j < 17; j++) {
            assertEquals("<-", t.getMatrica()[1][j].getOznaka(),
                    "Polje (1," + j + ") mora biti dio odlaznog kanala.");
            assertEquals("->", t.getMatrica()[2][j].getOznaka(),
                    "Polje (2," + j + ") mora biti dio dolaznog kanala.");
        }
    }

    @Test
    @DisplayName("Rezervacija veza je atomarna — dva plovila ne mogu dobiti isti vez")
    void rezervacijaJeAtomarna() throws InterruptedException {
        final int brojNiti = 40;
        final java.util.Set<Integer> dodijeljeni =
                java.util.Collections.synchronizedSet(new java.util.HashSet<Integer>());
        final java.util.concurrent.atomic.AtomicInteger uspjesnih =
                new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger duplikata =
                new java.util.concurrent.atomic.AtomicInteger();

        final java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch kraj = new java.util.concurrent.CountDownLatch(brojNiti);
        java.util.concurrent.ExecutorService exec =
                java.util.concurrent.Executors.newFixedThreadPool(16);

        for (int i = 0; i < brojNiti; i++) {
            final Plovilo p = TestFactory.kontejnerski("IMO-" + i);
            exec.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        start.await();
                        Dok d = t.rezervisiSlobodanDok(p);
                        if (d != null) {
                            uspjesnih.incrementAndGet();
                            if (!dodijeljeni.add(d.getOznakaVezova())) {
                                duplikata.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    } finally {
                        kraj.countDown();
                    }
                }
            });
        }

        start.countDown();
        assertTrue(kraj.await(15, java.util.concurrent.TimeUnit.SECONDS));
        exec.shutdownNow();

        assertEquals(0, duplikata.get(), "Isti vez je dodijeljen dvama plovilima.");
        assertEquals(30, uspjesnih.get(), "Svih 30 vezova je trebalo biti dodijeljeno.");
        assertEquals(0, t.getBrojRaspolozivihVezova());
    }

    @Test
    @DisplayName("Otkazana rezervacija vraća vez u opticaj")
    void otkazivanjeRezervacije() {
        Dok d = t.rezervisiSlobodanDok(TestFactory.kontejnerski("1"));

        assertNotNull(d);
        assertEquals(29, t.getBrojRaspolozivihVezova());
        assertEquals(30, t.getBrojSlobodnihVezova(), "Rezervacija ne zauzima vez fizički.");

        t.otkaziRezervaciju(d);
        assertEquals(30, t.getBrojRaspolozivihVezova());
    }

    // ------------------------------------------------------------------
    // BUCKET C — blokada saobraćaja (I3/I4, priprema za R4)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Novi terminal nije blokiran, i sva plovila smiju proći")
    void noviTerminalNijeBlokiran() {
        assertFalse(t.isSaobracajBlokiran());
        assertTrue(t.smijeProci(TestFactory.kontejnerski("1")));
        assertTrue(t.smijeProci(TestFactory.tankerVatrogasci("2")));
    }

    @Test
    @DisplayName("blokirajSaobracaj() postavlja zastavicu i zaustavlja obično plovilo")
    void blokirajSaobracajZaustavljaObicnoPlovilo() {
        t.blokirajSaobracaj();

        assertTrue(t.isSaobracajBlokiran());
        assertFalse(t.smijeProci(TestFactory.kontejnerski("1")),
                "Obično plovilo ne smije proći dok je terminal blokiran.");
    }

    @Test
    @DisplayName("Blokada propušta samo službeno plovilo pod aktivnom rotacijom")
    void blokadaPropustaSamoPloviloPodRotacijom() {
        t.blokirajSaobracaj();

        TankerVatrogasci podRotacijom = TestFactory.tankerVatrogasci("HITNO-1");
        podRotacijom.setRotacija(true);
        TankerVatrogasci bezRotacije = TestFactory.tankerVatrogasci("HITNO-2");

        assertTrue(t.smijeProci(podRotacijom),
                "Službeno plovilo pod aktivnom rotacijom mora proći i kroz blokiran terminal.");
        assertFalse(t.smijeProci(bezRotacije),
                "Službeno plovilo bez uključene rotacije se ne razlikuje od običnog — ne smije proći.");
    }

    @Test
    @DisplayName("odblokirajSaobracaj() vraća terminal u normalno stanje")
    void odblokirajSaobracajVracaNormalnoStanje() {
        t.blokirajSaobracaj();
        assertTrue(t.isSaobracajBlokiran());

        t.odblokirajSaobracaj();

        assertFalse(t.isSaobracajBlokiran());
        assertTrue(t.smijeProci(TestFactory.kontejnerski("1")));
    }

}
