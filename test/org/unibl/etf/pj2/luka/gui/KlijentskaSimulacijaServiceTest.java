package org.unibl.etf.pj2.luka.gui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.model.classes.Dok;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.simulation.BrodThread;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KlijentskaSimulacijaService — pokretanje, odlazak, dodavanje, kraj simulacije")
class KlijentskaSimulacijaServiceTest {

    private Luka luka;
    private Terminal t;

    @BeforeEach
    void setUp() {
        luka = TestFactory.luka(1);
        t = luka.getTerminali().get(0);
    }

    // ------------------------------------------------------------------
    // Korak 1 — validacija minimuma
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Pozitivan broj do 30 prolazi validaciju")
    void pozitivanBrojDo30Prolazi() {
        assertTrue(KlijentskaSimulacijaService.validirajMinimum("5").isEmpty());
        assertTrue(KlijentskaSimulacijaService.validirajMinimum("30").isEmpty());
    }

    @Test
    @DisplayName("Nula i negativan broj se odbijaju")
    void nulaINegativanSeOdbijaju() {
        assertFalse(KlijentskaSimulacijaService.validirajMinimum("0").isEmpty());
        assertFalse(KlijentskaSimulacijaService.validirajMinimum("-3").isEmpty());
    }

    @Test
    @DisplayName("Broj veći od 30 se odbija")
    void brojVeciOd30SeOdbija() {
        assertFalse(KlijentskaSimulacijaService.validirajMinimum("31").isEmpty());
    }

    @Test
    @DisplayName("Ne-broj se odbija")
    void neBrojSeOdbija() {
        assertFalse(KlijentskaSimulacijaService.validirajMinimum("abc").isEmpty());
    }

    // ------------------------------------------------------------------
    // Korak 3 — odabir za odlazak (C7)
    // ------------------------------------------------------------------

    private BrodThread privezanaNit(Plovilo p, Dok dok) {
        dok.getLokacija().setTrenutnoPlovilo(p);
        return new BrodThread(p, luka, t, dok);
    }

