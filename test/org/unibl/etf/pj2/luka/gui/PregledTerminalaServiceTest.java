package org.unibl.etf.pj2.luka.gui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.model.classes.Dok;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PregledTerminalaService — tabelarni prikaz")
class PregledTerminalaServiceTest {

    private Luka luka;
    private Terminal t;

    @BeforeEach
    void setUp() {
        luka = TestFactory.luka(1);
        t = luka.getTerminali().get(0);
    }

    @Test
    @DisplayName("Prazan terminal nema redova")
    void praznTerminalNemaRedova() {
        assertTrue(PregledTerminalaService.redovi(t).isEmpty());
    }

    @Test
    @DisplayName("Zaglavlja imaju sedam kolona u dogovorenom redoslijedu")
    void zaglavljaImajuSedamKolona() {
        assertEquals(7, PregledTerminalaService.ZAGLAVLJA.length);
        assertArrayEquals(new String[]{"IMO", "Naziv", "Tip", "Registarski broj",
                "Specifičan atribut", "Služba", "Rotacija"}, PregledTerminalaService.ZAGLAVLJA);
    }

    @Test
    @DisplayName("Red komercijalnog plovila nema službu ni rotaciju")
    void redKomercijalnogPlovila() {
        Dok d = TestFactory.prviSlobodanDok(t);
        d.getLokacija().setTrenutnoPlovilo(TestFactory.kontejnerski("200"));

        List<String[]> redovi = PregledTerminalaService.redovi(t);
        assertEquals(1, redovi.size());
        String[] red = redovi.get(0);
        assertEquals("200", red[0]);
        assertEquals("Kontejnerski brod", red[2]);
        assertEquals("1500 TEU", red[4]);
        assertEquals("—", red[5]);
        assertEquals("—", red[6]);
    }

    @Test
    @DisplayName("Red službenog plovila prikazuje službu i stanje rotacije")
    void redSluzbenogPlovila() {
        Dok d = TestFactory.prviSlobodanDok(t);
        var vatrogasci = TestFactory.tankerVatrogasci("201");
        vatrogasci.setRotacija(true);
        d.getLokacija().setTrenutnoPlovilo(vatrogasci);

        String[] red = PregledTerminalaService.redovi(t).get(0);
        assertEquals("Tanker", red[2]);
        assertEquals("Vatrogasci", red[5]);
        assertEquals("Da", red[6]);
    }

    @Test
    @DisplayName("pronadjiPlovilo pronalazi po IMO broju, vraća null kad ne postoji")
    void pronadjiPlovilo() {
        Dok d = TestFactory.prviSlobodanDok(t);
        Plovilo p = TestFactory.kruzer("202");
        d.getLokacija().setTrenutnoPlovilo(p);

        assertSame(p, PregledTerminalaService.pronadjiPlovilo(t, "202"));
        assertNull(PregledTerminalaService.pronadjiPlovilo(t, "NEMA"));
    }
}
