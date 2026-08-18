package org.unibl.etf.pj2.luka.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.model.classes.Dok;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.KontejnerskiBrod;
import org.unibl.etf.pj2.luka.model.classes.PutnickiKruzer;
import org.unibl.etf.pj2.luka.model.classes.Tanker;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.model.interfaces.Carina;
import org.unibl.etf.pj2.luka.model.interfaces.ObalskaStraza;
import org.unibl.etf.pj2.luka.model.interfaces.SluzbenoPlovilo;
import org.unibl.etf.pj2.luka.model.interfaces.Vatrogasci;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GeneratorPlovila — slučajna generacija plovila (C2)")
class GeneratorPlovilaTest {

    private static final int UZORAK = 10_000;

    @Test
    @DisplayName("Udio komercijalnih plovila je ~90% na velikom uzorku")
    void udioKomercijalnihJeOko90Posto() {
        Random rnd = new Random(1);
        int komercijalnih = 0;

        for (int i = 0; i < UZORAK; i++) {
            Plovilo p = GeneratorPlovila.generisiSlucajno(rnd);
            if (!(p instanceof SluzbenoPlovilo)) {
                komercijalnih++;
            }
        }

        double udio = komercijalnih / (double) UZORAK;
        assertTrue(udio >= 0.88 && udio <= 0.92,
                "Udio komercijalnih plovila (" + udio + ") je van očekivanog opsega [0.88, 0.92].");
    }

    @Test
    @DisplayName("Svako generisano državno plovilo je dozvoljena kombinacija trupa i službe")
    void svakoDrzavnoPloviloJeDozvoljenaKombinacija() {
        Random rnd = new Random(2);

        for (int i = 0; i < UZORAK; i++) {
            Plovilo p = GeneratorPlovila.generisiSlucajno(rnd);

            if (p instanceof Vatrogasci) {
                assertTrue(p instanceof Tanker, "Vatrogasci smiju biti samo tanker: " + p.getClass());
                assertFalse(p instanceof ObalskaStraza || p instanceof Carina,
                        "Vatrogasno plovilo ne smije istovremeno biti i druga služba.");
            } else if (p instanceof ObalskaStraza) {
                assertTrue(p instanceof KontejnerskiBrod || p instanceof PutnickiKruzer || p instanceof Tanker,
                        "Obalska straža mora biti kontejnerski, kruzer ili tanker: " + p.getClass());
            } else if (p instanceof Carina) {
                assertTrue(p instanceof PutnickiKruzer || p instanceof Tanker,
                        "Carina mora biti kruzer ili tanker (nikad kontejnerski): " + p.getClass());
                assertFalse(p instanceof KontejnerskiBrod, "Ne postoji carinski kontejnerski brod.");
            }
        }
    }

    @Test
    @DisplayName("Nema duplih IMO brojeva na 10.000 generisanih plovila")
    void nemaDuplihImoBrojeva() {
        Random rnd = new Random(3);
        Set<String> imoBrojevi = new HashSet<>();

        for (int i = 0; i < UZORAK; i++) {
            Plovilo p = GeneratorPlovila.generisiSlucajno(rnd);
            assertTrue(imoBrojevi.add(p.getImoBroj()),
                    "Duplirani IMO broj: " + p.getImoBroj());
        }

        assertEquals(UZORAK, imoBrojevi.size());
    }

    @Test
    @DisplayName("IMO broj je sedmocifren")
    void imoBrojJeSedmocifren() {
        Random rnd = new Random(4);
        for (int i = 0; i < 500; i++) {
            String imo = GeneratorPlovila.generisiSlucajno(rnd).getImoBroj();
            assertEquals(7, imo.length(), "IMO broj '" + imo + "' nije sedmocifren.");
            assertTrue(imo.chars().allMatch(Character::isDigit), "IMO broj mora sadržati samo cifre.");
        }
    }

