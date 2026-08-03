package org.unibl.etf.pj2.luka.util;

import org.junit.jupiter.api.*;
import org.unibl.etf.pj2.luka.model.classes.*;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testovi serijalizacije i deserijalizacije stanja luke.
 *
 * <p>NAPOMENA: {@link SerializationUtil} koristi hardkodovanu putanju "luka.ser" u radnom
 * direktorijumu, pa test pravi rezervnu kopiju postojećeg fajla i vraća ga nakon izvršavanja.
 * Ovo je razlog zbog kojeg je poželjan refaktor R3 — metode koje primaju putanju kao parametar.</p>
 */
@DisplayName("SerializationUtil — čuvanje i učitavanje stanja luke")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SerializationUtilTest {

    private static final Path SER = Path.of("luka.ser");
    private static final Path BACKUP = Path.of("luka.ser.testbackup");

    @BeforeEach
    void sacuvajPostojeciFajl() throws Exception {
        System.gc();
        Thread.sleep(50);
        try {
            if (Files.exists(SER)) {
                Files.move(SER, BACKUP, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (java.nio.file.FileSystemException fse) {
            // Fajl je zakljucan zbog procurjelog handle-a — nastavi, test svakako pise preko njega.
        }
    }

    @AfterEach
    void vratiPostojeciFajl() throws Exception {
        try {
            Files.deleteIfExists(SER);
            if (Files.exists(BACKUP)) {
                Files.move(BACKUP, SER, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (java.nio.file.FileSystemException fse) {
            // Ciscenje nije kriticno za ispravnost testa.
        }
    }

    @Test
    @Order(1)
    @DisplayName("Kada fajl ne postoji, učitavanje vraća null")
    void nepostojeciFajlVracaNull() {
        assertFalse(Files.exists(SER));
        assertNull(SerializationUtil.ucitajStanjeLuke());
    }

    @Test
    @Order(2)
    @DisplayName("Prazna luka preživljava serijalizaciju")
    void prazanRoundTrip() {
        Luka original = TestFactory.luka(3);

        SerializationUtil.serijalizujStanjeLuke(original);
        assertTrue(Files.exists(SER), "Fajl luka.ser nije kreiran.");

        Luka ucitana = SerializationUtil.ucitajStanjeLuke();
        assertNotNull(ucitana);
        assertEquals(3, ucitana.getTerminali().size());

        for (Terminal t : ucitana.getTerminali()) {
            assertEquals(30, t.getBrojSlobodnihVezova());
            assertEquals(4, t.getMatrica().length);
            assertEquals(17, t.getMatrica()[0].length);
        }
    }

    @Test
    @Order(3)
    @DisplayName("Privezana plovila preživljavaju serijalizaciju sa svim atributima")
    void plovilaPrezivljavajuRoundTrip() {
        Luka original = TestFactory.luka(2);
        Terminal t0 = original.getTerminali().get(0);

        KontejnerskiBrod kont = TestFactory.kontejnerski("1111111");
        TankerVatrogasci vatr = TestFactory.tankerVatrogasci("2222222");
        vatr.setRotacija(true);

        t0.getDokovi().get(0).getLokacija().setTrenutnoPlovilo(kont);
        t0.getDokovi().get(1).getLokacija().setTrenutnoPlovilo(vatr);
        original.addToEvidencija("1111111", LocalDateTime.of(2026, 8, 3, 9, 0));

        SerializationUtil.serijalizujStanjeLuke(original);
        Luka ucitana = SerializationUtil.ucitajStanjeLuke();

        assertNotNull(ucitana);
        Terminal ut0 = ucitana.getTerminali().get(0);

        Plovilo p0 = ut0.getDokovi().get(0).getLokacija().getTrenutnoPlovilo();
        Plovilo p1 = ut0.getDokovi().get(1).getLokacija().getTrenutnoPlovilo();

        assertNotNull(p0);
        assertNotNull(p1);
        assertTrue(p0 instanceof KontejnerskiBrod);
        assertTrue(p1 instanceof TankerVatrogasci);

        assertEquals("1111111", p0.getImoBroj());
        assertEquals(1500, ((KontejnerskiBrod) p0).getKapacitetTEU());
        assertEquals(kont.getBrzina(), p0.getBrzina(), 0.000001, "Brzina se mora sačuvati, ne regenerisati.");

        assertTrue(((TankerVatrogasci) p1).isRotacija(), "Stanje rotacije mora preživjeti serijalizaciju.");
        assertEquals(1, p1.getPrioritet());

        assertEquals(28, ut0.getBrojSlobodnihVezova());
        assertEquals(LocalDateTime.of(2026, 8, 3, 9, 0), ucitana.getEvidencijaUlaska().get("1111111"));
    }

    @Test
    @Order(4)
    @DisplayName("Referentni identitet Dok <-> Polje preživljava serijalizaciju")
    void identitetDokaIPoljaPrezivljava() {
        // Java serijalizacija čuva identitet unutar istog toka. Ako se ovo ikad pokvari
        // (npr. writeObject/readObject override), zauzetost doka i matrice će se razići.
        Luka original = TestFactory.luka(1);
        Terminal t0 = original.getTerminali().get(0);
        t0.getDokovi().get(3).getLokacija().setTrenutnoPlovilo(TestFactory.tanker("3333333"));

        SerializationUtil.serijalizujStanjeLuke(original);
        Luka ucitana = SerializationUtil.ucitajStanjeLuke();
        assertNotNull(ucitana);

        Terminal ut = ucitana.getTerminali().get(0);
        Dok dok = ut.getDokovi().get(3);
        Polje izMatrice = ut.getMatrica()[dok.getLokacija().getX()][dok.getLokacija().getY()];

        assertSame(izMatrice, dok.getLokacija(),
                "Nakon deserijalizacije dok i matrica moraju dijeliti isti objekat Polja.");
        assertFalse(dok.isSlobodan());
        assertEquals(29, ut.getBrojSlobodnihVezova());
    }

    @Test
    @Order(5)
    @DisplayName("Fotografija (File referenca) preživljava serijalizaciju")
    void fotografijaPrezivljava() {
        Luka original = TestFactory.luka(1);
        original.getTerminali().get(0).getDokovi().get(0).getLokacija()
                .setTrenutnoPlovilo(TestFactory.kontejnerski("4444444"));

        SerializationUtil.serijalizujStanjeLuke(original);
        Luka ucitana = SerializationUtil.ucitajStanjeLuke();
        assertNotNull(ucitana);

        Plovilo p = ucitana.getTerminali().get(0).getDokovi().get(0).getLokacija().getTrenutnoPlovilo();
        assertNotNull(p.getFotografija());
        assertEquals(new File("test-foto.jpg"), p.getFotografija());
    }

    @Test
    @Order(6)
    @DisplayName("Oštećen fajl ne ruši aplikaciju, već vraća null i loguje grešku")
    void osteceniFajlVracaNull() throws Exception {
        Files.writeString(SER, "ovo definitivno nije serijalizovan objekat");

        assertDoesNotThrow(new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                assertNull(SerializationUtil.ucitajStanjeLuke(),
                        "Oštećen luka.ser mora rezultovati sa null, a ne izuzetkom.");
            }
        });
    }

    @Test
    @Order(7)
    @DisplayName("BUG: obalska straža gubi spisak potjernica pri deserijalizaciji ako fajl nije apsolutan")
    void spisakPotjeraPrezivljava() {
        Luka original = TestFactory.luka(1);
        TankerObalskaStraza os = TestFactory.tankerOS("5555555");
        original.getTerminali().get(0).getDokovi().get(0).getLokacija().setTrenutnoPlovilo(os);

        SerializationUtil.serijalizujStanjeLuke(original);
        Luka ucitana = SerializationUtil.ucitajStanjeLuke();
        assertNotNull(ucitana);

        Plovilo p = ucitana.getTerminali().get(0).getDokovi().get(0).getLokacija().getTrenutnoPlovilo();
        assertTrue(p instanceof TankerObalskaStraza);

        File spisak = ((TankerObalskaStraza) p).getSpisakPotjera();
        assertNotNull(spisak, "Spisak potjernica se izgubio pri deserijalizaciji.");
        assertEquals(TestFactory.SPISAK, spisak, "Putanja do spiska potjernica se promijenila pri round-tripu.");
    }
}
