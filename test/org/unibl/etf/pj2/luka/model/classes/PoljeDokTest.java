package org.unibl.etf.pj2.luka.model.classes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testovi za {@link Polje} i {@link Dok} — najsitnije gradivne jedinice matrice terminala.
 */
@DisplayName("Polje i Dok")
class PoljeDokTest {

    @Test
    @DisplayName("Polje pamti svoje koordinate i oznaku")
    void poljePamtiKoordinate() {
        Polje p = new Polje(2, 7, "D", null);

        assertEquals(2, p.getX());
        assertEquals(7, p.getY());
        assertEquals("D", p.getOznaka());
        assertNull(p.getTrenutnoPlovilo());
    }

    @Test
    @DisplayName("Polje je prazno kada nema plovila, zauzeto kada ga ima")
    void poljeMijenjaZauzetost() {
        Polje p = new Polje(0, 3, "D", null);
        KontejnerskiBrod b = TestFactory.kontejnerski("1234567");

        assertNull(p.getTrenutnoPlovilo());

        p.setTrenutnoPlovilo(b);
        assertSame(b, p.getTrenutnoPlovilo());

        p.setTrenutnoPlovilo(null);
        assertNull(p.getTrenutnoPlovilo());
    }

    @Test
    @DisplayName("toString polja vraća njegovu oznaku")
    void poljeToString() {
        assertEquals("D", new Polje(0, 3, "D", null).toString());
        assertEquals("^", new Polje(1, 1, "^", null).toString());
    }

    @Test
    @DisplayName("Dok je slobodan dok mu je lokacija prazna")
    void dokJeSlobodanKadNemaPlovila() {
        Polje lokacija = new Polje(3, 5, "D", null);
        Dok dok = new Dok(lokacija, 12);

        assertTrue(dok.isSlobodan());
        assertEquals(12, dok.getOznakaVezova());
        assertSame(lokacija, dok.getLokacija());
    }

    @Test
    @DisplayName("Dok postaje zauzet čim se plovilo postavi na njegovu lokaciju")
    void dokPostajeZauzet() {
        Polje lokacija = new Polje(3, 5, "D", null);
        Dok dok = new Dok(lokacija, 12);

        lokacija.setTrenutnoPlovilo(TestFactory.tanker("7654321"));
        assertFalse(dok.isSlobodan(), "Dok mora biti zauzet kada je njegova lokacija zauzeta.");

        lokacija.setTrenutnoPlovilo(null);
        assertTrue(dok.isSlobodan(), "Dok mora biti slobodan kada se plovilo ukloni.");
    }

    @Test
    @DisplayName("Dok i njegova lokacija dijele isti objekat (bez kopiranja stanja)")
    void dokIPoljeDijeleIstiObjekat() {
        // Ovo je ključna invarijanta: ako Dok ikad počne da drži kopiju Polja,
        // zauzetost doka i zauzetost matrice će se razići i simulacija će "izgubiti" brodove.
        Terminal t = new Terminal(0);
        Dok prvi = t.getDokovi().get(0);
        Polje izMatrice = t.getMatrica()[prvi.getLokacija().getX()][prvi.getLokacija().getY()];

        assertSame(izMatrice, prvi.getLokacija(),
                "Dok mora referencirati identičan objekat Polja kao i matrica terminala.");
    }
}