    @Test
    @DisplayName("Isti seed daje identičnu flotu (tip, naziv, prioritet) pri dva odvojena poziva")
    void istiSeedDajeIdenticnuFlotu() {
        int pocetniImo = 5_000_000;
        int pocetniNaziv = 1;

        GeneratorPlovila.resetujImoBrojacZaTest(pocetniImo);
        GeneratorPlovila.resetujNazivBrojacZaTest(pocetniNaziv);
        Random prvi = new Random(42);
        Plovilo[] flotaA = new Plovilo[200];
        for (int i = 0; i < flotaA.length; i++) {
            flotaA[i] = GeneratorPlovila.generisiSlucajno(prvi);
        }

        GeneratorPlovila.resetujImoBrojacZaTest(pocetniImo);
        GeneratorPlovila.resetujNazivBrojacZaTest(pocetniNaziv);
        Random drugi = new Random(42);
        Plovilo[] flotaB = new Plovilo[200];
        for (int i = 0; i < flotaB.length; i++) {
            flotaB[i] = GeneratorPlovila.generisiSlucajno(drugi);
        }

        for (int i = 0; i < flotaA.length; i++) {
            Plovilo a = flotaA[i];
            Plovilo b = flotaB[i];
            assertEquals(a.getClass(), b.getClass(), "Tip se razlikuje na poziciji " + i);
            assertEquals(a.getImoBroj(), b.getImoBroj(), "IMO se razlikuje na poziciji " + i);
            assertEquals(a.getNaziv(), b.getNaziv(), "Naziv se razlikuje na poziciji " + i);
            assertEquals(a.getPrioritet(), b.getPrioritet(), "Prioritet se razlikuje na poziciji " + i);
        }
    }

    @Test
    @DisplayName("Fotografija nikad nije null, ni za komercijalna ni za državna plovila")
    void fotografijaNikadNijeNull() {
        Random rnd = new Random(5);
        for (int i = 0; i < 1000; i++) {
            Plovilo p = GeneratorPlovila.generisiSlucajno(rnd);
            assertNotNull(p.getFotografija(), "Fotografija je null za " + p.getClass());
        }
    }

    @Test
    @DisplayName("Obalska straža uvijek dobija spisak potjernica, nikad null")
    void obalskaStrazaUvijekImaSpisakPotjera() {
        Random rnd = new Random(6);
        boolean pronadjenaObalskaStraza = false;

        for (int i = 0; i < UZORAK; i++) {
            Plovilo p = GeneratorPlovila.generisiSlucajno(rnd);
            if (p instanceof ObalskaStraza os) {
                pronadjenaObalskaStraza = true;
                assertNotNull(os.getSpisakPotjera(),
                        "Spisak potjera je null za generisano plovilo obalske straže: " + p.getClass());
            }
        }

        assertTrue(pronadjenaObalskaStraza,
                "Nijedno plovilo obalske straže nije generisano na uzorku od " + UZORAK + " — provjeri raspodjelu.");
    }


    @Test
    @DisplayName("Brojač IMO se pomjera iznad postojećih plovila u luci — nema kolizije sa deserijalizovanom lukom")
    void obezbjeduJedinstvenostImoIzbjegavaKolizijuSaPostojecomLukom() {
        // Simulira luku učitanu iz luka.ser: plovila već "sjede" na dokovima sa IMO brojevima
        // u istom opsegu koji bi brojač generatora inače dodijelio pri sljedećem pokretanju JVM-a.
        GeneratorPlovila.resetujImoBrojacZaTest(1_000_000);

        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        List<Dok> dokovi = t.getDokovi();

        Set<String> postojeciImo = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            String imo = String.valueOf(1_000_000 + i);
            dokovi.get(i).getLokacija().setTrenutnoPlovilo(TestFactory.kontejnerski(imo));
            postojeciImo.add(imo);
        }

