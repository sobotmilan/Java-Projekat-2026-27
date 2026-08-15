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

    @Test
    @DisplayName("Izmjena naziva ne mijenja brzinu plovila")
    void izmjenaNazivaNeMijenjaBrzinu() {
        UredjivanjePlovilaService.dodajPlovilo(luka, t, TestFactory.kontejnerski("105"));
        double originalnaBrzina = PregledTerminalaService.pronadjiPlovilo(t, "105").getBrzina();

        KontejnerskiBrod azurirano = new KontejnerskiBrod("Novo ime", "105", "M-novi", "REG-novi", TestFactory.FOTO, 999);
        List<String> greske = UredjivanjePlovilaService.izmijeniPlovilo(luka, t, "105", azurirano);

        assertTrue(greske.isEmpty());
        assertEquals(originalnaBrzina, PregledTerminalaService.pronadjiPlovilo(t, "105").getBrzina());
    }

    @Test
    @DisplayName("Izmjena službenog plovila čuva stanje rotacije")
    void izmjenaCuvaRotaciju() {
        var vatrogasci = TestFactory.tankerVatrogasci("106");
        vatrogasci.setRotacija(true);
        List<String> greskeDodavanja = UredjivanjePlovilaService.dodajPlovilo(luka, t, vatrogasci);
        assertTrue(greskeDodavanja.isEmpty());

        var azurirano = TestFactory.tankerVatrogasci("106");
        List<String> greske = UredjivanjePlovilaService.izmijeniPlovilo(luka, t, "106", azurirano);

        assertTrue(greske.isEmpty());
        assertTrue(azurirano.isRotacija());
    }

    @Test
    @DisplayName("izmijeniPlovilo(..., rotacijaEksplicitnoZadata=true) NE prepisuje rotaciju kandidata starom vrijednošću")
    void izmjenaSaEksplicitnomRotacijomNePrepisujeVrijednost() {
        var vatrogasci = TestFactory.tankerVatrogasci("109");
        vatrogasci.setRotacija(true);
        assertTrue(UredjivanjePlovilaService.dodajPlovilo(luka, t, vatrogasci).isEmpty());

        var azurirano = TestFactory.tankerVatrogasci("109");
        azurirano.setRotacija(false);
        List<String> greske = UredjivanjePlovilaService.izmijeniPlovilo(luka, t, "109", azurirano, true);

        assertTrue(greske.isEmpty());
        assertFalse(azurirano.isRotacija(),
                "Sa eksplicitnom zastavicom, kandidatova rotacija mora ostati onakva kakvu ju je pozivalac postavio.");
    }

    @Test
    @DisplayName("izmijeniPlovilo(..., rotacijaEksplicitnoZadata=false) i dalje prenosi rotaciju kao ranije (G1)")
    void izmjenaBezEksplicitneZastaviceIDaljeCuvaRotaciju() {
        var vatrogasci = TestFactory.tankerVatrogasci("110");
        vatrogasci.setRotacija(true);
        assertTrue(UredjivanjePlovilaService.dodajPlovilo(luka, t, vatrogasci).isEmpty());

        var azurirano = TestFactory.tankerVatrogasci("110");
        List<String> greske = UredjivanjePlovilaService.izmijeniPlovilo(luka, t, "110", azurirano, false);

        assertTrue(greske.isEmpty());
        assertTrue(azurirano.isRotacija());
    }

    @Test
    @DisplayName("IMO broj se može ponovo iskoristiti nakon što je plovilo obrisano")
    void imoSeMozePonovoIskoristitiNakonBrisanja() {
        UredjivanjePlovilaService.dodajPlovilo(luka, t, TestFactory.kontejnerski("107"));
        assertTrue(UredjivanjePlovilaService.obrisiPlovilo(t, "107"));

        List<String> greske = UredjivanjePlovilaService.dodajPlovilo(luka, t, TestFactory.kruzer("107"));
        assertTrue(greske.isEmpty());
    }

    @Test
    @DisplayName("Dodavanje briše zaostalu evidenciju ulaska za taj IMO")
    void dodavanjeBrisePreostaluEvidenciju() {
        luka.addToEvidencija("108", java.time.LocalDateTime.now().minusDays(3));
        List<String> greske = UredjivanjePlovilaService.dodajPlovilo(luka, t, TestFactory.kontejnerski("108"));
        assertTrue(greske.isEmpty());
        assertFalse(luka.getEvidencijaUlaska().containsKey("108"));
    }
}
