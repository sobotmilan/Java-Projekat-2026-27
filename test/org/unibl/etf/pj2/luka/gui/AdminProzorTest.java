package org.unibl.etf.pj2.luka.gui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.testutil.TestFactory;
import org.unibl.etf.pj2.luka.util.SerializationUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AdminProzor — osvježavanje stanja nakon zatvaranja klijentskog prozora (bug: dvostruka naplata)")
class AdminProzorTest {

    private static final Path SER = Path.of("luka.ser");
    private static final Path BACKUP = Path.of("luka.ser.adminprozortestbackup");
    private boolean postojaoPrijeTesta;

    private AdminProzor admin;

    @BeforeEach
    void sacuvajSer() throws Exception {
        postojaoPrijeTesta = Files.exists(SER);
        if (postojaoPrijeTesta) {
            Files.move(SER, BACKUP, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.deleteIfExists(SER);
    }

    @AfterEach
    void vratiSer() throws Exception {
        if (admin != null) {
            admin.dispose();
        }
        Files.deleteIfExists(SER);
        if (postojaoPrijeTesta) {
            Files.move(BACKUP, SER, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void cekaj(BooleanSupplier uslov, long timeoutMs) throws InterruptedException {
        long krajnjeVrijeme = System.currentTimeMillis() + timeoutMs;
        while (!uslov.getAsBoolean() && System.currentTimeMillis() < krajnjeVrijeme) {
            Thread.sleep(50);
        }
    }

    @Test
    @DisplayName("Nakon zatvaranja klijentskog prozora, evidencija obrisana klijentom (E2) se ne vraća")
    void zatvaranjeKlijentaNeVracaObrisanuEvidenciju() throws Exception {
        // "Stara" (admin-ova zastarjela) luka: plovilo sa zapisom u evidenciji, kao da je
        // administrator ovo stanje učitao PRIJE nego što je simulacija uopšte pokrenuta.
        Luka staraLuka = TestFactory.luka(1);
        Terminal t = staraLuka.getTerminali().get(0);
        t.getDokovi().get(0).getLokacija().setTrenutnoPlovilo(TestFactory.kontejnerski("1"));
        staraLuka.addToEvidencija("1", LocalDateTime.now().minusHours(2));
        SerializationUtil.serijalizujStanjeLuke(staraLuka);

        admin = new AdminProzor();
        cekaj(() -> admin.getLukaZaTest() != null, 5_000);
        assertTrue(admin.getLukaZaTest().getEvidencijaUlaska().containsKey("1"),
                "Test pretpostavlja da admin počinje sa zastarjelim stanjem koje sadrži zapis.");

        KlijentskiProzor klijent = admin.napraviKlijentskiProzor();

        // "E2": klijent na kraju simulacije upisuje ISPRAVNO stanje — plovilo je otišlo,
        // obracunajIZabiljeziTaksu() je već obrisalo njegov zapis iz evidencije.
        Luka ispravnoStanje = TestFactory.luka(1);
        SerializationUtil.serijalizujStanjeLuke(ispravnoStanje);

        // windowClosed se diže samo za prozor koji je stvarno bio prikazan (setVisible(true)) prije
        // dispose() — provjereno posebnim probnim programom: dispose() na nikad-prikazanom JFrame-u
        // tiho ne radi ništa, nema WINDOW_CLOSED događaja. Zato ovdje mora ići kroz pravi ciklus.
        klijent.setVisible(true);
        klijent.dispose(); // trigeruje windowClosed -> AdminProzor.ucitajStanje()

        cekaj(() -> admin.getLukaZaTest() != staraLuka, 5_000);
        cekaj(() -> !admin.getLukaZaTest().getEvidencijaUlaska().containsKey("1"), 5_000);

        assertFalse(admin.getLukaZaTest().getEvidencijaUlaska().containsKey("1"),
                "Nakon osvježavanja, admin ne smije zadržati zapis koji je klijent obrisao — "
                        + "bez ispravke bi se sljedeći klik na 'Pokreni klijentsku aplikaciju' "
                        + "resurektovao ovaj zapis i doveo do apsurdne ponovne naplate.");
    }

    @Test
    @DisplayName("Nakon zatvaranja klijentskog prozora, AdminProzor ima isti broj terminala/plovila kao luka.ser na disku")
    void adminOdgovaraStanjuNaDisku() throws Exception {
        Luka staraLuka = TestFactory.luka(1);
        SerializationUtil.serijalizujStanjeLuke(staraLuka);

        admin = new AdminProzor();
        cekaj(() -> admin.getLukaZaTest() != null, 5_000);

        KlijentskiProzor klijent = admin.napraviKlijentskiProzor();

        // Klijentska simulacija je "otkrila" 3 terminala umjesto admin-ova 1 (npr. luka.properties
        // je pročitan sa drugim brojem terminala pri pripremi simulacije) — proizvoljno drugačije
        // stanje, samo da bude jasno raspoznatljivo od stare admin-ove kopije.
        Luka noviRezultat = TestFactory.luka(3);
        SerializationUtil.serijalizujStanjeLuke(noviRezultat);

        klijent.setVisible(true);
        klijent.dispose();

        cekaj(() -> admin.getLukaZaTest() != null && admin.getLukaZaTest().getTerminali().size() == 3, 5_000);

        Luka saDiska = SerializationUtil.ucitajStanjeLuke();
        assertNotNull(saDiska);
        assertEquals(saDiska.getTerminali().size(), admin.getLukaZaTest().getTerminali().size(),
                "AdminProzor mora odražavati tačno ono što je posljednje upisano u luka.ser.");
    }
}
