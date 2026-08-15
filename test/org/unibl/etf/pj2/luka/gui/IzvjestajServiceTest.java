package org.unibl.etf.pj2.luka.gui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IzvjestajService — preuzimanje CSV izvještaja taksi (F5)")
class IzvjestajServiceTest {

    private static final Path CSV = Path.of("takse.csv");
    private static final Path BACKUP = Path.of("takse.csv.izvjestajtestbackup");
    private boolean postojaoPrijeTesta;

    @BeforeEach
    void sacuvajCsv() throws IOException {
        postojaoPrijeTesta = Files.exists(CSV);
        if (postojaoPrijeTesta) {
            Files.move(CSV, BACKUP, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @AfterEach
    void vratiCsv() throws IOException {
        Files.deleteIfExists(CSV);
        if (postojaoPrijeTesta) {
            Files.move(BACKUP, CSV, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Test
    @DisplayName("izvjestajPostoji() vraća false kad CSV ne postoji")
    void izvjestajPostojiFalseKadNemaFajla() {
        assertFalse(IzvjestajService.izvjestajPostoji());
    }

    @Test
    @DisplayName("izvjestajPostoji() vraća true kad CSV postoji")
    void izvjestajPostojiTrueKadPostojiFajl() throws IOException {
        Files.writeString(CSV, "IMO Broj,Naziv,Tip,Vrijeme ulaska,Vrijeme izlaska,Iznos\n");
        assertTrue(IzvjestajService.izvjestajPostoji());
    }

    @Test
    @DisplayName("preuzmiIzvjestaj() kopira sadržaj na odredište")
    void preuzmiIzvjestajKopiraSadrzaj(@TempDir Path tempDir) throws IOException {
        Files.writeString(CSV, "IMO Broj,Naziv,Tip,Vrijeme ulaska,Vrijeme izlaska,Iznos\n"
                + "1234567,Test,Kont,x,y,100.00\n");

        File odrediste = tempDir.resolve("preuzeto.csv").toFile();
        IzvjestajService.preuzmiIzvjestaj(odrediste);

        assertTrue(odrediste.exists());
        List<String> linije = Files.readAllLines(odrediste.toPath());
        assertEquals(2, linije.size());
        assertTrue(linije.get(1).contains("1234567"));
    }

    @Test
    @DisplayName("preuzmiIzvjestaj() prepisuje postojeći fajl na odredištu")
    void preuzmiIzvjestajPrepisujePostojeceOdrediste(@TempDir Path tempDir) throws IOException {
        Files.writeString(CSV, "IMO Broj,Naziv,Tip,Vrijeme ulaska,Vrijeme izlaska,Iznos\n"
                + "7654321,Novi,Kont,x,y,200.00\n");

        File odrediste = tempDir.resolve("preuzeto.csv").toFile();
        Files.writeString(odrediste.toPath(), "staro,stanje,fajla");

        IzvjestajService.preuzmiIzvjestaj(odrediste);

        List<String> linije = Files.readAllLines(odrediste.toPath());
        assertTrue(linije.get(1).contains("7654321"));
        assertFalse(linije.get(0).contains("staro"));
    }
}
