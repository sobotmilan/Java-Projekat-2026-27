package org.unibl.etf.pj2.luka.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.model.classes.KontejnerskiBrod;
import org.unibl.etf.pj2.luka.model.classes.KontejnerskiBrodObalskaStraza;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.PutnickiKruzer;
import org.unibl.etf.pj2.luka.model.classes.PutnickiKruzerCarina;
import org.unibl.etf.pj2.luka.model.classes.PutnickiKruzerObalskaStraza;
import org.unibl.etf.pj2.luka.model.classes.Tanker;
import org.unibl.etf.pj2.luka.model.classes.TankerCarina;
import org.unibl.etf.pj2.luka.model.classes.TankerObalskaStraza;
import org.unibl.etf.pj2.luka.model.classes.TankerVatrogasci;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlovilaFabrika — konstrukcija po tipu")
class PlovilaFabrikaTest {

    @Test
    @DisplayName("Kontejnerski brod dobija zajednička polja i TEU")
    void kontejnerski() {
        Plovilo p = PlovilaFabrika.napravi(TipPlovila.KONTEJNERSKI, "Aurora", "1", "M-1", "REG-1",
                TestFactory.FOTO, 1500, null);
        assertInstanceOf(KontejnerskiBrod.class, p);
        KontejnerskiBrod kb = (KontejnerskiBrod) p;
        assertEquals("Aurora", kb.getNaziv());
        assertEquals("1", kb.getImoBroj());
        assertEquals(1500, kb.getKapacitetTEU());
    }

    @Test
    @DisplayName("Kontejnerski brod obalske straže dobija spisak potjera")
    void kontejnerskiObalskaStraza() {
        Plovilo p = PlovilaFabrika.napravi(TipPlovila.KONTEJNERSKI_OBALSKA_STRAZA, "Aurora", "2", "M-2", "REG-2",
                TestFactory.FOTO, 1500, TestFactory.SPISAK);
        assertInstanceOf(KontejnerskiBrodObalskaStraza.class, p);
        assertEquals(TestFactory.SPISAK, ((KontejnerskiBrodObalskaStraza) p).getSpisakPotjera());
    }

    @Test
    @DisplayName("Putnički kruzer dobija broj putnika")
    void kruzer() {
        Plovilo p = PlovilaFabrika.napravi(TipPlovila.KRUZER, "Neptun", "3", "M-3", "REG-3",
                TestFactory.FOTO, 800, null);
        assertInstanceOf(PutnickiKruzer.class, p);
        assertEquals(800, ((PutnickiKruzer) p).getBrojPutnika());
    }

    @Test
    @DisplayName("Putnički kruzer obalske straže dobija spisak potjera")
    void kruzerObalskaStraza() {
        Plovilo p = PlovilaFabrika.napravi(TipPlovila.KRUZER_OBALSKA_STRAZA, "Neptun", "4", "M-4", "REG-4",
                TestFactory.FOTO, 800, TestFactory.SPISAK);
        assertInstanceOf(PutnickiKruzerObalskaStraza.class, p);
        assertEquals(TestFactory.SPISAK, ((PutnickiKruzerObalskaStraza) p).getSpisakPotjera());
    }

    @Test
    @DisplayName("Putnički kruzer carine nema spisak potjera")
    void kruzerCarina() {
        Plovilo p = PlovilaFabrika.napravi(TipPlovila.KRUZER_CARINA, "Neptun", "5", "M-5", "REG-5",
                TestFactory.FOTO, 800, TestFactory.SPISAK);
        assertInstanceOf(PutnickiKruzerCarina.class, p);
        assertEquals(800, ((PutnickiKruzerCarina) p).getBrojPutnika());
    }

    @Test
    @DisplayName("Tanker dobija zapreminu u barelima kao realan broj")
    void tanker() {
        Plovilo p = PlovilaFabrika.napravi(TipPlovila.TANKER, "Posejdon", "6", "M-6", "REG-6",
                TestFactory.FOTO, 123456.75, null);
        assertInstanceOf(Tanker.class, p);
        assertEquals(123456.75, ((Tanker) p).getZapreminaBarel(), 0.0001);
    }

    @Test
    @DisplayName("Tanker obalske straže dobija spisak potjera")
    void tankerObalskaStraza() {
        Plovilo p = PlovilaFabrika.napravi(TipPlovila.TANKER_OBALSKA_STRAZA, "Posejdon", "7", "M-7", "REG-7",
                TestFactory.FOTO, 123456.75, TestFactory.SPISAK);
        assertInstanceOf(TankerObalskaStraza.class, p);
        assertEquals(TestFactory.SPISAK, ((TankerObalskaStraza) p).getSpisakPotjera());
    }

    @Test
    @DisplayName("Tanker carine nema spisak potjera")
    void tankerCarina() {
        Plovilo p = PlovilaFabrika.napravi(TipPlovila.TANKER_CARINA, "Posejdon", "8", "M-8", "REG-8",
                TestFactory.FOTO, 123456.75, null);
        assertInstanceOf(TankerCarina.class, p);
    }

    @Test
    @DisplayName("Tanker vatrogasaca se pravi bez spiska potjera")
    void tankerVatrogasci() {
        Plovilo p = PlovilaFabrika.napravi(TipPlovila.TANKER_VATROGASCI, "Posejdon", "9", "M-9", "REG-9",
                TestFactory.FOTO, 123456.75, null);
        assertInstanceOf(TankerVatrogasci.class, p);
    }
}