    @Test
    @DisplayName("Broj odabranih za odlazak je tačno ceil(0.15 * broj plovila)")
    void brojOdabranihJeTacnoCeil15Posto() {
        List<BrodThread> naTerminalu = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            naTerminalu.add(privezanaNit(TestFactory.kontejnerski("K" + i), t.getDokovi().get(i)));
        }
        // ceil(0.15 * 20) = 3
        List<BrodThread> odabrani = KlijentskaSimulacijaService.odaberiZaOdlazak(naTerminalu);
        assertEquals(3, odabrani.size());
    }

    @Test
    @DisplayName("Terminal sa bar jednim plovilom mora imati bar jedno označeno za odlazak")
    void baremJednoOznacenoAkoIma() {
        List<BrodThread> naTerminalu = List.of(
                privezanaNit(TestFactory.kontejnerski("SAMO1"), t.getDokovi().get(0)));
        List<BrodThread> odabrani = KlijentskaSimulacijaService.odaberiZaOdlazak(naTerminalu);
        assertEquals(1, odabrani.size());
    }

    @Test
    @DisplayName("Prazan terminal ne bira ništa")
    void praznTerminalNeBiraNista() {
        assertTrue(KlijentskaSimulacijaService.odaberiZaOdlazak(List.of()).isEmpty());
    }

    @Test
    @DisplayName("Službena plovila se biraju samo ako komercijalnih nema dovoljno")
    void sluzbenaSeBirajuSamoAkoNemaDovoljnoKomercijalnih() {
        List<BrodThread> naTerminalu = new ArrayList<>();
        naTerminalu.add(privezanaNit(TestFactory.tankerVatrogasci("V1"), t.getDokovi().get(0)));
        naTerminalu.add(privezanaNit(TestFactory.kontejnerski("K1"), t.getDokovi().get(1)));
        naTerminalu.add(privezanaNit(TestFactory.kontejnerski("K2"), t.getDokovi().get(2)));
        // ceil(0.15 * 3) = 1 -> mora biti komercijalno, ne službeno, iako je službeno prvo u listi
        List<BrodThread> odabrani = KlijentskaSimulacijaService.odaberiZaOdlazak(naTerminalu);
        assertEquals(1, odabrani.size());
        assertFalse(odabrani.get(0).getPlovilo() instanceof org.unibl.etf.pj2.luka.model.interfaces.SluzbenoPlovilo);
    }

    @Test
    @DisplayName("Službena plovila se ipak biraju kad komercijalnih nema dovoljno")
    void sluzbenaSeBirajuKadNemaDovoljnoKomercijalnih() {
        List<BrodThread> naTerminalu = new ArrayList<>();
        naTerminalu.add(privezanaNit(TestFactory.kontejnerski("K1"), t.getDokovi().get(0)));
        for (int i = 0; i < 9; i++) {
            naTerminalu.add(privezanaNit(TestFactory.tankerVatrogasci("V" + i), t.getDokovi().get(i + 1)));
        }
        // 10 ukupno, ceil(0.15*10)=2, samo 1 komercijalno -> mora popuniti sa 1 službenim
        List<BrodThread> odabrani = KlijentskaSimulacijaService.odaberiZaOdlazak(naTerminalu);
        assertEquals(2, odabrani.size());
        long brojSluzbenih = odabrani.stream()
                .filter(bt -> bt.getPlovilo() instanceof org.unibl.etf.pj2.luka.model.interfaces.SluzbenoPlovilo)
                .count();
        assertEquals(1, brojSluzbenih);
    }

    // ------------------------------------------------------------------
    // Korak 4 — dodavanje tokom simulacije (C8/C9)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("imaSlobodnogVezaBiloGdje vraća true kad postoji bar jedan slobodan vez")
    void imaSlobodnogVezaBiloGdjeTrue() {
        assertTrue(KlijentskaSimulacijaService.imaSlobodnogVezaBiloGdje(luka));
    }

    @Test
    @DisplayName("imaSlobodnogVezaBiloGdje vraća false kad je cijela luka puna")
    void imaSlobodnogVezaBiloGdjeFalse() {
        TestFactory.popuniSveDokove(t);
        assertFalse(KlijentskaSimulacijaService.imaSlobodnogVezaBiloGdje(luka));
    }

    @Test
    @DisplayName("dodajTokomSimulacije se odbija kad je luka puna, bez pokretanja niti")
    void dodajTokomSimulacijeOdbijaKadJePuno() {
        TestFactory.popuniSveDokove(t);
        List<String> greske = KlijentskaSimulacijaService.dodajTokomSimulacije(luka, TestFactory.kontejnerski("PUN-1"));
        assertFalse(greske.isEmpty());
        assertNull(KlijentskaSimulacijaService.pronadjiAktivnuNit(luka, "PUN-1"));
    }

    @Test
    @DisplayName("dodajTokomSimulacije pokreće nit koja se registruje u aktivna plovila i na kraju priveže")
    void dodajTokomSimulacijePokrecuNit() throws InterruptedException {
        List<String> greske = KlijentskaSimulacijaService.dodajTokomSimulacije(luka, TestFactory.kontejnerski("NOVO-1"));
        assertTrue(greske.isEmpty());

        long krajnjeVrijeme = System.currentTimeMillis() + 10_000;
        BrodThread bt = null;
        while (System.currentTimeMillis() < krajnjeVrijeme) {
            bt = KlijentskaSimulacijaService.pronadjiAktivnuNit(luka, "NOVO-1");
            if (bt != null && bt.isPrivezan()) {
                break;
            }
            TimeUnit.MILLISECONDS.sleep(20);
        }
        assertNotNull(bt, "Novododato plovilo mora dobiti svoju nit.");
        assertTrue(bt.isPrivezan(), "Novododato plovilo se mora na kraju privezati.");
    }

    // ------------------------------------------------------------------
    // Korak 5 — kraj simulacije (E1)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Simulacija nije završena dok je označeni-za-odlazak još uvijek aktivan")
    void nijeZavrsenaDokJeOznaceniAktivan() {
        BrodThread bt = privezanaNit(TestFactory.kontejnerski("ODLAZI-1"), t.getDokovi().get(0));
        luka.getAktivnaPlovila().add(bt);

        assertFalse(KlijentskaSimulacijaService.jeSimulacijaZavrsena(
                luka, Set.of("ODLAZI-1"), Set.of()));
    }

    @Test
    @DisplayName("Simulacija je završena kad je označeni-za-odlazak napustio (nije više aktivan)")
    void zavrsenaKadOznaceniOtisao() {
        assertTrue(KlijentskaSimulacijaService.jeSimulacijaZavrsena(
                luka, Set.of("ODLAZI-2"), Set.of()));
    }

    @Test
    @DisplayName("Simulacija nije završena dok dodato plovilo nije privezano")
    void nijeZavrsenaDokDodatoNijePrivezano() {
        Plovilo p = TestFactory.kontejnerski("DODATO-1");
        BrodThread bt = new BrodThread(p, luka); // nije privezan (nepokrenuta nit)
        luka.getAktivnaPlovila().add(bt);

        assertFalse(KlijentskaSimulacijaService.jeSimulacijaZavrsena(
                luka, Set.of(), Set.of("DODATO-1")));
    }

    @Test
    @DisplayName("Simulacija je završena kad je dodato plovilo privezano")
    void zavrsenaKadDodatoPrivezano() {
        BrodThread bt = privezanaNit(TestFactory.kontejnerski("DODATO-2"), t.getDokovi().get(0));
        luka.getAktivnaPlovila().add(bt);

        assertTrue(KlijentskaSimulacijaService.jeSimulacijaZavrsena(
                luka, Set.of(), Set.of("DODATO-2")));
    }

    @Test
    @DisplayName("Dodato plovilo čija je nit završila bez privezivanja se tretira kao razriješeno")
    void dodatoBezNitiSeTretiraKaoRazrijeseno() {
        // nit nikad nije ni dodana u aktivna plovila (kao da je završila bez veza)
        assertTrue(KlijentskaSimulacijaService.jeSimulacijaZavrsena(
                luka, Set.of(), Set.of("NESTALO-1")));
    }
}
