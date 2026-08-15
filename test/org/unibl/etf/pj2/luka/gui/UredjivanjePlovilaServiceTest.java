package org.unibl.etf.pj2.luka.gui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.model.classes.KontejnerskiBrod;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UredjivanjePlovilaService — dodavanje/izmjena/brisanje kroz Terminal")
class UredjivanjePlovilaServiceTest {

    private Luka luka;
    private Terminal t;

    @BeforeEach
    void setUp() {
        luka = TestFactory.luka(1);
        t = luka.getTerminali().get(0);
    }

    @Test
    @DisplayName("Dodavanje plovila smanjuje broj slobodnih vezova")
    void dodavanjeSmanjujeSlobodneVezove() {
        int prije = t.getBrojSlobodnihVezova();
        List<String> greske = UredjivanjePlovilaService.dodajPlovilo(luka, t, TestFactory.kontejnerski("100"));
        assertTrue(greske.isEmpty());
        assertEquals(prije - 1, t.getBrojSlobodnihVezova());
        assertEquals(prije - 1, t.getBrojRaspolozivihVezova());
    }

    @Test
    @DisplayName("Brisanje plovila vraća broj slobodnih vezova")
    void brisanjeVracaSlobodneVezove() {
        int prije = t.getBrojSlobodnihVezova();
        UredjivanjePlovilaService.dodajPlovilo(luka, t, TestFactory.kontejnerski("101"));
        assertTrue(UredjivanjePlovilaService.obrisiPlovilo(t, "101"));
        assertEquals(prije, t.getBrojSlobodnihVezova());
    }

    @Test
    @DisplayName("Brisanje nepostojećeg IMO broja vraća false")
    void brisanjeNepostojeceg() {
        assertFalse(UredjivanjePlovilaService.obrisiPlovilo(t, "NEMA-TAKVOG"));
    }

    @Test
    @DisplayName("Dodavanje duplikata IMO broja se odbija")
    void dodavanjeDuplikataSeOdbija() {
        UredjivanjePlovilaService.dodajPlovilo(luka, t, TestFactory.kontejnerski("102"));
        List<String> greske = UredjivanjePlovilaService.dodajPlovilo(luka, t, TestFactory.kontejnerski("102"));
        assertFalse(greske.isEmpty());
    }

    @Test
    @DisplayName("Izmjena ne mijenja broj vezova")
    void izmjenaNeMijenjaBrojVezova() {
        UredjivanjePlovilaService.dodajPlovilo(luka, t, TestFactory.kontejnerski("103"));
        int poslijeDodavanja = t.getBrojSlobodnihVezova();

        KontejnerskiBrod azurirano = new KontejnerskiBrod("Novo ime", "103", "M-novi", "REG-novi", TestFactory.FOTO, 999);
        List<String> greske = UredjivanjePlovilaService.izmijeniPlovilo(luka, t, "103", azurirano);

        assertTrue(greske.isEmpty());
        assertEquals(poslijeDodavanja, t.getBrojSlobodnihVezova());
        assertEquals("Novo ime", PregledTerminalaService.pronadjiPlovilo(t, "103").getNaziv());
    }

    @Test
    @DisplayName("Dodavanje u pun terminal se odbija")
    void dodavanjeUPunTerminalSeOdbija() {
        TestFactory.popuniSveDokove(t);
        List<String> greske = UredjivanjePlovilaService.dodajPlovilo(luka, t, TestFactory.kontejnerski("104"));
        assertFalse(greske.isEmpty());
    }
}
