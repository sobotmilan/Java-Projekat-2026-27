package org.unibl.etf.pj2.luka.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SpisakPotjeraUtil — čitanje spiska IMO brojeva potjernica (M6)")
class SpisakPotjeraUtilTest {

    private Path privremeniFajl;

    @BeforeEach
    void pripremi() {
        SpisakPotjeraUtil.resetujKes();
    }

    @AfterEach
    void ocisti() throws Exception {
        SpisakPotjeraUtil.resetujKes();
        if (privremeniFajl != null) {
            Files.deleteIfExists(privremeniFajl);
        }
    }

    private File napisi(String sadrzaj) throws Exception {
        privremeniFajl = Files.createTempFile("spisak-potjera-test-", ".txt");
        Files.writeString(privremeniFajl, sadrzaj);
        return privremeniFajl.toFile();
    }

    @Test
    @DisplayName("Ispravan fajl — svaki IMO na svojoj liniji se učitava")
    void ispravanFajl() throws Exception {
        File fajl = napisi("1234567\n7654321\n1112223\n");

        Set<String> spisak = SpisakPotjeraUtil.ucitaj(fajl);

        assertEquals(Set.of("1234567", "7654321", "1112223"), spisak);
    }

    @Test
    @DisplayName("Prazan fajl daje prazan skup")
    void prazanFajl() throws Exception {
        File fajl = napisi("");

        assertTrue(SpisakPotjeraUtil.ucitaj(fajl).isEmpty());
    }

    @Test
    @DisplayName("Nepostojeći fajl daje prazan skup, bez izuzetka")
    void nepostojeciFajl() {
        File fajl = new File("ne-postoji-spisak-potjera-" + System.nanoTime() + ".txt");

        Set<String> spisak = assertDoesNotThrow(() -> SpisakPotjeraUtil.ucitaj(fajl));

        assertTrue(spisak.isEmpty());
    }

    @Test
    @DisplayName("null fajl daje prazan skup, bez izuzetka")
    void nullFajl() {
        assertTrue(SpisakPotjeraUtil.ucitaj(null).isEmpty());
    }

    @Test
    @DisplayName("Komentari i prazne linije se preskaču")
    void komentariIPrazneLinijeSePreskacu() throws Exception {
        File fajl = napisi("""
                # potjernica avgust 2026
                1234567

                # jos jedan komentar
                7654321

                """);

        assertEquals(Set.of("1234567", "7654321"), SpisakPotjeraUtil.ucitaj(fajl));
    }

    @Test
    @DisplayName("Rezultat se kešira po putanji fajla — izmjena bez reseta ne mijenja rezultat")
    void rezultatSeKesira() throws Exception {
        File fajl = napisi("1234567\n");
        assertEquals(Set.of("1234567"), SpisakPotjeraUtil.ucitaj(fajl));

        Files.writeString(privremeniFajl, "7654321\n");
        assertEquals(Set.of("1234567"), SpisakPotjeraUtil.ucitaj(fajl),
                "Izmjena fajla bez resetovanja keša ne smije promijeniti rezultat.");

        SpisakPotjeraUtil.resetujKes();
        assertEquals(Set.of("7654321"), SpisakPotjeraUtil.ucitaj(fajl),
                "Nakon reseta, nova vrijednost se čita.");
    }

    @Test
    @DisplayName("Različiti fajlovi (različite putanje) se kširaju nezavisno")
    void razlicitiFajloviSeKesirajuNezavisno() throws Exception {
        File fajl1 = napisi("1234567\n");
        Path drugiPut = Files.createTempFile("spisak-potjera-test-2-", ".txt");
        try {
            Files.writeString(drugiPut, "9999999\n");

            assertEquals(Set.of("1234567"), SpisakPotjeraUtil.ucitaj(fajl1));
            assertEquals(Set.of("9999999"), SpisakPotjeraUtil.ucitaj(drugiPut.toFile()));
        } finally {
            Files.deleteIfExists(drugiPut);
        }
    }
}
