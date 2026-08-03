package org.unibl.etf.pj2.luka.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testovi čitanja konfiguracije iz {@code luka.properties}.
 *
 * <p>Svaki test piše svoj konfiguracioni fajl, pa se stvarni fajl privremeno
 * sklanja i vraća nakon izvršavanja.</p>
 */
@DisplayName("PropertiesUtil — konfiguracija luke")
class PropertiesUtilTest {

    private static final Path PROPS = Path.of(PropertiesUtil.DEFAULT_PATH);
    private static final Path BACKUP = Path.of(PropertiesUtil.DEFAULT_PATH + ".testbackup");

    @BeforeEach
    void sacuvaj() throws Exception {
        PropertiesUtil.resetujKes();
        try {
            if (Files.exists(PROPS)) {
                Files.move(PROPS, BACKUP, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (java.nio.file.FileSystemException fse) {
            // Zakljucan fajl ne smije oboriti test.
        }
    }

    @AfterEach
    void vrati() throws Exception {
        PropertiesUtil.resetujKes();
        try {
            Files.deleteIfExists(PROPS);
            if (Files.exists(BACKUP)) {
                Files.move(BACKUP, PROPS, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (java.nio.file.FileSystemException fse) {
            // Ciscenje nije kriticno.
        }
    }

    private static void napisiKonfiguraciju(String sadrzaj) throws Exception {
        Files.writeString(PROPS, sadrzaj);
        PropertiesUtil.resetujKes();
    }

    @Test
    @DisplayName("Ispravna vrijednost se čita iz fajla")
    void citaIspravnuVrijednost() throws Exception {
        napisiKonfiguraciju("broj.terminala=5");
        assertEquals(5, PropertiesUtil.getBrojTerminala());
    }

    @Test
    @DisplayName("Vrijednost sa suvišnim razmacima se ispravno parsira")
    void toleriseRazmake() throws Exception {
        napisiKonfiguraciju("broj.terminala=  7  ");
        assertEquals(7, PropertiesUtil.getBrojTerminala());
    }

    @Test
    @DisplayName("Kada fajl ne postoji, koristi se podrazumijevana vrijednost")
    void nepostojeciFajl() throws Exception {
        Files.deleteIfExists(PROPS);
        PropertiesUtil.resetujKes();

        assertEquals(PropertiesUtil.PODRAZUMIJEVANI_BROJ_TERMINALA, PropertiesUtil.getBrojTerminala());
    }

    @Test
    @DisplayName("Kada ključ nedostaje, koristi se podrazumijevana vrijednost")
    void nedostajeKljuc() throws Exception {
        napisiKonfiguraciju("neka.druga.opcija=42");
        assertEquals(PropertiesUtil.PODRAZUMIJEVANI_BROJ_TERMINALA, PropertiesUtil.getBrojTerminala());
    }

    @Test
    @DisplayName("Vrijednost koja nije broj ne ruši aplikaciju")
    void neispravanFormat() throws Exception {
        napisiKonfiguraciju("broj.terminala=tri");
        assertEquals(PropertiesUtil.PODRAZUMIJEVANI_BROJ_TERMINALA, PropertiesUtil.getBrojTerminala());
    }

    @Test
    @DisplayName("Prazna vrijednost ne ruši aplikaciju")
    void praznaVrijednost() throws Exception {
        napisiKonfiguraciju("broj.terminala=");
        assertEquals(PropertiesUtil.PODRAZUMIJEVANI_BROJ_TERMINALA, PropertiesUtil.getBrojTerminala());
    }

    @Test
    @DisplayName("Nula i negativne vrijednosti se odbacuju")
    void vrijednostIspodOpsega() throws Exception {
        napisiKonfiguraciju("broj.terminala=0");
        assertEquals(PropertiesUtil.PODRAZUMIJEVANI_BROJ_TERMINALA, PropertiesUtil.getBrojTerminala());

        napisiKonfiguraciju("broj.terminala=-4");
        assertEquals(PropertiesUtil.PODRAZUMIJEVANI_BROJ_TERMINALA, PropertiesUtil.getBrojTerminala());
    }

    @Test
    @DisplayName("Prevelike vrijednosti se odbacuju")
    void vrijednostIznadOpsega() throws Exception {
        napisiKonfiguraciju("broj.terminala=" + (PropertiesUtil.MAX_TERMINALA + 1));
        assertEquals(PropertiesUtil.PODRAZUMIJEVANI_BROJ_TERMINALA, PropertiesUtil.getBrojTerminala());
    }

    @Test
    @DisplayName("Granične vrijednosti opsega su prihvatljive")
    void granicneVrijednosti() throws Exception {
        napisiKonfiguraciju("broj.terminala=" + PropertiesUtil.MIN_TERMINALA);
        assertEquals(PropertiesUtil.MIN_TERMINALA, PropertiesUtil.getBrojTerminala());

        napisiKonfiguraciju("broj.terminala=" + PropertiesUtil.MAX_TERMINALA);
        assertEquals(PropertiesUtil.MAX_TERMINALA, PropertiesUtil.getBrojTerminala());
    }

    @Test
    @DisplayName("Konfiguracija se čita samo jednom i kešira")
    void vrijednostSeKesira() throws Exception {
        napisiKonfiguraciju("broj.terminala=4");
        assertEquals(4, PropertiesUtil.getBrojTerminala());

        // Izmjena fajla bez resetovanja keša ne smije promijeniti rezultat.
        Files.writeString(PROPS, "broj.terminala=9");
        assertEquals(4, PropertiesUtil.getBrojTerminala());

        // Nakon reseta, nova vrijednost se čita.
        PropertiesUtil.resetujKes();
        assertEquals(9, PropertiesUtil.getBrojTerminala());
    }
}
