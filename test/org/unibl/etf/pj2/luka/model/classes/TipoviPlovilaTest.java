package org.unibl.etf.pj2.luka.model.classes;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.model.interfaces.Carina;
import org.unibl.etf.pj2.luka.model.interfaces.ObalskaStraza;
import org.unibl.etf.pj2.luka.model.interfaces.Vatrogasci;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testovi hijerarhije tipova plovila, markerskih interfejsa i prioriteta pod rotacijom.
 */
@DisplayName("Tipovi plovila i prioriteti")
class TipoviPlovilaTest {

    // ------------------------------------------------------------------
    // BUCKET A — dozvoljene kombinacije tipova iz specifikacije
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Kontejnerski brod može biti samo obalska straža")
    void kontejnerskiKombinacije() {
        assertTrue(TestFactory.kontejnerskiOS("1") instanceof KontejnerskiBrod);
        assertTrue(TestFactory.kontejnerskiOS("1") instanceof ObalskaStraza);
        assertFalse(TestFactory.kontejnerski("2") instanceof ObalskaStraza);
    }

    @Test
    @DisplayName("Kruzer može biti obalska straža ili carina, ali ne i vatrogasni")
    void kruzerKombinacije() {
        assertTrue(TestFactory.kruzerOS("1") instanceof ObalskaStraza);
        assertTrue(TestFactory.kruzerCarina("2") instanceof Carina);
        assertFalse(TestFactory.kruzerCarina("2") instanceof Vatrogasci);
        assertFalse(TestFactory.kruzerOS("1") instanceof Vatrogasci);
    }

    @Test
    @DisplayName("Tanker može biti obalska straža, carina i vatrogasni")
    void tankerKombinacije() {
        assertTrue(TestFactory.tankerOS("1") instanceof ObalskaStraza);
        assertTrue(TestFactory.tankerCarina("2") instanceof Carina);
        assertTrue(TestFactory.tankerVatrogasci("3") instanceof Vatrogasci);
    }

    @Test
    @DisplayName("Službena plovila nose specifične atribute svog osnovnog tipa")
    void sluzbenaZadrzavajuAtributeOsnovnogTipa() {
        assertEquals(1500, TestFactory.kontejnerskiOS("1").getKapacitetTEU());
        assertEquals(800, TestFactory.kruzerCarina("2").getBrojPutnika());
        assertEquals(120000.0, TestFactory.tankerVatrogasci("3").getZapreminaBarel(), 0.0001);
    }

    @Test
    @DisplayName("Obalska straža nosi spisak IMO brojeva za potjernicom")
    void obalskaStrazaImaSpisakPotjera() {
        ObalskaStraza os = TestFactory.tankerOS("1");
        assertNotNull(os.getSpisakPotjera());
        assertEquals(TestFactory.SPISAK, os.getSpisakPotjera());
    }

    // ------------------------------------------------------------------
    // BUCKET A — prioriteti
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Bez upaljene rotacije službeno plovilo ima prioritet običnog plovila")
    void bezRotacijeNemaPrioriteta() {
        assertEquals(10, TestFactory.tankerVatrogasci("1").getPrioritet());
        assertEquals(10, TestFactory.tankerOS("2").getPrioritet());
        assertEquals(10, TestFactory.tankerCarina("3").getPrioritet());
        assertEquals(10, TestFactory.kontejnerski("4").getPrioritet());
    }

    @Test
    @DisplayName("Pod rotacijom važi redoslijed: vatrogasci > obalska straža > carina")
    void redoslijedPrioritetaPodRotacijom() {
        TankerVatrogasci v = TestFactory.tankerVatrogasci("1");
        TankerObalskaStraza os = TestFactory.tankerOS("2");
        TankerCarina c = TestFactory.tankerCarina("3");
        Tanker obican = TestFactory.tanker("4");

        v.setRotacija(true);
        os.setRotacija(true);
        c.setRotacija(true);

        // Niža vrijednost = viši prioritet.
        assertTrue(v.getPrioritet() < os.getPrioritet(), "Vatrogasci moraju imati viši prioritet od obalske straže.");
        assertTrue(os.getPrioritet() < c.getPrioritet(), "Obalska straža mora imati viši prioritet od carine.");
        assertTrue(c.getPrioritet() < obican.getPrioritet(), "Carina pod rotacijom mora imati viši prioritet od komercijalnog plovila.");
    }

    @Test
    @DisplayName("Gašenje rotacije vraća plovilo na običan prioritet")
    void gasenjeRotacijeVracaPrioritet() {
        TankerVatrogasci v = TestFactory.tankerVatrogasci("1");
        v.setRotacija(true);
        assertEquals(1, v.getPrioritet());
        v.setRotacija(false);
        assertEquals(10, v.getPrioritet());
    }

    @Test
    @DisplayName("Rotacija je podrazumijevano ugašena")
    void rotacijaJePodrazumijevanoUgasena() {
        assertFalse(TestFactory.tankerVatrogasci("1").isRotacija());
        assertFalse(TestFactory.kruzerOS("2").isRotacija());
        assertFalse(TestFactory.tankerCarina("3").isRotacija());
    }

    // ------------------------------------------------------------------
    // BUCKET C — testovi koji zahtijevaju refaktor R1 (vidi PRONALASCI.md)
    // Otkomentarisati tek nakon uvođenja interfejsa SluzbenoPlovilo.
    // ------------------------------------------------------------------

    @Test
    @Disabled("Zahtijeva refaktor R1: zajednički interfejs SluzbenoPlovilo sa isRotacija()/setRotacija().")
    @DisplayName("R1: rotacija se može uključiti polimorfno, bez instanceof lanca")
    void rotacijaSeMozeUkljucitiPolimorfno() {
        // Trenutno je nemoguće: setRotacija() postoji zasebno na 6 klasa i nije u nadtipu.
        // Zbog toga je linija `this.plovilo.setRotacija(true)` u BrodThread-u zakomentarisana.
        //
        // Nakon refaktora ovo treba da radi:
        //
        // List<Plovilo> sluzbena = List.of(
        //         TestFactory.tankerVatrogasci("1"),
        //         TestFactory.tankerOS("2"),
        //         TestFactory.kruzerCarina("3"));
        //
        // for (Plovilo p : sluzbena) {
        //     assertTrue(p instanceof SluzbenoPlovilo);
        //     ((SluzbenoPlovilo) p).setRotacija(true);
        //     assertTrue(((SluzbenoPlovilo) p).isRotacija());
        //     assertTrue(p.getPrioritet() < 10);
        // }
        fail("Test nije implementiran — čeka refaktor R1.");
    }

    // ------------------------------------------------------------------
    // BUCKET B — mrtvo polje u modelu
    // ------------------------------------------------------------------

    @Test
    @Tag("bug")
    @DisplayName("BUG: polje 'prioritet' proslijeđeno konstruktoru se nikada ne koristi")
    void poljePrioritetJeMrtvoUSluzbenimKlasama() {
        // TankerVatrogasci poziva super(..., 1), ali getPrioritet() je override-ovan
        // i vraća isRotacija() ? 1 : 10 — vrijednost iz konstruktora se ignoriše.
        // Time postoje dva izvora istine za istu stvar.
        TankerVatrogasci v = TestFactory.tankerVatrogasci("1");

        // Ovo pada dok postoji duplirana logika: konstruktor kaže 1, getter kaže 10.
        assertEquals(1, v.getPrioritet(),
                "Konstruktor postavlja prioritet 1, ali getPrioritet() ga ignoriše. "
                        + "Ukloni parametar 'prioritet' iz konstruktora službenih klasa ili ga stvarno koristi.");
    }
}
