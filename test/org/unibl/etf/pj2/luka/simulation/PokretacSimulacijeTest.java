package org.unibl.etf.pj2.luka.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.model.classes.Dok;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PokretacSimulacije — priprema početnog stanja (C1/C3/C4/T1)")
class PokretacSimulacijeTest {

    private static int brojDokovanih(Luka luka) {
        int brojac = 0;
        for (Terminal t : luka.getTerminali()) {
            for (Dok d : t.getDokovi()) {
                if (!d.isSlobodan()) {
                    brojac++;
                }
            }
        }
        return brojac;
    }

    private static int brojDokovanihUTerminalu(Terminal t) {
        int brojac = 0;
        for (Dok d : t.getDokovi()) {
            if (!d.isSlobodan()) {
                brojac++;
            }
        }
        return brojac;
    }

    @Test
    @DisplayName("Broj terminala u novoj luci odgovara zadatom parametru (T1)")
    void brojTerminalaOdgovaraParametru() {
        Luka luka = PokretacSimulacije.pripremiPocetnoStanje(null, 5, 0, new Random(1));
        assertEquals(5, luka.getTerminali().size());
        for (int i = 0; i < 5; i++) {
            assertEquals(i, luka.getTerminali().get(i).getIdTerminala());
        }
    }

    @Test
    @DisplayName("Broj terminala manji od 1 baca IllegalArgumentException")
    void brojTerminalaIspodJedanBacaIzuzetak() {
        assertThrows(IllegalArgumentException.class,
                () -> PokretacSimulacije.pripremiPocetnoStanje(null, 0, 0, new Random(1)));
    }

    @Test
    @DisplayName("Negativan minimum po terminalu baca IllegalArgumentException")
    void negativanMinimumBacaIzuzetak() {
        assertThrows(IllegalArgumentException.class,
                () -> PokretacSimulacije.pripremiPocetnoStanje(null, 1, -1, new Random(1)));
    }

    @Test
    @DisplayName("C1/C4: svaki terminal se dopunjava tačno do zadatog minimuma kada nema zatečene flote")
    void dopunjavaSvakiTerminalDoMinimuma() {
        Luka luka = PokretacSimulacije.pripremiPocetnoStanje(null, 3, 7, new Random(2));

        for (Terminal t : luka.getTerminali()) {
            assertEquals(7, brojDokovanihUTerminalu(t),
                    "Terminal " + t.getIdTerminala() + " nije dopunjen do minimuma.");
        }
    }

    @Test
    @DisplayName("C4: minimum veći od kapaciteta terminala (30) se ograničava na kapacitet, ne ruši aplikaciju")
    void minimumVeciOdKapacitetaSeOgranicava() {
        Luka luka = PokretacSimulacije.pripremiPocetnoStanje(null, 1, 999, new Random(3));
        Terminal t = luka.getTerminali().get(0);
        assertEquals(30, brojDokovanihUTerminalu(t));
    }

    @Test
    @DisplayName("Minimum 0 bez zatečene flote ostavlja luku praznu")
    void minimumNulaOstavljaLukuPraznu() {
        Luka luka = PokretacSimulacije.pripremiPocetnoStanje(null, 2, 0, new Random(4));
        assertEquals(0, brojDokovanih(luka));
    }

    @Test
    @DisplayName("C3: zatečena flota iz prethodne luke se u cijelosti pojavljuje u novoj luci")
    void zatecenaFlotaSePojavljujeUNovojLuci() {
        Luka postojeca = TestFactory.luka(1);
        Terminal staraT = postojeca.getTerminali().get(0);
        Set<String> zatecenaImo = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            String imo = "OLD-" + i;
            staraT.getDokovi().get(i).getLokacija().setTrenutnoPlovilo(TestFactory.kontejnerski(imo));
            zatecenaImo.add(imo);
        }

        Luka nova = PokretacSimulacije.pripremiPocetnoStanje(postojeca, 2, 0, new Random(5));

        Set<String> imoUNovoj = new HashSet<>();
        for (Terminal t : nova.getTerminali()) {
            for (Dok d : t.getDokovi()) {
                Plovilo p = d.getLokacija().getTrenutnoPlovilo();
                if (p != null) {
                    imoUNovoj.add(p.getImoBroj());
                }
            }
        }

