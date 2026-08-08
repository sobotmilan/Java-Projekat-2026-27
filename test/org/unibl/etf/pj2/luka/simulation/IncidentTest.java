package org.unibl.etf.pj2.luka.simulation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.TankerVatrogasci;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testovi za {@link Incident} (I6/I7/D6): model podataka jednog incidenta i njegov binarni upis.
 */
@DisplayName("Incident — zapis uviđaja (I6/I7) i apsolutne putanje fotografija (D6)")
class IncidentTest {

    private Path privremeniDirektorijum;

    @BeforeEach
    void napraviPrivremeniDirektorijum() throws IOException {
        privremeniDirektorijum = Files.createTempDirectory("incident-test-");
    }

    @AfterEach
    void obrisiPrivremeniDirektorijum() throws IOException {
        if (privremeniDirektorijum == null || !Files.exists(privremeniDirektorijum)) {
            return;
        }
        try (Stream<Path> tok = Files.walk(privremeniDirektorijum)) {
            tok.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignore) {
                    // Čišćenje nije kritično za ispravnost testa.
                }
            });
        }
    }

    private static Incident napraviIncident() {
        List<Plovilo> ucesnici = new ArrayList<>();
        ucesnici.add(TestFactory.kontejnerski("SUDAR-1"));
        ucesnici.add(TestFactory.tanker("SUDAR-2"));

        List<Plovilo> sluzbena = new ArrayList<>();
        TankerVatrogasci vatrogasci = TestFactory.tankerVatrogasci("PATROLA-1");
        vatrogasci.setRotacija(true);
        sluzbena.add(vatrogasci);

        return new Incident(ucesnici, sluzbena, LocalDateTime.of(2026, 8, 8, 12, 30), 4200L, 2);
    }

    @Test
    @DisplayName("Konstruktor čuva učesnike sudara i odazvana službena plovila")
    void konstruktorCuvaUcesnikeISluzbenaPlovila() {
        Incident incident = napraviIncident();

        assertEquals(2, incident.getUcesniciSudara().size());
        assertEquals(1, incident.getOdazvanaSluzbenaPlovila().size());
        assertEquals("SUDAR-1", incident.getUcesniciSudara().get(0).getImoBroj());
        assertEquals("SUDAR-2", incident.getUcesniciSudara().get(1).getImoBroj());
        assertEquals("PATROLA-1", incident.getOdazvanaSluzbenaPlovila().get(0).getImoBroj());
    }

    @Test
    @DisplayName("Vrijeme, trajanje uviđaja i id terminala se čuvaju bez izmjene")
    void ostaliAtributiSeCuvajuBezIzmjene() {
        LocalDateTime vrijeme = LocalDateTime.of(2026, 8, 8, 12, 30);
        Incident incident = new Incident(
                List.of(TestFactory.kontejnerski("A")), List.of(TestFactory.tankerVatrogasci("B")),
                vrijeme, 7500L, 3);

        assertEquals(vrijeme, incident.getVrijeme());
        assertEquals(7500L, incident.getTrajanjeUvidjajaMs());
        assertEquals(3, incident.getIdTerminala());
    }

    @Test
    @DisplayName("D6: putanje do fotografija svih učesnika se čuvaju kao apsolutne, čak i ako je izvorni File relativan")
    void putanjeDoFotografijaSuApsolutne() {
        Incident incident = napraviIncident();

        assertEquals(3, incident.getApsolutnePutanjeFotografija().size(),
                "Sva tri plovila (dva učesnika sudara + jedno službeno) imaju fotografiju iz TestFactory.FOTO.");

        for (String putanja : incident.getApsolutnePutanjeFotografija()) {
            assertTrue(new File(putanja).isAbsolute(),
                    "Putanja '" + putanja + "' mora biti apsolutna (D6), bez obzira što je "
                            + "TestFactory.FOTO relativan File.");
        }

        String ocekivano = TestFactory.FOTO.getAbsolutePath();
        assertTrue(incident.getApsolutnePutanjeFotografija().stream().allMatch(ocekivano::equals),
                "Sve tri putanje potiču od istog TestFactory.FOTO fajla, pa moraju biti identične.");
    }

    @Test
    @DisplayName("Plovilo bez fotografije (null) ne dodaje unos u listu putanja")
    void ploviloBezFotografijeSePreskace() {
        Plovilo bezFoto = new org.unibl.etf.pj2.luka.model.classes.KontejnerskiBrod(
                "BezFoto", "0000001", "M-1", "REG-1", null, 1500);

        Incident incident = new Incident(List.of(bezFoto), List.of(), LocalDateTime.now(), 3000L, 0);

        assertTrue(incident.getApsolutnePutanjeFotografija().isEmpty());
    }

    @Test
    @DisplayName("sacuvaj(direktorijum) upisuje binarni fajl u zadati direktorijum")
    void sacuvajUZadatiDirektorijumPravestvoriBinarniFajl() {
        Incident incident = napraviIncident();

        File fajl = incident.sacuvaj(privremeniDirektorijum.toFile());

        assertNotNull(fajl);
        assertTrue(fajl.exists(), "Fajl incidenta mora postojati na disku nakon sacuvaj().");
        assertTrue(fajl.length() > 0, "Fajl ne smije biti prazan.");
        assertEquals(privremeniDirektorijum.toFile(), fajl.getParentFile());
    }

    @Test
    @DisplayName("Svaki poziv sacuvaj() proizvodi poseban fajl — jedan fajl po slučaju (I7)")
    void svakiIncidentDobijaSvojFajl() {
        Incident prvi = napraviIncident();
        Incident drugi = napraviIncident();

        File fajl1 = prvi.sacuvaj(privremeniDirektorijum.toFile());
        File fajl2 = drugi.sacuvaj(privremeniDirektorijum.toFile());

        assertNotEquals(fajl1.getName(), fajl2.getName(),
                "Dva različita incidenta ne smiju prepisati isti binarni fajl.");
        assertTrue(fajl1.exists());
        assertTrue(fajl2.exists());
    }

    @Test
    @DisplayName("Round-trip: incident učitan nakon sacuvaj() ima iste podatke kao original")
    void roundTripSacuvajUcitaj() {
        Incident original = napraviIncident();

        File fajl = original.sacuvaj(privremeniDirektorijum.toFile());
        Incident ucitan = Incident.ucitaj(fajl);

        assertNotNull(ucitan);
        assertEquals(original.getUcesniciSudara(), ucitan.getUcesniciSudara(),
                "Plovila se porede preko IMO broja (equals), pa lista mora ostati jednaka.");
        assertEquals(original.getOdazvanaSluzbenaPlovila(), ucitan.getOdazvanaSluzbenaPlovila());
        assertEquals(original.getVrijeme(), ucitan.getVrijeme());
        assertEquals(original.getTrajanjeUvidjajaMs(), ucitan.getTrajanjeUvidjajaMs());
        assertEquals(original.getIdTerminala(), ucitan.getIdTerminala());
        assertEquals(original.getApsolutnePutanjeFotografija(), ucitan.getApsolutnePutanjeFotografija());
    }

    @Test
    @DisplayName("ucitaj() nepostojećeg fajla vraća null umjesto izuzetka")
    void ucitajNepostojeciFajlVracaNull() {
        File nepostojeci = new File(privremeniDirektorijum.toFile(), "nema-ovakvog-fajla.ser");

        assertDoesNotThrow(() -> assertNull(Incident.ucitaj(nepostojeci)));
    }

    @Test
    @DisplayName("I7: podrazumijevani sacuvaj() bez argumenata piše u System.getProperty(\"user.home\")")
    void podrazumijevaniSacuvajPiseUUserHome() {
        Incident incident = napraviIncident();
        File fajl = null;
        try {
            fajl = incident.sacuvaj();

            assertNotNull(fajl);
            assertTrue(fajl.exists());
            assertEquals(new File(System.getProperty("user.home")), fajl.getParentFile(),
                    "Podrazumijevani sacuvaj() mora pisati direktno u user.home (I7), ne u radni direktorijum.");
        } finally {
            if (fajl != null) {
                assertTrue(fajl.delete() || !fajl.exists(),
                        "Čišćenje test-fajla iz stvarnog user.home direktorijuma nije uspjelo.");
            }
        }
    }
}
