package org.unibl.etf.pj2.luka.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.TankerCarina;
import org.unibl.etf.pj2.luka.model.classes.TankerObalskaStraza;
import org.unibl.etf.pj2.luka.model.classes.TankerVatrogasci;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testovi {@link PrikazTerminala} — čisto deterministički, bez niti i bez tajmauta.
 */
@DisplayName("PrikazTerminala — vizuelni prikaz terminala (C6)")
class PrikazTerminalaTest {

    // ------------------------------------------------------------------
    // Prazan terminal
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Prazan terminal ima ispravnu oznaku na svakom polju")
    void prazanTerminalImaIspravneOznakeSvuda() {
        Terminal t = new Terminal(0);
        String[][] prikaz = PrikazTerminala.render(t);

        for (int red = 0; red < 4; red++) {
            for (int kolona = 0; kolona < 17; kolona++) {
                String ocekivano;
                if (kolona == 0) {
                    ocekivano = "v";
                } else if (kolona == 1) {
                    ocekivano = "^";
                } else if (red == 0 || red == 3) {
                    ocekivano = "*";
                } else {
                    ocekivano = ".";
                }
                assertEquals(ocekivano, prikaz[red][kolona],
                        "Neočekivana oznaka na polju (" + red + "," + kolona + ")");
            }
        }
    }

    @Test
    @DisplayName("render() vraća matricu dimenzija 4x17 bez null vrijednosti")
    void dimenzijeSuTacneIBezNull() {
        Terminal t = new Terminal(0);
        String[][] prikaz = PrikazTerminala.render(t);

        assertEquals(4, prikaz.length);
        for (String[] red : prikaz) {
            assertEquals(17, red.length);
            for (String polje : red) {
                assertNotNull(polje);
            }
        }
    }

    // ------------------------------------------------------------------
    // Mapiranje tipova na oznake
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Svaki tip plovila se mapira na svoje slovo")
    void tipoviSeMapirajuNaOdgovarajuceSlovo() {
        assertEquals("K", oznakaZaJedno(TestFactory.kontejnerski("1")));
        assertEquals("P", oznakaZaJedno(TestFactory.kruzer("2")));
        assertEquals("T", oznakaZaJedno(TestFactory.tanker("3")));
        assertEquals("O", oznakaZaJedno(TestFactory.kruzerOS("4")));
        assertEquals("C", oznakaZaJedno(TestFactory.kruzerCarina("5")));
        assertEquals("V", oznakaZaJedno(TestFactory.tankerVatrogasci("6")));
    }

    @Test
    @DisplayName("TankerVatrogasci se prikazuje kao V, ne T — identitet službe pobjeđuje tip trupa")
    void identitetSluzbePobjedujeTipTrupa() {
        TankerVatrogasci v = TestFactory.tankerVatrogasci("1");
        assertEquals("V", oznakaZaJedno(v), "Vatrogasni tanker mora prikazati V, ne T.");
    }

    @Test
    @DisplayName("Upaljena rotacija dodaje sufiks R na oznaku službenog plovila")
    void rotacijaDodajeSufiksR() {
        TankerVatrogasci v = TestFactory.tankerVatrogasci("1");
        v.setRotacija(true);
        assertEquals("VR", oznakaZaJedno(v));

        TankerObalskaStraza os = TestFactory.tankerOS("2");
        os.setRotacija(true);
        assertEquals("OR", oznakaZaJedno(os));

        TankerCarina c = TestFactory.tankerCarina("3");
        c.setRotacija(true);
        assertEquals("CR", oznakaZaJedno(c));
    }

    @Test
    @DisplayName("Bez upaljene rotacije nema sufiksa R")
    void bezRotacijeNemaSufiksa() {
        assertEquals("V", oznakaZaJedno(TestFactory.tankerVatrogasci("1")));
    }

    // ------------------------------------------------------------------
    // Zamjena podrazumijevane oznake plovilom
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Plovilo u kanalu zamjenjuje tačku praznog polja")
    void ploviloUKanaluZamjenjujeTacku() {
        Terminal t = new Terminal(0);
        Plovilo p = TestFactory.kontejnerski("1");
        t.getMatrica()[Terminal.KANAL_ULAZ][5].setTrenutnoPlovilo(p);

        String[][] prikaz = PrikazTerminala.render(t);
        assertEquals("K", prikaz[Terminal.KANAL_ULAZ][5]);
    }

    @Test
    @DisplayName("Plovilo na ulaznom polju [0][0] zamjenjuje oznaku v")
    void ploviloNaUlazuZamjenjujeV() {
        Terminal t = new Terminal(0);
        Plovilo p = TestFactory.kontejnerski("1");
        t.getMatrica()[0][0].setTrenutnoPlovilo(p);

        String[][] prikaz = PrikazTerminala.render(t);
        assertEquals("K", prikaz[0][0]);
    }

    @Test
    @DisplayName("Plovilo na doku zamjenjuje oznaku *")
    void ploviloNaDokuZamjenjujeZvjezdicu() {
        Terminal t = new Terminal(0);
        Plovilo p = TestFactory.tanker("1");
        t.getMatrica()[0][2].setTrenutnoPlovilo(p);

        String[][] prikaz = PrikazTerminala.render(t);
        assertEquals("T", prikaz[0][2]);
    }

    // ------------------------------------------------------------------
    // renderAsText()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("renderAsText() vraća tačno 4 reda, poravnata kolonama")
    void renderAsTextVracaCetiriReda() {
        Terminal t = new Terminal(0);
        String tekst = PrikazTerminala.renderAsText(t);

        String[] linije = tekst.split("\\R");
        assertEquals(4, linije.length, "Očekuju se tačno 4 reda (dimenzija terminala).");

        // Redovi 0 i 3 su dokovi (*), redovi 1 i 2 su plovni kanal (.) — prazan terminal
        // nema ni jedno plovilo, pa ni jedan red ne smije sadržavati oznaku plovila.
        assertTrue(linije[0].contains("*"), "Red 0 (dokovi) treba da sadrži prazan dok.");
        assertTrue(linije[3].contains("*"), "Red 3 (dokovi) treba da sadrži prazan dok.");
        assertTrue(linije[1].contains("."), "Red 1 (kanal) treba da sadrži praznu vodu.");
        assertTrue(linije[2].contains("."), "Red 2 (kanal) treba da sadrži praznu vodu.");
    }

    @Test
    @DisplayName("renderAsText() sadrži oznaku plovila kad je terminal zauzet")
    void renderAsTextSadrziOznakuPlovila() {
        Terminal t = new Terminal(0);
        TankerVatrogasci v = TestFactory.tankerVatrogasci("1");
        v.setRotacija(true);
        t.getMatrica()[0][2].setTrenutnoPlovilo(v);

        String tekst = PrikazTerminala.renderAsText(t);
        assertTrue(tekst.contains("VR"), "Tekstualni prikaz treba da sadrži VR za vatrogasce pod rotacijom.");
    }

    // ------------------------------------------------------------------
    // Pomoćna metoda
    // ------------------------------------------------------------------

    private static String oznakaZaJedno(Plovilo p) {
        Terminal t = new Terminal(0);
        t.getMatrica()[0][2].setTrenutnoPlovilo(p);
        return PrikazTerminala.render(t)[0][2];
    }
}
