package org.unibl.etf.pj2.luka.gui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.model.interfaces.SluzbenoPlovilo;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlovilaFormaDijalog — checkbox rotacije (C6)")
class PlovilaFormaDijalogTest {

    private Luka luka;
    private Terminal t;

    @BeforeEach
    void setUp() {
        luka = TestFactory.luka(1);
        t = luka.getTerminali().get(0);
    }

    @Test
    @DisplayName("Komercijalno plovilo nema checkbox za rotaciju")
    void komercijalnoPloviloNemaCheckbox() {
        PlovilaFormaDijalog dijalog = new PlovilaFormaDijalog(null, luka, t, TipPlovila.KONTEJNERSKI, null);
        try {
            assertFalse(dijalog.imaRotacijuCheckbox());
        } finally {
            dijalog.dispose();
        }
    }

    @Test
    @DisplayName("Službeno plovilo ima checkbox za rotaciju")
    void sluzbenoPloviloImaCheckbox() {
        PlovilaFormaDijalog dijalog = new PlovilaFormaDijalog(null, luka, t, TipPlovila.TANKER_VATROGASCI, null);
        try {
            assertTrue(dijalog.imaRotacijuCheckbox());
        } finally {
            dijalog.dispose();
        }
    }

    @Test
    @DisplayName("Rotacija zadata u formi preživi dodavanje")
    void rotacijaZadataUFormiPrezivljavaDodavanje() {
        PlovilaFormaDijalog dijalog = new PlovilaFormaDijalog(null, luka, t, TipPlovila.TANKER_VATROGASCI, null);
        try {
            dijalog.popuniZaTest("Vatra-1", "9001", "M-1", "REG-1", TestFactory.FOTO, "5000", null);
            dijalog.postaviRotacijuZaTest(true);

            dijalog.pokusajSacuvaj();

            assertTrue(dijalog.jeSacuvano());
            Plovilo sacuvano = PregledTerminalaService.pronadjiPlovilo(t, "9001");
            assertNotNull(sacuvano);
            assertTrue(((SluzbenoPlovilo) sacuvano).isRotacija());
        } finally {
            dijalog.dispose();
        }
    }

    @Test
    @DisplayName("Izmjena sa uključene na isključenu rotaciju stvarno je isključi")
    void izmjenaIskljucujeRotaciju() {
        var vatrogasci = TestFactory.tankerVatrogasci("9002");
        vatrogasci.setRotacija(true);
        assertTrue(UredjivanjePlovilaService.dodajPlovilo(luka, t, vatrogasci).isEmpty());

        PlovilaFormaDijalog dijalog = new PlovilaFormaDijalog(null, luka, t, TipPlovila.TANKER_VATROGASCI, vatrogasci);
        try {
            assertTrue(dijalog.jeRotacijaOznacenaZaTest(),
                    "Checkbox mora biti predpopunjen trenutnim stanjem rotacije.");

            dijalog.postaviRotacijuZaTest(false);
            dijalog.pokusajSacuvaj();

            assertTrue(dijalog.jeSacuvano());
            Plovilo azurirano = PregledTerminalaService.pronadjiPlovilo(t, "9002");
            assertNotNull(azurirano);
            assertFalse(((SluzbenoPlovilo) azurirano).isRotacija());
        } finally {
            dijalog.dispose();
        }
    }
}
