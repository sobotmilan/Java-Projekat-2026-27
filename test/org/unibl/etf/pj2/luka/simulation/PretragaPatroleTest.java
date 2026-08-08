package org.unibl.etf.pj2.luka.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.model.classes.Dok;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Polje;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testovi za {@link PretragaPatrole} (D2): pretraga najbliže patrole je port-wide
 * ({@link Luka#getAktivnaPlovila()}), ne ograničena na jedan terminal.
 */
@DisplayName("PretragaPatrole — najbliža patrola na nivou luke (D2)")
class PretragaPatroleTest {

    /**
     * Registruje plovilo kao aktivno na proizvoljnoj (x,y) poziciji unutar zadatog terminala, bez
     * pokretanja niti — koristi predokovani konstruktor sa fiktivnim {@link Dok} objektom čija je
     * jedina svrha da nosi tačno zadate koordinate (nije nužno stvaran vez terminala).
     */
    private static BrodThread pozicioniraj(Plovilo p, Luka luka, Terminal terminal, int x, int y) {
        Dok fiktivniDok = new Dok(new Polje(x, y, "", null), -1);
        BrodThread bt = new BrodThread(p, luka, terminal, fiktivniDok);
        luka.getAktivnaPlovila().add(bt);
        return bt;
    }

    @Test
    @DisplayName("Prazan registar aktivnih plovila vraća null")
    void prazanRegistarVracaNull() {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);

        assertNull(PretragaPatrole.najblizaPatrola(luka, t, 0, 5));
    }

    @Test
    @DisplayName("Samo komercijalna plovila u registru — vraća null")
    void neSluzbenaPlovilaSeIgnorisuIVracaSeNull() {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        pozicioniraj(TestFactory.kontejnerski("K-1"), luka, t, 0, 3);
        pozicioniraj(TestFactory.tanker("T-1"), luka, t, 3, 8);

        assertNull(PretragaPatrole.najblizaPatrola(luka, t, 0, 5));
    }

    @Test
    @DisplayName("Jedina patrola u terminalu se pronalazi")
    void pronalaziJedinuPatroluUIstomTerminalu() {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        Plovilo vatrogasci = TestFactory.tankerVatrogasci("V-1");
        BrodThread bt = pozicioniraj(vatrogasci, luka, t, 3, 4);

        assertSame(bt, PretragaPatrole.najblizaPatrola(luka, t, 0, 5));
    }

    @Test
    @DisplayName("Između više patrola u istom terminalu bira se najbliža po Menhetn rastojanju")
    void biraBliziMedjuVisePatrolaUIstomTerminaluPoManhattanRastojanju() {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);

        // Cilj: (0, 10). Dalja patrola na rastojanju 6, bliža na rastojanju 4.
        BrodThread daljaPatrola = pozicioniraj(TestFactory.tankerOS("OS-1"), luka, t, 0, 4);
        BrodThread blizaPatrola = pozicioniraj(TestFactory.kruzerCarina("C-1"), luka, t, 3, 9);

        assertSame(blizaPatrola, PretragaPatrole.najblizaPatrola(luka, t, 0, 10),
                "Patrola na Menhetn rastojanju 4 mora biti bliža od one na rastojanju 6.");
    }

    @Test
    @DisplayName("Patrola u ciljnom terminalu se preferira nad patrolom u dalekom terminalu (D2: port-wide)")
    void terminalskaRazlikaDominiraNadLokalnimRastojanjem() {
        Luka luka = TestFactory.luka(6);
        Terminal ciljniTerminal = luka.getTerminali().get(0);
        Terminal dalekiTerminal = luka.getTerminali().get(5);

        // U ciljnom terminalu, lokalno rastojanje 10 (nije nula, ali je i dalje u istom terminalu).
        BrodThread uCiljnomTerminalu = pozicioniraj(TestFactory.tankerVatrogasci("V-BLIZU"), luka, ciljniTerminal, 0, 0);
        // U terminalu udaljenom 5 mjesta, čak i na identičnim lokalnim koordinatama kao cilj.
        pozicioniraj(TestFactory.tankerVatrogasci("V-DALEKO"), luka, dalekiTerminal, 0, 10);

        assertSame(uCiljnomTerminalu, PretragaPatrole.najblizaPatrola(luka, ciljniTerminal, 0, 10),
                "Vatrogasac u ciljnom terminalu mora biti izabran ispred onog u terminalu udaljenom 5 mjesta, "
                        + "čak i uz veće lokalno rastojanje unutar terminala.");
    }

    @Test
    @DisplayName("Sve tri patrolne službe (vatrogasci, obalska straža, carina) se prepoznaju")
    void sveTriPatrolneSluzbeSePrepoznaju() {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);

        Luka lukaVatrogasci = TestFactory.luka(1);
        Terminal tv = lukaVatrogasci.getTerminali().get(0);
        BrodThread btv = pozicioniraj(TestFactory.tankerVatrogasci("SAMO-V"), lukaVatrogasci, tv, 0, 5);
        assertSame(btv, PretragaPatrole.najblizaPatrola(lukaVatrogasci, tv, 0, 5));

        Luka lukaOS = TestFactory.luka(1);
        Terminal tos = lukaOS.getTerminali().get(0);
        BrodThread btos = pozicioniraj(TestFactory.kontejnerskiOS("SAMO-OS"), lukaOS, tos, 0, 5);
        assertSame(btos, PretragaPatrole.najblizaPatrola(lukaOS, tos, 0, 5));

        Luka lukaCarina = TestFactory.luka(1);
        Terminal tc = lukaCarina.getTerminali().get(0);
        BrodThread btc = pozicioniraj(TestFactory.kruzerCarina("SAMO-C"), lukaCarina, tc, 0, 5);
        assertSame(btc, PretragaPatrole.najblizaPatrola(lukaCarina, tc, 0, 5));
    }

    @Test
    @DisplayName("Nepozicionirano plovilo (x/y == -1, još u kanalu prije ulaska) se preskače")
    void nepozicioniranoPloviloSePreskace() {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);

        BrodThread nepozicionirano = new BrodThread(TestFactory.tankerVatrogasci("V-NEPOZ"), luka);
        luka.getAktivnaPlovila().add(nepozicionirano);

        assertNull(PretragaPatrole.najblizaPatrola(luka, t, 0, 5),
                "Plovilo koje još nije pozicionirano ni u jednom terminalu ne smije biti kandidat.");
    }
}