        assertTrue(imoUNovoj.containsAll(zatecenaImo),
                "Neka zatečena plovila nisu prenesena u novu luku: " + zatecenaImo);
        assertEquals(4, brojDokovanih(nova), "Ne smije biti više dokovanih plovila nego što ih je zatečeno.");
    }

    @Test
    @DisplayName("C3: zatečena flota se prenosi i kada se broj terminala između sesija smanjio")
    void zatecenaFlotaSePrenosiIKadSeBrojTerminalaSmanji() {
        Luka postojeca = TestFactory.luka(3);
        Set<String> zatecenaImo = new HashSet<>();
        int redni = 0;
        for (Terminal t : postojeca.getTerminali()) {
            String imo = "MULTI-" + redni++;
            t.getDokovi().get(0).getLokacija().setTrenutnoPlovilo(TestFactory.kontejnerski(imo));
            zatecenaImo.add(imo);
        }

        Luka nova = PokretacSimulacije.pripremiPocetnoStanje(postojeca, 1, 0, new Random(6));

        assertEquals(1, nova.getTerminali().size());
        assertEquals(3, brojDokovanih(nova),
                "Sva tri zatečena plovila moraju stati u jedan terminal (kapacitet 30 >> 3).");
    }

    @Test
    @DisplayName("C3 + C4 zajedno: zatečena flota se računa u minimum, ne dodaje se preko njega")
    void zatecenaFlotaSeRacunaUMinimum() {
        Luka postojeca = TestFactory.luka(1);
        Terminal staraT = postojeca.getTerminali().get(0);
        for (int i = 0; i < 3; i++) {
            staraT.getDokovi().get(i).getLokacija().setTrenutnoPlovilo(TestFactory.kontejnerski("PREV-" + i));
        }

        Luka nova = PokretacSimulacije.pripremiPocetnoStanje(postojeca, 1, 5, new Random(7));

        assertEquals(5, brojDokovanihUTerminalu(nova.getTerminali().get(0)),
                "3 zatečena + 2 dopunska = 5, ne 3 + 5 = 8.");
    }

    @Test
    @DisplayName("Prvo pokretanje (postojeca == null) ne ruši harnes i vrši samo dopunu do minimuma")
    void prvoPokretanjeBezPostojeceLuke() {
        Luka nova = PokretacSimulacije.pripremiPocetnoStanje(null, 2, 3, new Random(8));
        assertEquals(6, brojDokovanih(nova));
    }

    @Test
    @DisplayName("pokreniPrivezanaPlovila pokreće po jednu nit za svako dokovano plovilo, u stanju PRIVEZAN")
    void pokreniPrivezanaPlovilaPokreceNitiUParkiranomStanju() throws Exception {
        Luka luka = PokretacSimulacije.pripremiPocetnoStanje(null, 1, 4, new Random(9));

        List<BrodThread> niti = PokretacSimulacije.pokreniPrivezanaPlovila(luka);
        try {
            assertEquals(4, niti.size());

            for (BrodThread bt : niti) {
                long krajnjeVrijeme = System.currentTimeMillis() + 5_000;
                while (bt.getZadatak() != Zadatak.PRIVEZAN && System.currentTimeMillis() < krajnjeVrijeme) {
                    Thread.sleep(20);
                }
                assertEquals(Zadatak.PRIVEZAN, bt.getZadatak());
                assertTrue(bt.isPrivezan());
                assertTrue(luka.getAktivnaPlovila().contains(bt));
            }

            assertEquals(4, luka.getAktivnaPlovila().size());
        } finally {
            for (BrodThread bt : niti) {
                bt.zatraziNapustanje();
            }
        }
    }

    @Test
    @DisplayName("Plovilo pokrenuto preko harnesa uredno napušta terminal kada se to zatraži")
    void pokrenutoPloviloNapustaTerminalNaZahtjev() throws Exception {
        Luka luka = PokretacSimulacije.pripremiPocetnoStanje(null, 1, 1, new Random(10));
        Terminal t = luka.getTerminali().get(0);
        assertEquals(29, t.getBrojSlobodnihVezova());

        List<BrodThread> niti = PokretacSimulacije.pokreniPrivezanaPlovila(luka);
        BrodThread bt = niti.get(0);

        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            long krajnjeVrijeme = System.currentTimeMillis() + 5_000;
            while (bt.getZadatak() != Zadatak.PRIVEZAN && System.currentTimeMillis() < krajnjeVrijeme) {
                Thread.sleep(20);
            }
            assertEquals(Zadatak.PRIVEZAN, bt.getZadatak());

            bt.zatraziNapustanje();

            Future<?> cekanje = exec.submit(() -> {
                while (luka.getAktivnaPlovila().contains(bt)) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });
            cekanje.get(10, TimeUnit.SECONDS);

            assertEquals(30, t.getBrojSlobodnihVezova(), "Plovilo je trebalo osloboditi vez nakon napuštanja.");
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
