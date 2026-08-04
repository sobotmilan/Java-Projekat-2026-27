package org.unibl.etf.pj2.luka.util;

import org.junit.jupiter.api.*;
import org.unibl.etf.pj2.luka.model.classes.KontejnerskiBrod;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testovi obračuna lučkih taksi i CSV evidencije.
 *
 * <p>Cjenovnik iz specifikacije: 100 KM po satu zadržavanja, zadržavanje do 12 sati 1000 KM,
 * a do 24 sata 2000 KM. Profesor je pojasnio: "Do sat vremena je 100KM, a preko naredna tarifa itd..."</p>
 */
@DisplayName("PokretacIzvjestaja — lučke takse")
class PokretacIzvjestajaTest {

    private static final LocalDateTime ULAZAK = LocalDateTime.of(2026, 8, 3, 0, 0);
    private static final Path CSV = Path.of("takse.csv");
    private static final Path BACKUP = Path.of("takse.csv.testbackup");

    @BeforeEach
    void sacuvajCsv() throws Exception {
        if (Files.exists(CSV)) {
            Files.move(CSV, BACKUP, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @AfterEach
    void vratiCsv() throws Exception {
        Files.deleteIfExists(CSV);
        if (Files.exists(BACKUP)) {
            Files.move(BACKUP, CSV, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static double taksa(int sati) {
        return PokretacIzvjestaja.izracunajTaksuZaPlovilo(
                TestFactory.kontejnerski("1234567"), ULAZAK, ULAZAK.plusHours(sati));
    }

    /**
     * Broji logičke kolone u jednom CSV redu prema RFC 4180 — zarezi unutar navodnika
     * (npr. u imenu escape-ovanom zbog zareza) se ne računaju kao granica kolone.
     * Obično {@code String.split(",")} to ne zna, pa bi naivno prijavio previše kolona
     * čim je neko polje ispravno citirano.
     */
    private static int brojKolonaCsv(String red) {
        int kolone = 1;
        boolean uNavodnicima = false;
        for (int i = 0; i < red.length(); i++) {
            char c = red.charAt(i);
            if (c == '"') {
                uNavodnicima = !uNavodnicima;
            } else if (c == ',' && !uNavodnicima) {
                kolone++;
            }
        }
        return kolone;
    }

    @Test
    @DisplayName("Zadržavanje kraće od sata naplaćuje se kao jedan sat")
    void minimalnaTarifa() {
        double t = PokretacIzvjestaja.izracunajTaksuZaPlovilo(
                TestFactory.kontejnerski("1"), ULAZAK, ULAZAK.plusMinutes(20));
        assertEquals(100.0, t, 0.001);
    }

    @Test
    @DisplayName("Do 12 sati naplaćuje se 100 KM po satu, sa gornjom granicom 1000 KM")
    void tarifaDoDvanaestSati() {
        assertEquals(100.0, taksa(1), 0.001);
        assertEquals(300.0, taksa(3), 0.001);
        assertEquals(500.0, taksa(5), 0.001);
        assertEquals(900.0, taksa(9), 0.001);
        assertEquals(1000.0, taksa(10), 0.001);
        assertEquals(1000.0, taksa(12), 0.001);
    }

    @Test
    @DisplayName("Između 12 i 24 sata tarifa raste do gornje granice 2000 KM")
    void tarifaDoDvadesetCetiriSata() {
        assertEquals(1500.0, taksa(15), 0.001);
        assertEquals(1900.0, taksa(19), 0.001);
        assertEquals(2000.0, taksa(20), 0.001);
        assertEquals(2000.0, taksa(24), 0.001);
    }

    @Test
    @DisplayName("Preko 24 sata na 2000 KM se dodaje 100 KM po satu")
    void tarifaPrekoDvadesetCetiriSata() {
        assertEquals(2100.0, taksa(25), 0.001);
        assertEquals(2600.0, taksa(30), 0.001);
        assertEquals(4400.0, taksa(48), 0.001);
    }

    @Test
    @DisplayName("Taksa je monotono neopadajuća sa dužinom zadržavanja")
    void taksaJeMonotona() {
        double prethodna = 0.0;
        for (int sati = 0; sati <= 60; sati++) {
            double trenutna = taksa(sati);
            assertTrue(trenutna >= prethodna,
                    "Taksa je pala sa " + prethodna + " na " + trenutna + " pri " + sati + " sati.");
            prethodna = trenutna;
        }
    }

    // ------------------------------------------------------------------
    // BUCKET A — oslobađanje državnih plovila
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Obalska straža, carina i vatrogasci ne plaćaju taksu")
    void sluzbenaPlovilaNePlacaju() {
        LocalDateTime izlazak = ULAZAK.plusHours(50);

        assertEquals(0.0, PokretacIzvjestaja.izracunajTaksuZaPlovilo(TestFactory.tankerVatrogasci("1"), ULAZAK, izlazak), 0.001);
        assertEquals(0.0, PokretacIzvjestaja.izracunajTaksuZaPlovilo(TestFactory.tankerOS("2"), ULAZAK, izlazak), 0.001);
        assertEquals(0.0, PokretacIzvjestaja.izracunajTaksuZaPlovilo(TestFactory.tankerCarina("3"), ULAZAK, izlazak), 0.001);
        assertEquals(0.0, PokretacIzvjestaja.izracunajTaksuZaPlovilo(TestFactory.kruzerOS("4"), ULAZAK, izlazak), 0.001);
        assertEquals(0.0, PokretacIzvjestaja.izracunajTaksuZaPlovilo(TestFactory.kruzerCarina("5"), ULAZAK, izlazak), 0.001);
        assertEquals(0.0, PokretacIzvjestaja.izracunajTaksuZaPlovilo(TestFactory.kontejnerskiOS("6"), ULAZAK, izlazak), 0.001);
    }

    @Test
    @DisplayName("Komercijalna plovila svih tipova plaćaju taksu")
    void komercijalnaPlacaju() {
        LocalDateTime izlazak = ULAZAK.plusHours(5);

        assertEquals(500.0, PokretacIzvjestaja.izracunajTaksuZaPlovilo(TestFactory.kontejnerski("1"), ULAZAK, izlazak), 0.001);
        assertEquals(500.0, PokretacIzvjestaja.izracunajTaksuZaPlovilo(TestFactory.kruzer("2"), ULAZAK, izlazak), 0.001);
        assertEquals(500.0, PokretacIzvjestaja.izracunajTaksuZaPlovilo(TestFactory.tanker("3"), ULAZAK, izlazak), 0.001);
    }

    @Test
    @DisplayName("Prvi upis kreira CSV sa zaglavljem")
    void prviUpisKreiraZaglavlje() throws Exception {
        KontejnerskiBrod b = TestFactory.kontejnerski("1234567");
        PokretacIzvjestaja.evidentirajUCSV(b, ULAZAK, ULAZAK.plusHours(3), 300.0);

        assertTrue(Files.exists(CSV));
        List<String> linije = Files.readAllLines(CSV);

        assertEquals(2, linije.size(), "Očekuje se zaglavlje + jedan red.");
        assertTrue(linije.get(0).startsWith("IMO Broj,Naziv,Tip"));
        assertTrue(linije.get(1).contains("1234567"));
        assertTrue(linije.get(1).contains("KontejnerskiBrod"));
    }

    @Test
    @DisplayName("Naredni upisi se dodaju bez ponavljanja zaglavlja")
    void nakndniUpisiNePonavljajuZaglavlje() throws Exception {
        PokretacIzvjestaja.evidentirajUCSV(TestFactory.kontejnerski("1"), ULAZAK, ULAZAK.plusHours(1), 100.0);
        PokretacIzvjestaja.evidentirajUCSV(TestFactory.tanker("2"), ULAZAK, ULAZAK.plusHours(2), 200.0);
        PokretacIzvjestaja.evidentirajUCSV(TestFactory.kruzer("3"), ULAZAK, ULAZAK.plusHours(3), 300.0);

        List<String> linije = Files.readAllLines(CSV);
        assertEquals(4, linije.size());

        int zaglavlja = 0;
        for (String l : linije) {
            if (l.startsWith("IMO Broj,")) {
                zaglavlja++;
            }
        }
        assertEquals(1, zaglavlja, "Zaglavlje se ponovilo u sredini fajla.");
    }

    @Test
    @DisplayName("Svaki red CSV-a ima tačno 6 kolona")
    void csvImaIspravanBrojKolona() throws Exception {
        PokretacIzvjestaja.evidentirajUCSV(TestFactory.kontejnerski("1234567"), ULAZAK, ULAZAK.plusHours(3), 300.0);

        List<String> linije = Files.readAllLines(CSV);
        for (String l : linije) {
            assertEquals(6, brojKolonaCsv(l), "Neispravan broj kolona u redu: " + l);
        }
    }

    @Test
//    @Disabled("Odluka o zaokruživanju: Duration.toHours() reže naniže. Odluči i dokumentuj prije predaje.")
    @DisplayName("C: nepun sat se zaokružuje naviše (90 min = 2 sata = 200 KM)")
    void nepunSatSeZaokruzujeNavise() {
        double t = PokretacIzvjestaja.izracunajTaksuZaPlovilo(
                TestFactory.kontejnerski("1"), ULAZAK, ULAZAK.plusMinutes(90));
        assertEquals(200.0, t, 0.001);
    }

    @Test
    @DisplayName("Naziv plovila sa zarezom ne razbija CSV")
    void nazivSaZarezomNeRazbijaCsv() throws Exception {
        KontejnerskiBrod b = TestFactory.kontejnerski("1234567");
        b.setNaziv("Luka, Kraljica Mora");

        PokretacIzvjestaja.evidentirajUCSV(b, ULAZAK, ULAZAK.plusHours(1), 100.0);

        List<String> linije = Files.readAllLines(CSV);
        String red = linije.get(1);

        // Naivni String.split(",") ne poznaje RFC 4180 navodnike, pa bi i nakon ispravnog
        // escape-ovanja izbrojao 7 "kolona" jer zarez unutar navodnika ostaje u tekstu.
        // brojKolonaCsv() poštuje navodnike i broji stvarne logičke kolone.
        assertEquals(6, brojKolonaCsv(red),
                "Naziv sa zarezom mora biti u navodnicima ili escape-ovan — inače CSV ima 7 kolona.");
        assertTrue(red.contains("\"Luka, Kraljica Mora\""),
                "Naziv sa zarezom treba da bude pod navodnicima u CSV-u (RFC 4180).");
    }
}
