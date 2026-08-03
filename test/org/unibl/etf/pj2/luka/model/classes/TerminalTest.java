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
    @Tag("bug")
    @DisplayName("BUG: terminal treba da izloži atomarnu rezervaciju doka")
    void terminalTrebaAtomarnuRezervaciju() {
        // Bez ove metode svaki BrodThread sam radi "pronađi slobodan dok" pa "zauzmi ga"
        // u dva odvojena koraka, što je klasična race condition situacija:
        // dva broda mogu pronaći isti slobodan dok prije nego ijedan stigne da ga zauzme.
        //
        // Očekivani potpis: public synchronized Dok rezervisiSlobodanDok(Plovilo p)
        //
        // Ovaj test namjerno pada dok metoda ne postoji — vidi refaktor R2 u PRONALASCI.md.
        fail("Nedostaje Terminal.rezervisiSlobodanDok(Plovilo) — atomarna rezervacija veza (refaktor R2).");
    }
}
