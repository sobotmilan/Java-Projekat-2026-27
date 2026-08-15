package org.unibl.etf.pj2.luka.gui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.model.classes.Dok;
import org.unibl.etf.pj2.luka.model.classes.KontejnerskiBrod;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.PutnickiKruzer;
import org.unibl.etf.pj2.luka.model.classes.Tanker;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlovilaValidator — pravila unosa")
class PlovilaValidatorTest {

    private Luka luka;
    private Terminal t;

    @BeforeEach
    void setUp() {
        luka = TestFactory.luka(1);
        t = luka.getTerminali().get(0);
    }

    @Test
    @DisplayName("Prazan IMO se odbija")
    void praznIMo() {
        KontejnerskiBrod kb = new KontejnerskiBrod("Aurora", "", "M-1", "REG-1", TestFactory.FOTO, 100);
        List<String> greske = PlovilaValidator.validiraj(luka, kb, null);
        assertFalse(greske.isEmpty());
    }

    @Test
    @DisplayName("Duplikat IMO na doku terminala se odbija")
    void duplikatImoNaDoku() {
        Dok dok = TestFactory.prviSlobodanDok(t);
        dok.getLokacija().setTrenutnoPlovilo(TestFactory.kontejnerski("DUP"));

        KontejnerskiBrod kandidat = new KontejnerskiBrod("Neptun", "DUP", "M-2", "REG-2", TestFactory.FOTO, 100);
        List<String> greske = PlovilaValidator.validiraj(luka, kandidat, null);
        assertFalse(greske.isEmpty());
    }

    @Test
    @DisplayName("IMO prisutan samo u evidenciji ulaska (plovilo davno otišlo) ne blokira validaciju")
    void imoUEvidencijiBezFizickogPrisustvaNeBlokira() {
        luka.addToEvidencija("EVID", java.time.LocalDateTime.now());
        KontejnerskiBrod kandidat = new KontejnerskiBrod("Neptun", "EVID", "M-2", "REG-2", TestFactory.FOTO, 100);
        List<String> greske = PlovilaValidator.validiraj(luka, kandidat, null);
        assertTrue(greske.isEmpty());
    }

    @Test
    @DisplayName("Izuzeti IMO (izmjena bez promjene IMO-a) prolazi")
    void izuzetiImoProlazi() {
        Dok dok = TestFactory.prviSlobodanDok(t);
        dok.getLokacija().setTrenutnoPlovilo(TestFactory.kontejnerski("SAM"));

        KontejnerskiBrod azurirano = new KontejnerskiBrod("Novo ime", "SAM", "M-2", "REG-2", TestFactory.FOTO, 200);
        List<String> greske = PlovilaValidator.validiraj(luka, azurirano, "SAM");
        assertTrue(greske.isEmpty());
    }

    @Test
    @DisplayName("Nepozitivan TEU se odbija")
    void nepozitivanTeu() {
        KontejnerskiBrod kb = new KontejnerskiBrod("Aurora", "10", "M-1", "REG-1", TestFactory.FOTO, 0);
        List<String> greske = PlovilaValidator.validiraj(luka, kb, null);
        assertFalse(greske.isEmpty());
    }

    @Test
    @DisplayName("Nepozitivan broj putnika se odbija")
    void nepozitivanBrojPutnika() {
        PutnickiKruzer pk = new PutnickiKruzer("Neptun", "11", "M-1", "REG-1", TestFactory.FOTO, -5);
        List<String> greske = PlovilaValidator.validiraj(luka, pk, null);
        assertFalse(greske.isEmpty());
    }

    @Test
    @DisplayName("Nepozitivna zapremina se odbija")
    void nepozitivnaZapremina() {
        Tanker t2 = new Tanker("Posejdon", "12", "M-1", "REG-1", TestFactory.FOTO, 0.0);
        List<String> greske = PlovilaValidator.validiraj(luka, t2, null);
        assertFalse(greske.isEmpty());
    }

    @Test
    @DisplayName("Nedostajuća fotografija se odbija")
    void nedostajucaFotografija() {
        KontejnerskiBrod kb = new KontejnerskiBrod("Aurora", "13", "M-1", "REG-1", null, 100);
        List<String> greske = PlovilaValidator.validiraj(luka, kb, null);
        assertFalse(greske.isEmpty());
    }

    @Test
    @DisplayName("Validan kandidat prolazi bez grešaka")
    void validanKandidatProlazi() {
        KontejnerskiBrod kb = new KontejnerskiBrod("Aurora", "14", "M-1", "REG-1", TestFactory.FOTO, 100);
        List<String> greske = PlovilaValidator.validiraj(luka, kb, null);
        assertTrue(greske.isEmpty());
    }
}
