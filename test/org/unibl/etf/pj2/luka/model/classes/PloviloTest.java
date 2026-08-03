package org.unibl.etf.pj2.luka.model.classes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testovi osnovnih atributa i ponašanja apstraktne klase {@link Plovilo}.
 * Testira se preko konkretne nasljednice jer se apstraktna klasa ne može instancirati.
 */
@DisplayName("Plovilo — osnovni atributi")
class PloviloTest {

    @Test
    @DisplayName("Konstruktor postavlja sve prosleđene atribute")
    void konstruktorPostavljaAtribute() {
        KontejnerskiBrod b = TestFactory.kontejnerski("9876543");

        assertEquals("Kont-9876543", b.getNaziv());
        assertEquals("9876543", b.getImoBroj());
        assertEquals("M-9876543", b.getBrojMotora());
        assertEquals("REG-9876543", b.getRegistarskiBroj());
        assertEquals(TestFactory.FOTO, b.getFotografija());
        assertEquals(1500, b.getKapacitetTEU());
    }

    @Test
    @DisplayName("Brzina se generiše slučajno u opsegu [1, 50)")
    void brzinaJeUOcekivanomOpsegu() {
        for (int i = 0; i < 200; i++) {
            double brzina = TestFactory.kontejnerski("IMO" + i).getBrzina();
            assertTrue(brzina >= 1.0, "Brzina ispod donje granice: " + brzina);
            assertTrue(brzina < 50.0, "Brzina iznad gornje granice: " + brzina);
        }
    }

    @Test
    @DisplayName("Brzina je različita za različita plovila (jedinstvenost iz specifikacije)")
    void brzinaJeUglavnomJedinstvena() {
        Set<Double> brzine = new HashSet<>();
        int n = 300;
        for (int i = 0; i < n; i++) {
            brzine.add(TestFactory.tanker("IMO" + i).getBrzina());
        }
        // Kod double vrijednosti iz kontinuiranog opsega kolizije su praktično nemoguće.
        assertEquals(n, brzine.size(), "Detektovane duplirane brzine — generator brzine nije ispravan.");
    }

    @Test
    @DisplayName("Brzina nikada nije nula (dijeljenje nulom u BrodThread-u)")
    void brzinaNijeNula() {
        // BrodThread računa sleep kao (long)(1000 / brzina) — nula bi značila ArithmeticException/Infinity.
        for (int i = 0; i < 500; i++) {
            assertNotEquals(0.0, TestFactory.kruzer("IMO" + i).getBrzina());
        }
    }

    @Test
    @DisplayName("Setteri mijenjaju vrijednosti atributa")
    void setteriRade() {
        Tanker t = TestFactory.tanker("1111111");

        t.setNaziv("Novi Naziv");
        t.setImoBroj("2222222");
        t.setBrojMotora("NOVI-MOTOR");
        t.setRegistarskiBroj("NOVA-REG");
        t.setBrzina(12.5);
        t.setZapreminaBarel(999.0);

        assertEquals("Novi Naziv", t.getNaziv());
        assertEquals("2222222", t.getImoBroj());
        assertEquals("NOVI-MOTOR", t.getBrojMotora());
        assertEquals("NOVA-REG", t.getRegistarskiBroj());
        assertEquals(12.5, t.getBrzina(), 0.0001);
        assertEquals(999.0, t.getZapreminaBarel(), 0.0001);
    }

    @Test
    @DisplayName("toString ima format [IMO] Naziv")
    void toStringFormat() {
        KontejnerskiBrod b = TestFactory.kontejnerski("5555555");
        assertEquals("[5555555] Kont-5555555", b.toString());
    }

    // ------------------------------------------------------------------
    // BUCKET B — test koji pada i ukazuje na stvarni problem u modelu
    // ------------------------------------------------------------------

    @Test
    @Tag("bug")
    @DisplayName("BUG: Luka mora moći razlikovati dva plovila po IMO broju (equals/hashCode)")
    void plovilaSeMoguPorediti() {
        // Evidencija ulaska, spisak potjernica i tabele u GUI-ju porede plovila.
        // Bez equals/hashCode svako poređenje pada na identitet reference,
        // pa isto plovilo učitano iz luka.ser nije "jednako" sebi iz prethodne sesije.
        KontejnerskiBrod a = TestFactory.kontejnerski("7777777");
        KontejnerskiBrod b = TestFactory.kontejnerski("7777777");

        assertEquals(a, b, "Plovila sa istim IMO brojem treba da budu jednaka — dodaj equals()/hashCode() u Plovilo.");
        assertEquals(a.hashCode(), b.hashCode(), "hashCode mora biti konzistentan sa equals().");
    }
}