        // Brojač generatora bi bez ovog poziva krenuo od 1_000_000 - direktno u koliziju.
        GeneratorPlovila.obezbijediJedinstvenostImoZa(luka);

        Random rnd = new Random(7);
        Set<String> sviImo = new HashSet<>(postojeciImo);
        for (int i = 0; i < 100; i++) {
            Plovilo p = GeneratorPlovila.generisiSlucajno(rnd);
            assertTrue(sviImo.add(p.getImoBroj()),
                    "IMO kolizija sa postojećom lukom: " + p.getImoBroj());
        }
    }

    @Test
    @DisplayName("O1: brojač IMO se pomjera iznad plovila zabilježenih u evidenciji ulaska, ne samo na dokovima")
    void obezbjeduJedinstvenostImoSkeniraIEvidencijuUlaska() {
        // Plovilo koje je već napustilo luku ostaje samo u evidenciji ulaska (F1) - nije više
        // ni na jednom doku. O1 iz ZAHTJEVI.md: bez ovog skeniranja, novo plovilo bi moglo dobiti
        // IMO otišlog plovila i naslijediti njegov stari vremenski pečat u evidenciji (S6 equals/hashCode).
        GeneratorPlovila.resetujImoBrojacZaTest(2_000_000);

        Luka luka = TestFactory.luka(1);
        Set<String> postojeciImo = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            String imo = String.valueOf(2_000_000 + i);
            luka.addToEvidencija(imo, java.time.LocalDateTime.now());
            postojeciImo.add(imo);
        }

        GeneratorPlovila.obezbijediJedinstvenostImoZa(luka);

        Random rnd = new Random(8);
        Set<String> sviImo = new HashSet<>(postojeciImo);
        for (int i = 0; i < 100; i++) {
            Plovilo p = GeneratorPlovila.generisiSlucajno(rnd);
            assertTrue(sviImo.add(p.getImoBroj()),
                    "IMO kolizija sa plovilom koje je već napustilo luku: " + p.getImoBroj());
        }
    }

    @Test
    @DisplayName("Harnes pripreme simulacije (PokretacSimulacije) poziva obezbijediJedinstvenostImoZa prije dopune — nema kolizije")
    void pripremaPocetnogStanjaIzbjegavaImoKolizijuSaZatecenomFlotom() {
        // Integracioni test na nivou C1/C3/C4 harnesa: bez poziva na obezbijediJedinstvenostImoZa
        // prije popunjavanja terminala (C4), generator bi vrlo vjerovatno dodijelio IMO koji se
        // poklapa sa nekim iz zatečene flote (C3), pošto oba kreću iz istog opsega brojača.
        GeneratorPlovila.resetujImoBrojacZaTest(3_000_000);

        Luka postojeca = TestFactory.luka(1);
        Terminal t0 = postojeca.getTerminali().get(0);
        Set<String> zatecenaImo = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            String imo = String.valueOf(3_000_000 + i);
            t0.getDokovi().get(i).getLokacija().setTrenutnoPlovilo(TestFactory.kontejnerski(imo));
            zatecenaImo.add(imo);
        }

        Luka nova = org.unibl.etf.pj2.luka.simulation.PokretacSimulacije
                .pripremiPocetnoStanje(postojeca, 1, 20, new Random(9));

        Set<String> vidjeniImo = new HashSet<>();
        for (Terminal t : nova.getTerminali()) {
            for (Dok d : t.getDokovi()) {
                Plovilo p = d.getLokacija().getTrenutnoPlovilo();
                if (p != null) {
                    assertTrue(vidjeniImo.add(p.getImoBroj()), "Duplirani IMO u novoj luci: " + p.getImoBroj());
                }
            }
        }

        assertTrue(vidjeniImoSadrziSve(vidjeniImo, zatecenaImo));
    }

    private static boolean vidjeniImoSadrziSve(Set<String> viddeni, Set<String> ocekivani) {
        return viddeni.containsAll(ocekivani);
    }
}
