package org.unibl.etf.pj2.luka.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TipPlovila — mapiranje ka konkretnim klasama")
class TipPlovilaTest {

    @Test
    @DisplayName("odObjekta prepoznaje svih devet konkretnih tipova")
    void odObjektaPrepoznajeSvihDevet() {
        assertEquals(TipPlovila.KONTEJNERSKI, TipPlovila.odObjekta(TestFactory.kontejnerski("1")));
        assertEquals(TipPlovila.KONTEJNERSKI_OBALSKA_STRAZA, TipPlovila.odObjekta(TestFactory.kontejnerskiOS("2")));
        assertEquals(TipPlovila.KRUZER, TipPlovila.odObjekta(TestFactory.kruzer("3")));
        assertEquals(TipPlovila.KRUZER_OBALSKA_STRAZA, TipPlovila.odObjekta(TestFactory.kruzerOS("4")));
        assertEquals(TipPlovila.KRUZER_CARINA, TipPlovila.odObjekta(TestFactory.kruzerCarina("5")));
        assertEquals(TipPlovila.TANKER, TipPlovila.odObjekta(TestFactory.tanker("6")));
        assertEquals(TipPlovila.TANKER_OBALSKA_STRAZA, TipPlovila.odObjekta(TestFactory.tankerOS("7")));
        assertEquals(TipPlovila.TANKER_CARINA, TipPlovila.odObjekta(TestFactory.tankerCarina("8")));
        assertEquals(TipPlovila.TANKER_VATROGASCI, TipPlovila.odObjekta(TestFactory.tankerVatrogasci("9")));
    }

    @Test
    @DisplayName("Samo tipovi obalske straže zahtijevaju spisak potjera")
    void samoObalskaStrazaZahtijevaSpisakPotjera() {
        assertTrue(TipPlovila.KONTEJNERSKI_OBALSKA_STRAZA.zahtijevaSpisakPotjera());
        assertTrue(TipPlovila.KRUZER_OBALSKA_STRAZA.zahtijevaSpisakPotjera());
        assertTrue(TipPlovila.TANKER_OBALSKA_STRAZA.zahtijevaSpisakPotjera());

        assertFalse(TipPlovila.KONTEJNERSKI.zahtijevaSpisakPotjera());
        assertFalse(TipPlovila.KRUZER.zahtijevaSpisakPotjera());
        assertFalse(TipPlovila.TANKER.zahtijevaSpisakPotjera());
        assertFalse(TipPlovila.KRUZER_CARINA.zahtijevaSpisakPotjera());
        assertFalse(TipPlovila.TANKER_CARINA.zahtijevaSpisakPotjera());
        assertFalse(TipPlovila.TANKER_VATROGASCI.zahtijevaSpisakPotjera());
    }
}
