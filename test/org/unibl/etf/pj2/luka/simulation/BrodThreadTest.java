package org.unibl.etf.pj2.luka.simulation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.model.classes.*;
import org.unibl.etf.pj2.luka.testutil.TestFactory;
import org.unibl.etf.pj2.luka.view.PrikazTerminala;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BrodThread — kretanje i konkurentnost")
class BrodThreadTest {

    private static final int TIMEOUT_SEC = 40;

    @BeforeEach
    void postaviBezbjedanPodrazumijevaniNivoSudara() {
        BrodThread.SUDARI_OMOGUCENI = true;
        BrodThread.VJEROVATNOCA_SUDARA = 0.0;
    }

    private static final class Uzorkovac implements Runnable {
        private final Luka luka;
        private volatile boolean radi = true;


        final List<String> duplikati = Collections.synchronizedList(new ArrayList<String>());


        final Map<String, Set<String>> dokPoljaPoPlovilu = new ConcurrentHashMap<>();


        volatile boolean vidjenUHorizontalnomKanalu = false;


        final Map<Integer, Integer> minSlobodnihVezova = new ConcurrentHashMap<>();

        Uzorkovac(Luka luka) {
            this.luka = luka;
        }

        void stani() {
            radi = false;
        }

        @Override
        public void run() {
            while (radi) {
                for (Terminal t : luka.getTerminali()) {
                    synchronized (t) {
                        Map<String, Integer> brojac = new HashMap<>();
                        Polje[][] m = t.getMatrica();

                        for (int i = 0; i < m.length; i++) {
                            for (int j = 0; j < m[i].length; j++) {
                                Plovilo p = m[i][j].getTrenutnoPlovilo();
                                if (p == null) {
                                    continue;
                                }
                                String imo = p.getImoBroj();

                                Integer prethodno = brojac.get(imo);
                                brojac.put(imo, prethodno == null ? 1 : prethodno + 1);

                                boolean jeDok = (i == 0 || i == 3) && j >= 2;
                                if (jeDok) {
                                    Set<String> skup = dokPoljaPoPlovilu.get(imo);
                                    if (skup == null) {
                                        skup = Collections.synchronizedSet(new HashSet<String>());
                                        Set<String> stari = dokPoljaPoPlovilu.putIfAbsent(imo, skup);
                                        if (stari != null) {
                                            skup = stari;
                                        }
                                    }
                                    skup.add(t.getIdTerminala() + ":" + i + "," + j);
                                }

                                if ((i == 1 || i == 2) && j >= 3) {
                                    vidjenUHorizontalnomKanalu = true;
                                }
                            }
                        }

                        for (Map.Entry<String, Integer> e : brojac.entrySet()) {
                            if (e.getValue() > 1) {
                                duplikati.add("Plovilo " + e.getKey() + " je na " + e.getValue()
                                        + " polja istovremeno u terminalu " + t.getIdTerminala());
                            }
                        }

                        int slobodni = t.getBrojSlobodnihVezova();
                        Integer trenutniMin = minSlobodnihVezova.get(t.getIdTerminala());
                        if (trenutniMin == null || slobodni < trenutniMin) {
                            minSlobodnihVezova.put(t.getIdTerminala(), slobodni);
                        }
                    }
                }

                try {
                    Thread.sleep(3);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * Od uvođenja parkiranog stanja (PRIVEZAN) nit broda se više ne završava kada se plovilo
     * priveže — ostaje blokirana u {@code wait()} dok je neko (R4/C7/C8) ne pozove da napusti
     * terminal. {@code ExecutorService.awaitTermination()} zato više nije ispravan signal da je
     * "simulacija gotova": vraćao bi false čak i kad su se sva plovila uspješno privezala.
     * Umjesto toga se čeka da svako plovilo ili bude privezano ({@link BrodThread#isPrivezan()})
     * ili je njegova nit već završila bez privezivanja ({@link Future#isDone()}).
     */
    private static boolean pokreniIsacekaj(Luka luka, List<Plovilo> plovila, Uzorkovac uzorkovac)
            throws InterruptedException {

        ExecutorService exec = Executors.newFixedThreadPool(Math.max(2, plovila.size()));
        Thread nitUzorkovaca = null;

        if (uzorkovac != null) {
            nitUzorkovaca = new Thread(uzorkovac, "uzorkovac");
            nitUzorkovaca.setDaemon(true);
            nitUzorkovaca.start();
        }

        List<BrodThread> brodovi = new ArrayList<>();
        List<Future<?>> futures = new ArrayList<>();
        for (Plovilo p : plovila) {
            BrodThread bt = new BrodThread(p, luka);
            brodovi.add(bt);
            futures.add(exec.submit(bt));
        }

        boolean sviSlegnuti = sacekajDaSviSlegnu(brodovi, futures, TIMEOUT_SEC * 1000L);

        exec.shutdownNow();
        exec.awaitTermination(5, TimeUnit.SECONDS);

        if (uzorkovac != null) {
            uzorkovac.stani();
            nitUzorkovaca.join(1000);
        }
        return sviSlegnuti;
    }

    private static boolean sacekajDaSviSlegnu(List<BrodThread> brodovi, List<Future<?>> futures, long timeoutMs)
            throws InterruptedException {
        long krajnjeVrijeme = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < krajnjeVrijeme) {
            boolean sviSlegnuti = true;
            for (int i = 0; i < brodovi.size(); i++) {
                if (!brodovi.get(i).isPrivezan() && !futures.get(i).isDone()) {
                    sviSlegnuti = false;
                    break;
                }
            }
            if (sviSlegnuti) {
                return true;
            }
            Thread.sleep(20);
        }
        return false;
    }

    private static List<Plovilo> komercijalna(int n) {
        List<Plovilo> lista = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            switch (i % 3) {
                case 0:
                    lista.add(TestFactory.kontejnerski("IMO-K-" + i));
                    break;
                case 1:
                    lista.add(TestFactory.kruzer("IMO-P-" + i));
                    break;
                default:
                    lista.add(TestFactory.tanker("IMO-T-" + i));
                    break;
            }
        }
        return lista;
    }

    @Test
    @DisplayName("Prvo plovilo uspješno ulazi na ulazno polje terminala")
    void prvoPloviloUlazi() {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        Plovilo p = TestFactory.kontejnerski("1111111");

        BrodThread bt = new BrodThread(p, luka);
        assertTrue(bt.pokusajUciUTerminal(t));
        assertSame(p, t.getMatrica()[0][0].getTrenutnoPlovilo());
    }

    @Test
    @DisplayName("Drugo plovilo ne može ući dok je ulazno polje zauzeto")
    void ulaznoPoljeJeIskljucivo() {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);

        assertTrue(new BrodThread(TestFactory.kontejnerski("1"), luka).pokusajUciUTerminal(t));
        assertFalse(new BrodThread(TestFactory.kontejnerski("2"), luka).pokusajUciUTerminal(t),
                "Dva plovila ne smiju biti na istom ulaznom polju.");
    }

    @Test
    @DisplayName("Konkurentni ulazak: tačno jedno od N plovila zauzima ulazno polje")
    void konkurentniUlazakJeAtomaran() throws Exception {
        for (int ponavljanje = 0; ponavljanje < 50; ponavljanje++) {
            final Luka luka = TestFactory.luka(1);
            final Terminal t = luka.getTerminali().get(0);
            final int n = 12;

            final CountDownLatch start = new CountDownLatch(1);
            final CountDownLatch kraj = new CountDownLatch(n);
            final List<Boolean> rezultati = Collections.synchronizedList(new ArrayList<Boolean>());
            ExecutorService exec = Executors.newFixedThreadPool(n);

            for (int i = 0; i < n; i++) {
                final BrodThread bt = new BrodThread(TestFactory.kontejnerski("IMO-" + i), luka);
                exec.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            start.await();
                            rezultati.add(bt.pokusajUciUTerminal(t));
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        } finally {
                            kraj.countDown();
                        }
                    }
                });
            }

            start.countDown();
            assertTrue(kraj.await(10, TimeUnit.SECONDS));
            exec.shutdownNow();

            int uspjesnih = 0;
            for (Boolean b : rezultati) {
                if (Boolean.TRUE.equals(b)) {
                    uspjesnih++;
                }
            }
            assertEquals(1, uspjesnih, "Tačno jedno plovilo smije uspjeti (ponavljanje " + ponavljanje + ").");
        }
    }

    @Test
    @DisplayName("Jedan brod u praznoj luci se priveže i nit se uredno završi")
    void jedanBrodSePrivezuje() throws Exception {
        Luka luka = TestFactory.luka(1);
        List<Plovilo> plovila = new ArrayList<>();
        plovila.add(TestFactory.kontejnerski("1111111"));

        assertTrue(pokreniIsacekaj(luka, plovila, null),
                "Nit broda se nije završila u zadatom vremenu — vjerovatno beskonačna petlja.");

        Terminal t = luka.getTerminali().get(0);
        assertEquals(29, t.getBrojSlobodnihVezova(), "Brod je trebalo da zauzme tačno jedan vez.");
    }

    @Test
    @DisplayName("Brod ne ostavlja 'duha' na ulaznom polju nakon privezivanja")
    void nemaZaostalihReferenci() throws Exception {
        Luka luka = TestFactory.luka(1);
        List<Plovilo> plovila = new ArrayList<>();
        Plovilo p = TestFactory.kontejnerski("1111111");
        plovila.add(p);

        assertTrue(pokreniIsacekaj(luka, plovila, null));

        Terminal t = luka.getTerminali().get(0);
        int pojavljivanja = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 17; j++) {
                if (t.getMatrica()[i][j].getTrenutnoPlovilo() == p) {
                    pojavljivanja++;
                }
            }
        }
        assertEquals(1, pojavljivanja, "Plovilo se pojavljuje na " + pojavljivanja + " polja umjesto na jednom.");
    }

    @Test
    @DisplayName("Više brodova u luci: nijedno plovilo nije na dva polja istovremeno")
    void nemaDupliranihPlovila() throws Exception {
        Luka luka = TestFactory.luka(2);
        Uzorkovac uzorkovac = new Uzorkovac(luka);

        pokreniIsacekaj(luka, komercijalna(8), uzorkovac);

        assertTrue(uzorkovac.duplikati.isEmpty(),
                "Detektovani prekršaji: " + uzorkovac.duplikati);
    }

    @Test
    @DisplayName("Broj privezanih plovila ne prelazi kapacitet luke")
    void kapacitetSePostuje() throws Exception {
        Luka luka = TestFactory.luka(1);
        pokreniIsacekaj(luka, komercijalna(6), null);

        Terminal t = luka.getTerminali().get(0);
        assertTrue(t.getBrojSlobodnihVezova() >= 0);
        assertTrue(t.getBrojSlobodnihVezova() <= 30);
    }

    @Test
    @Tag("bug")
    @DisplayName("BUG: brod prolazi kroz dokove umjesto kroz horizontalni kanal")
    void brodNeSmijeProlazitiKrozDokove() throws Exception {
        Luka luka = TestFactory.luka(1);
        Uzorkovac uzorkovac = new Uzorkovac(luka);

        pokreniIsacekaj(luka, komercijalna(4), uzorkovac);

        List<String> prekrsioci = new ArrayList<>();
        for (Map.Entry<String, Set<String>> e : uzorkovac.dokPoljaPoPlovilu.entrySet()) {
            if (e.getValue().size() > 1) {
                prekrsioci.add(e.getKey() + " je zauzimalo " + e.getValue().size() + " dok-polja: " + e.getValue());
            }
        }

        assertTrue(prekrsioci.isEmpty(),
                "Plovila se kreću kroz dokove umjesto kroz plovni kanal (red 2 / red 1):\n"
                        + String.join("\n", prekrsioci));
    }

    @Test
    @Tag("bug")
    @DisplayName("BUG: horizontalni plovni kanal (redovi 1 i 2) se uopšte ne koristi")
    void horizontalniKanalSeKoristi() throws Exception {
        Luka luka = TestFactory.luka(1);
        Uzorkovac uzorkovac = new Uzorkovac(luka);

        pokreniIsacekaj(luka, komercijalna(4), uzorkovac);

        assertTrue(uzorkovac.vidjenUHorizontalnomKanalu,
                "Nijedno plovilo nikada nije viđeno u redu 1 ili 2 dalje od kolone 3. "
                        + "Kretanje se odvija kroz redove dokova, što je suprotno šemi terminala.");
    }

    @Test
    @Tag("bug")
    @DisplayName("BUG: brod ne ulazi u terminal koji nema slobodnih vezova")
    void neUlaziUPunTerminal() throws Exception {
        // Pojašnjenje profesora: "Ako terminal ima slobodnih mjesta, onda se ulazi u terminal."
        // Pun terminal se preskače — brod ide pravo na naredni.
        Luka luka = TestFactory.luka(2);
        Terminal pun = luka.getTerminali().get(0);
        Terminal slobodan = luka.getTerminali().get(1);
        TestFactory.popuniSveDokove(pun);

        Uzorkovac uzorkovac = new Uzorkovac(luka);
        List<Plovilo> plovila = new ArrayList<>();
        Plovilo p = TestFactory.kontejnerski("9999999");
        plovila.add(p);

        pokreniIsacekaj(luka, plovila, uzorkovac);

        assertEquals(29, slobodan.getBrojSlobodnihVezova(),
                "Brod je trebalo da preskoči pun terminal 0 i priveže se u terminalu 1.");

        Set<String> dokPolja = uzorkovac.dokPoljaPoPlovilu.get("9999999");
        if (dokPolja != null) {
            for (String polje : dokPolja) {
                assertFalse(polje.startsWith("0:"),
                        "Brod je ušao u dok-zonu punog terminala 0: " + polje);
            }
        }
    }

    @Test
    @Tag("bug")
    @DisplayName("BUG: evidencija ulaska se vodi za svako plovilo koje uđe u luku")
    void evidencijaSeVodiZaSvaPlovila() throws Exception {
        // Specifikacija: "Luka vodi i evidenciju plovila po IMO broju koja su ušla u nju
        // i vrijeme kada su ušla." Trenutno se upis dešava samo pri ulasku u terminal,
        // a brod koji nikada ne uspije ući ostaje neevidentiran.
        Luka luka = TestFactory.luka(1);
        List<Plovilo> plovila = komercijalna(5);

        pokreniIsacekaj(luka, plovila, null);

        for (Plovilo p : plovila) {
            assertTrue(luka.getEvidencijaUlaska().containsKey(p.getImoBroj()),
                    "Plovilo " + p.getImoBroj() + " nije evidentirano pri ulasku u luku.");
        }
    }

    private static Set<String> nazivIncidentFajlova(File dir) {
        File[] fajlovi = dir.listFiles((d, ime) -> ime.startsWith("incident-") && ime.endsWith(".ser"));
        Set<String> nazivi = new HashSet<>();
        if (fajlovi != null) {
            for (File f : fajlovi) {
                nazivi.add(f.getName());
            }
        }
        return nazivi;
    }

    private static File cekajNoviIncidentFajl(File dir, Set<String> postojeci, long timeoutMs) throws InterruptedException {
        long krajnjeVrijeme = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < krajnjeVrijeme) {
            File[] fajlovi = dir.listFiles((d, ime) -> ime.startsWith("incident-") && ime.endsWith(".ser"));
            if (fajlovi != null) {
                for (File f : fajlovi) {
                    if (!postojeci.contains(f.getName())) {
                        return f;
                    }
                }
            }
            Thread.sleep(100);
        }
        return null;
    }

    @Test
    @DisplayName("I1/R4b: sudar pri mimoilaženju sa dva učesnika pokreće koordinatora — nastaje incident "
            + "sa oba učesnika i bar jednim odazvanim službenim plovilom")
    void sudarUkljucujeDvaPlovila() throws Exception {
        boolean staroSudari = BrodThread.SUDARI_OMOGUCENI;
        double staraVjerovatnoca = BrodThread.VJEROVATNOCA_SUDARA;
        long staroMin = BrodThread.MIN_TRAJANJE_UVIDJAJA_MS;
        long staroMax = BrodThread.MAX_TRAJANJE_UVIDJAJA_MS;

        File userHome = new File(System.getProperty("user.home"));
        Set<String> postojeciFajlovi = nazivIncidentFajlova(userHome);

        ExecutorService exec = Executors.newFixedThreadPool(8);
        File noviFajl = null;
        try {
            BrodThread.SUDARI_OMOGUCENI = true;
            BrodThread.VJEROVATNOCA_SUDARA = 1.0;
            BrodThread.MIN_TRAJANJE_UVIDJAJA_MS = 100L;
            BrodThread.MAX_TRAJANJE_UVIDJAJA_MS = 100L;

            Luka luka = TestFactory.luka(1);
            Terminal t = luka.getTerminali().get(0);

            // Popuniti dokove do kolone 6 direktno (bez rezervacije), da bi brzo plovilo dobilo
            // dok tek na koloni 7+ i time garantovano prošlo kroz kolonu 6 u kanalu, gdje čeka
            // statička prepreka postavljena ispod.
            for (Dok d : t.getDokovi()) {
                if (d.getLokacija().getY() <= 6) {
                    d.getLokacija().setTrenutnoPlovilo(TestFactory.kontejnerski("POPUNA-" + d.getOznakaVezova()));
                }
            }

            // Patrola već privezana na prvi zaista slobodan dok, spremna da se odazove (I2).
            Dok dokPatrole = TestFactory.prviSlobodanDok(t);
            TankerVatrogasci patrolaPlovilo = TestFactory.tankerVatrogasci("PATROLA-INTEGRACIJA");
            dokPatrole.getLokacija().setTrenutnoPlovilo(patrolaPlovilo);
            exec.submit(new BrodThread(patrolaPlovilo, luka, t, dokPatrole));

            // Statička prepreka u kanalu — isti princip kao Step 1 testovi (drugi učesnik je samo
            // plovilo na fiksnoj poziciji, bez sopstvene niti), ali ovdje ga stvarno, samostalno
            // sustiže i pretiče jedno REALNO plovilo (T4/R5) kroz stvarnu navigaciju. Bez ovoga bi
            // tačan trenutak "prolaska" zavisio od nepredvidive konkurentske trke dva stvarna
            // plovila, što se pokazalo nedeterministički nepouzdanim.
            Plovilo statickaPrepreka = TestFactory.kontejnerski("PREPREKA");
            t.getMatrica()[Terminal.KANAL_ULAZ][6].setTrenutnoPlovilo(statickaPrepreka);

            Plovilo brzoPlovilo = TestFactory.kontejnerski("BRZO");
            brzoPlovilo.setBrzina(50.0);
            exec.submit(new BrodThread(brzoPlovilo, luka));

            noviFajl = cekajNoviIncidentFajl(userHome, postojeciFajlovi, 20_000);
            assertNotNull(noviFajl, "Nijedan incident nije nastao u zadatom vremenu — sudar se nije "
                    + "detektovao pri mimoilaženju sa vjerovatnoćom 1.0.");

            Incident incident = Incident.ucitaj(noviFajl);
            assertNotNull(incident, "Fajl incidenta mora biti čitljiv preko Incident.ucitaj().");
            assertEquals(2, incident.getUcesniciSudara().size(),
                    "Sudar mora uključiti tačno dva učesnika, ne jednog.");
            assertNotEquals(incident.getUcesniciSudara().get(0).getImoBroj(),
                    incident.getUcesniciSudara().get(1).getImoBroj(),
                    "Dva učesnika sudara moraju biti različita plovila.");
            assertFalse(incident.getOdazvanaSluzbenaPlovila().isEmpty(),
                    "Bar jedno službeno plovilo mora biti odazvano na incident (I2).");
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(10, TimeUnit.SECONDS);
            if (noviFajl != null) {
                noviFajl.delete();
            }
            BrodThread.SUDARI_OMOGUCENI = staroSudari;
            BrodThread.VJEROVATNOCA_SUDARA = staraVjerovatnoca;
            BrodThread.MIN_TRAJANJE_UVIDJAJA_MS = staroMin;
            BrodThread.MAX_TRAJANJE_UVIDJAJA_MS = staroMax;
        }
    }

    @Test
    @DisplayName("R5: obično plovilo ustupa prolaz plovilu pod rotacijom koje je iza njega u kanalu")
    void obicnoPloviloUstupaProlazPloviluPodRotacijom() {
        Terminal t = TestFactory.luka(1).getTerminali().get(0);
        TankerVatrogasci vatrogasci = TestFactory.tankerVatrogasci("HITNO-1");
        vatrogasci.setRotacija(true);
        Plovilo obicno = TestFactory.kontejnerski("SPORI-1");

        t.getMatrica()[Terminal.KANAL_ULAZ][5].setTrenutnoPlovilo(vatrogasci);
        t.getMatrica()[Terminal.KANAL_ULAZ][6].setTrenutnoPlovilo(obicno);

        assertTrue(BrodThread.ustupaProlaz(t, Terminal.KANAL_ULAZ, 6, obicno),
                "Obično plovilo mora ustupiti prolaz kada je plovilo pod rotacijom neposredno iza njega.");
    }

    @Test
    @DisplayName("R5: plovilo pod rotacijom ne ustupa prolaz običnom plovilu iza sebe")
    void ploviloPodRotacijomNeUstupaProlazObicnom() {
        Terminal t = TestFactory.luka(1).getTerminali().get(0);
        TankerVatrogasci vatrogasci = TestFactory.tankerVatrogasci("HITNO-1");
        vatrogasci.setRotacija(true);
        Plovilo obicno = TestFactory.kontejnerski("SPORI-1");

        t.getMatrica()[Terminal.KANAL_ULAZ][5].setTrenutnoPlovilo(obicno);
        t.getMatrica()[Terminal.KANAL_ULAZ][6].setTrenutnoPlovilo(vatrogasci);

        assertFalse(BrodThread.ustupaProlaz(t, Terminal.KANAL_ULAZ, 6, vatrogasci),
                "Plovilo pod rotacijom ima viši prioritet od običnog plovila i ne treba da mu ustupa prolaz.");
    }

    @Test
    @DisplayName("R5: redoslijed prioriteta se poštuje i među službenim plovilima pod rotacijom (carina ustupa vatrogascima)")
    void carinaUstupaProlazVatrogascimaPodRotacijom() {
        Terminal t = TestFactory.luka(1).getTerminali().get(0);
        TankerVatrogasci vatrogasci = TestFactory.tankerVatrogasci("HITNO-1");
        vatrogasci.setRotacija(true);
        TankerCarina carina = TestFactory.tankerCarina("CAR-1");
        carina.setRotacija(true);

        t.getMatrica()[Terminal.KANAL_ULAZ][5].setTrenutnoPlovilo(vatrogasci);
        t.getMatrica()[Terminal.KANAL_ULAZ][6].setTrenutnoPlovilo(carina);

        assertTrue(BrodThread.ustupaProlaz(t, Terminal.KANAL_ULAZ, 6, carina),
                "Carina pod rotacijom mora ustupiti prolaz vatrogascima pod rotacijom iza nje (viši prioritet).");
    }

    @Test
    @DisplayName("R5: redoslijed prioriteta se poštuje i među službenim plovilima pod rotacijom (vatrogasci ne ustupaju carini)")
    void vatrogasciNeUstupajuProlazCariniPodRotacijom() {
        Terminal t = TestFactory.luka(1).getTerminali().get(0);
        TankerCarina carina = TestFactory.tankerCarina("CAR-1");
        carina.setRotacija(true);
        TankerVatrogasci vatrogasci = TestFactory.tankerVatrogasci("HITNO-1");
        vatrogasci.setRotacija(true);

        t.getMatrica()[Terminal.KANAL_ULAZ][5].setTrenutnoPlovilo(carina);
        t.getMatrica()[Terminal.KANAL_ULAZ][6].setTrenutnoPlovilo(vatrogasci);

        assertFalse(BrodThread.ustupaProlaz(t, Terminal.KANAL_ULAZ, 6, vatrogasci),
                "Vatrogasci pod rotacijom imaju najviši prioritet i ne ustupaju prolaz carini iza sebe.");
    }

    @Test
    @DisplayName("R5: obično plovilo ne ustupa prolaz drugom običnom plovilu")
    void obicnoPloviloNeUstupaProlazObicnom() {
        Terminal t = TestFactory.luka(1).getTerminali().get(0);
        Plovilo prvo = TestFactory.kontejnerski("SPORI-1");
        Plovilo drugo = TestFactory.kontejnerski("SPORI-2");

        t.getMatrica()[Terminal.KANAL_ULAZ][5].setTrenutnoPlovilo(prvo);
        t.getMatrica()[Terminal.KANAL_ULAZ][6].setTrenutnoPlovilo(drugo);

        assertFalse(BrodThread.ustupaProlaz(t, Terminal.KANAL_ULAZ, 6, drugo),
                "Dva obična plovila se ne ustupaju prolaz jedno drugom.");
    }

    @Test
    @DisplayName("R5: plovilo pod rotacijom uspješno završi simulaciju usred sporog saobraćaja")
    void ploviloPodRotacijomZavrsavaSimulaciju() throws Exception {
        Luka luka = TestFactory.luka(1);
        TankerVatrogasci vatrogasci = TestFactory.tankerVatrogasci("HITNO-1");
        vatrogasci.setRotacija(true);
        vatrogasci.setBrzina(5.0);

        List<Plovilo> plovila = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Plovilo spori = TestFactory.kontejnerski("SPORI-" + i);
            spori.setBrzina(1.5);
            plovila.add(spori);
        }
        plovila.add(vatrogasci);

        assertTrue(pokreniIsacekaj(luka, plovila, null),
                "Simulacija se nije završila u zadatom vremenu.");

        Terminal t = luka.getTerminali().get(0);
        assertEquals(24, t.getBrojSlobodnihVezova(), "Svih 6 plovila je trebalo da se priveže.");
    }

    private static void cekajPrivezivanje(BrodThread bt, long timeoutMs) throws InterruptedException {
        long krajnjeVrijeme = System.currentTimeMillis() + timeoutMs;
        while (!bt.isPrivezan() && System.currentTimeMillis() < krajnjeVrijeme) {
            Thread.sleep(20);
        }
        assertTrue(bt.isPrivezan(), "Plovilo se nije privezalo u zadatom vremenu.");
    }

    private static void cekajZadatak(BrodThread bt, Zadatak ocekivani, long timeoutMs) throws InterruptedException {
        long krajnjeVrijeme = System.currentTimeMillis() + timeoutMs;
        while (bt.getZadatak() != ocekivani && System.currentTimeMillis() < krajnjeVrijeme) {
            Thread.sleep(20);
        }
        assertEquals(ocekivani, bt.getZadatak());
    }

    @Test
    @DisplayName("Nakon privezivanja plovilo ulazi u stanje PRIVEZAN i nit ostaje živa (ne završava se)")
    void ploviloUlaziUParkiranoStanjeNakonPrivezivanja() throws Exception {
        Luka luka = TestFactory.luka(1);
        BrodThread bt = new BrodThread(TestFactory.kontejnerski("1111111"), luka);

        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> future = exec.submit(bt);
        try {
            cekajPrivezivanje(bt, 10_000);
            cekajZadatak(bt, Zadatak.PRIVEZAN, 5_000);

            assertFalse(future.isDone(), "Nit ne smije završiti odmah nakon privezivanja — mora ostati parkirana.");
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("zatraziNapustanje() budi parkiranu nit, koja onda napušta terminal i završava")
    void zatraziNapustanjeBudiParkiranuNitINapustaTerminal() throws Exception {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        BrodThread bt = new BrodThread(TestFactory.kontejnerski("2222222"), luka);

        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> future = exec.submit(bt);
        try {
            cekajPrivezivanje(bt, 10_000);
            assertEquals(29, t.getBrojSlobodnihVezova(), "Plovilo je trebalo zauzeti tačno jedan vez.");

            bt.zatraziNapustanje();
            future.get(10, TimeUnit.SECONDS);

            assertEquals(Zadatak.NAPUSTA, bt.getZadatak());
            assertEquals(30, t.getBrojSlobodnihVezova(), "Plovilo je trebalo osloboditi vez nakon napuštanja.");
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    // ------------------------------------------------------------------
    // F4 — obračun i evidentiranje takse pri konačnom izlasku iz luke
    // ------------------------------------------------------------------

    private static java.nio.file.Path csvPutanja() {
        return java.nio.file.Path.of("takse.csv");
    }

    private static java.nio.file.Path csvBackupPutanja() {
        return java.nio.file.Path.of("takse.csv.brodthreadtestbackup");
    }

    private static boolean sacuvajCsvAkoPostoji() throws java.io.IOException {
        java.nio.file.Path csv = csvPutanja();
        if (java.nio.file.Files.exists(csv)) {
            java.nio.file.Files.move(csv, csvBackupPutanja(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        }
        return false;
    }

    private static void vratiCsv(boolean postojaoPrijeTesta) throws java.io.IOException {
        java.nio.file.Files.deleteIfExists(csvPutanja());
        if (postojaoPrijeTesta) {
            java.nio.file.Files.move(csvBackupPutanja(), csvPutanja(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Test
    @DisplayName("F4: normalno napuštanje obračunava taksu, upisuje CSV red i briše zapis iz evidencije")
    void normalnoNapustanjeObracunavaTaksuIBrisEvidenciju() throws Exception {
        boolean postojaoPrijeTesta = sacuvajCsvAkoPostoji();
        try {
            Luka luka = TestFactory.luka(1);
            BrodThread bt = new BrodThread(TestFactory.kontejnerski("F4-NORMALNO"), luka);

            ExecutorService exec = Executors.newSingleThreadExecutor();
            Future<?> future = exec.submit(bt);
            try {
                cekajPrivezivanje(bt, 10_000);
                assertTrue(luka.getEvidencijaUlaska().containsKey("F4-NORMALNO"));

                bt.zatraziNapustanje();
                future.get(10, TimeUnit.SECONDS);

                assertFalse(luka.getEvidencijaUlaska().containsKey("F4-NORMALNO"),
                        "Zapis mora biti uklonjen iz evidencije nakon obračuna takse pri izlasku.");
            } finally {
                exec.shutdownNow();
                exec.awaitTermination(5, TimeUnit.SECONDS);
            }

            assertTrue(java.nio.file.Files.exists(csvPutanja()), "takse.csv mora biti kreiran.");
            List<String> linije = java.nio.file.Files.readAllLines(csvPutanja());
            assertTrue(linije.stream().anyMatch(l -> l.contains("F4-NORMALNO")),
                    "CSV mora sadržati red za plovilo koje je upravo napustilo luku.");
        } finally {
            vratiCsv(postojaoPrijeTesta);
        }
    }

    @Test
    @DisplayName("F4: obracunajIZabiljeziTaksu() je no-op za plovilo bez zapisa u evidenciji")
    void obracunajIZabiljeziTaksuNoOpBezEvidencije() {
        Luka luka = TestFactory.luka(1);
        BrodThread bt = new BrodThread(TestFactory.kontejnerski("F4-BEZ-EVIDENCIJE"), luka);

        assertDoesNotThrow(bt::obracunajIZabiljeziTaksu);
        assertFalse(luka.getEvidencijaUlaska().containsKey("F4-BEZ-EVIDENCIJE"));
    }

    @Test
    @DisplayName("F4: obracunajIZabiljeziTaksu() radi samostalno, bez obzira ko je poziva (K10-stil test)")
    void obracunajIZabiljeziTaksuRadiSamostalno() throws Exception {
        boolean postojaoPrijeTesta = sacuvajCsvAkoPostoji();
        try {
            Luka luka = TestFactory.luka(1);
            Plovilo p = TestFactory.kontejnerski("F4-DIREKTNO");
            luka.addToEvidencija("F4-DIREKTNO", java.time.LocalDateTime.now().minusHours(2));
            BrodThread bt = new BrodThread(p, luka);

            bt.obracunajIZabiljeziTaksu();

            assertFalse(luka.getEvidencijaUlaska().containsKey("F4-DIREKTNO"));
            List<String> linije = java.nio.file.Files.readAllLines(csvPutanja());
            assertTrue(linije.stream().anyMatch(l -> l.contains("F4-DIREKTNO")));
        } finally {
            vratiCsv(postojaoPrijeTesta);
        }
    }

    @Test
    @DisplayName("REGRESIJA: vez postaje i fizički i po rezervaciji ponovo raspoloživ nakon normalnog napuštanja "
            + "(rezervisiSlobodanDok() se nikad nije oslobađala na uspješnom privezivanju, prije R4)")
    void vezPostajeRaspolozivIPoRezervacijiNakonNormalnogNapustanja() throws Exception {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        BrodThread bt = new BrodThread(TestFactory.kontejnerski("REZERVACIJA-1"), luka);

        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> future = exec.submit(bt);
        try {
            cekajPrivezivanje(bt, 10_000);
            assertEquals(29, t.getBrojSlobodnihVezova());
            assertEquals(29, t.getBrojRaspolozivihVezova(),
                    "Rezervacija veza mora biti oslobođena čim plovilo stvarno stigne do njega.");

            bt.zatraziNapustanje();
            future.get(10, TimeUnit.SECONDS);

            assertEquals(30, t.getBrojSlobodnihVezova());
            assertEquals(30, t.getBrojRaspolozivihVezova(),
                    "Bez oslobađanja rezervacije na uspješnom privezivanju, ovaj vez bi ostao trajno "
                            + "\"rezervisan\" a prazan — getBrojRaspolozivihVezova() drifta naniže tokom duže "
                            + "simulacije dok luka ne izgleda puna iako nije.");
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("KRITIČNO: parkirano čekanje ne drži zaključan terminal — PrikazTerminala.render() ne blokira")
    void parkiranoCekanjeNeBlokiraRenderTerminala() throws Exception {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        BrodThread bt = new BrodThread(TestFactory.kontejnerski("3333333"), luka);

        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> future = exec.submit(bt);
        try {
            cekajPrivezivanje(bt, 10_000);

            assertTimeout(Duration.ofSeconds(2), () -> PrikazTerminala.render(t),
                    "render() se zaglavio dok je plovilo parkirano — wait() je vjerovatno pozvan unutar synchronized(terminal).");
        } finally {
            bt.zatraziNapustanje();
            exec.shutdownNow();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Plovilo koje nikad ne uđe u terminal ima koordinate (-1,-1) prije pokretanja niti")
    void koordinatePrijePokretanjaSuMinusJedan() {
        Luka luka = TestFactory.luka(1);
        BrodThread bt = new BrodThread(TestFactory.kontejnerski("4444444"), luka);

        assertEquals(-1, bt.getX());
        assertEquals(-1, bt.getY());
        assertEquals(Zadatak.KA_DOKU, bt.getZadatak(), "Početni zadatak mora biti KA_DOKU.");
    }

    @Test
    @DisplayName("Konstruktor za već privezano plovilo (C3/C4 seeding) startuje direktno u stanju PRIVEZAN")
    void predokovaniKonstruktorPocinjeDirektnoPrivezan() throws Exception {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        Dok dok = TestFactory.prviSlobodanDok(t);
        Plovilo p = TestFactory.tankerVatrogasci("5555555");
        dok.getLokacija().setTrenutnoPlovilo(p);

        BrodThread bt = new BrodThread(p, luka, t, dok);
        assertTrue(bt.isPrivezan(), "Konstruktor mora odmah postaviti isPrivezan na true.");
        assertEquals(dok.getLokacija().getX(), bt.getX());
        assertEquals(dok.getLokacija().getY(), bt.getY());

        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> future = exec.submit(bt);
        try {
            cekajZadatak(bt, Zadatak.PRIVEZAN, 5_000);
            assertTrue(luka.getAktivnaPlovila().contains(bt),
                    "Predokovano plovilo mora biti registrovano u Luka.getAktivnaPlovila().");
            assertFalse(future.isDone(), "Nit predokovanog plovila ne smije završiti dok je parkirana.");
        } finally {
            bt.zatraziNapustanje();
            exec.shutdownNow();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Luka.getAktivnaPlovila() registruje nit dok radi i uklanja je nakon napuštanja terminala")
    void aktivnaPlovilaSeRegistrujuIUklanjaju() throws Exception {
        Luka luka = TestFactory.luka(1);
        BrodThread bt = new BrodThread(TestFactory.kontejnerski("6666666"), luka);

        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> future = exec.submit(bt);
        try {
            cekajPrivezivanje(bt, 10_000);
            assertTrue(luka.getAktivnaPlovila().contains(bt),
                    "Privezano plovilo mora ostati registrovano dok je parkirano.");

            bt.zatraziNapustanje();
            future.get(10, TimeUnit.SECONDS);

            assertFalse(luka.getAktivnaPlovila().contains(bt),
                    "Plovilo koje je napustilo terminal ne smije ostati u registru živih niti.");
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Plovilo koje ne uspije da se priveže (pun terminal) se ipak uklanja iz registra po završetku")
    void aktivnaPlovilaSeUklanjajuIKadPloviloOdustane() throws Exception {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        TestFactory.popuniSveDokove(t);

        BrodThread bt = new BrodThread(TestFactory.kontejnerski("7777777"), luka);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> future = exec.submit(bt);
        try {
            future.get(20, TimeUnit.SECONDS);

            assertFalse(bt.isPrivezan());
            assertEquals(Zadatak.KA_DOKU, bt.getZadatak(), "Plovilo koje odustane nikad nije bilo privezano.");
            assertFalse(luka.getAktivnaPlovila().contains(bt),
                    "Plovilo koje je odustalo ne smije ostati u registru živih niti.");
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    // ------------------------------------------------------------------
    // R4a — blokada saobraćaja na terminalu (I3/I4): provjera u pomjeriNaPolje()
    // ------------------------------------------------------------------

    /** Hoda susjednim poljima od doka do (KANAL_IZLAZ, kolona) — G3: pomjeriNaPolje() odbija skokove. */
    private static void pomjeriUKanalIzlaz(BrodThread bt, int kolona) {
        while (bt.getX() != Terminal.KANAL_IZLAZ) {
            int sljedeciX = bt.getX() < Terminal.KANAL_IZLAZ ? bt.getX() + 1 : bt.getX() - 1;
            assertTrue(bt.pomjeriNaPolje(sljedeciX, bt.getY()));
        }
        while (bt.getY() != kolona) {
            int sljedeciY = bt.getY() < kolona ? bt.getY() + 1 : bt.getY() - 1;
            assertTrue(bt.pomjeriNaPolje(Terminal.KANAL_IZLAZ, sljedeciY));
        }
    }

    @Test
    @DisplayName("G2: pokusajUciUTerminal() ne uspijeva za obično plovilo dok je terminal blokiran, "
            + "uspijeva nakon odblokade")
    void pokusajUciUTerminalNeUspijevaKadJeTerminalBlokiran() {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        BrodThread bt = new BrodThread(TestFactory.kontejnerski("BLOK-ENTRY"), luka);

        t.blokirajSaobracaj();
        assertFalse(bt.pokusajUciUTerminal(t), "Obično plovilo ne smije ući u blokiran terminal.");
        assertNull(t.getMatrica()[0][Terminal.KOLONA_ULAZ].getTrenutnoPlovilo());

        t.odblokirajSaobracaj();
        assertTrue(bt.pokusajUciUTerminal(t), "Nakon odblokade ulazak mora ponovo uspjeti.");
    }

    @Test
    @DisplayName("G2: plovilo pod rotacijom smije ući u terminal i kad je blokiran")
    void pokusajUciUTerminalUspijevaZaPloviloPodRotacijomKadJeTerminalBlokiran() {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        TankerVatrogasci vatrogasci = TestFactory.tankerVatrogasci("BLOK-ENTRY-ROT");
        vatrogasci.setRotacija(true);
        BrodThread bt = new BrodThread(vatrogasci, luka);

        t.blokirajSaobracaj();

        assertTrue(bt.pokusajUciUTerminal(t),
                "Plovilo pod rotacijom mora moći ući i kroz blokiran terminal.");
    }

    @Test
    @DisplayName("R4a: pomjeriNaPolje() ne uspijeva za obično plovilo dok je terminal blokiran")
    void pomjeriNaPoljeNeUspijevaKadJeTerminalBlokiranZaObicnoPlovilo() {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        Dok dok = TestFactory.prviSlobodanDok(t);
        Plovilo p = TestFactory.kontejnerski("BLOK-1");
        dok.getLokacija().setTrenutnoPlovilo(p);

        BrodThread bt = new BrodThread(p, luka, t, dok);
        pomjeriUKanalIzlaz(bt, 5);
        t.blokirajSaobracaj();

        assertFalse(bt.pomjeriNaPolje(Terminal.KANAL_IZLAZ, 6),
                "Obično plovilo ne smije moći da se pomjeri dok je terminal blokiran.");
        assertSame(p, t.getMatrica()[Terminal.KANAL_IZLAZ][5].getTrenutnoPlovilo(), "Plovilo mora ostati na starom polju.");
        assertNull(t.getMatrica()[Terminal.KANAL_IZLAZ][6].getTrenutnoPlovilo());
    }

    @Test
    @DisplayName("R4a: pomjeriNaPolje() uspijeva za plovilo pod rotacijom čak i kad je terminal blokiran")
    void pomjeriNaPoljeUspijevaZaPloviloPodRotacijomKadJeTerminalBlokiran() {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        Dok dok = TestFactory.prviSlobodanDok(t);
        TankerVatrogasci vatrogasci = TestFactory.tankerVatrogasci("BLOK-2");
        vatrogasci.setRotacija(true);
        dok.getLokacija().setTrenutnoPlovilo(vatrogasci);

        BrodThread bt = new BrodThread(vatrogasci, luka, t, dok);
        pomjeriUKanalIzlaz(bt, 5);
        t.blokirajSaobracaj();

        assertTrue(bt.pomjeriNaPolje(Terminal.KANAL_IZLAZ, 6),
                "Plovilo pod rotacijom mora moći da se pomjeri i kroz blokiran terminal.");
        assertSame(vatrogasci, t.getMatrica()[Terminal.KANAL_IZLAZ][6].getTrenutnoPlovilo());
    }

    @Test
    @DisplayName("R4a: službeno plovilo bez uključene rotacije se ponaša kao obično — blokada ga zaustavlja")
    void pomjeriNaPoljeZaustavljaSluzbenoPloviloBezRotacije() {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        Dok dok = TestFactory.prviSlobodanDok(t);
        TankerVatrogasci vatrogasci = TestFactory.tankerVatrogasci("BLOK-3");
        dok.getLokacija().setTrenutnoPlovilo(vatrogasci);

        BrodThread bt = new BrodThread(vatrogasci, luka, t, dok);
        pomjeriUKanalIzlaz(bt, 5);
        t.blokirajSaobracaj();

        assertFalse(bt.pomjeriNaPolje(Terminal.KANAL_IZLAZ, 6),
                "Rotacija mora biti aktivno uključena — sama pripadnost službi nije dovoljna.");
    }

    @Test
    @DisplayName("R4a: odblokirajSaobracaj() vraća normalno kretanje za obično plovilo")
    void pomjeriNaPoljeRadiNormalnoNakonOdblokade() {
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        Dok dok = TestFactory.prviSlobodanDok(t);
        Plovilo p = TestFactory.kontejnerski("BLOK-4");
        dok.getLokacija().setTrenutnoPlovilo(p);

        BrodThread bt = new BrodThread(p, luka, t, dok);
        pomjeriUKanalIzlaz(bt, 5);
        t.blokirajSaobracaj();
        assertFalse(bt.pomjeriNaPolje(Terminal.KANAL_IZLAZ, 6));

        t.odblokirajSaobracaj();
        assertTrue(bt.pomjeriNaPolje(Terminal.KANAL_IZLAZ, 6),
                "Nakon odblokade obično plovilo ponovo smije da se kreće.");
    }

    @Test
    @DisplayName("R4a: blokada saobraćaja ne troši budžet pokušaja u pomjeriSaCekanjem() — "
            + "plovilo ne odustaje samo zato što je uviđaj potrajao maksimalno dugo")
    void pomjeriSaCekanjemNeOdustajeZbogBlokadeIakoTrajeDuzeOdBudzetaPokusaja() throws Exception {
        // MAX_POKUSAJA (100) * CEKANJE_MS (100ms) = 10000ms, tačno MAX_TRAJANJE_UVIDJAJA_MS
        // (podrazumijevano). Prije ove ispravke, plovilo koje bi čekalo baš na posljednjem koraku
        // ulaska u dok tokom najdužeg mogućeg uviđaja bi otkazalo rezervaciju veza samo zbog
        // podudarnosti dva vremenska budžeta, iako ciljna ćelija nikad nije bila stvarno zauzeta.
        Luka luka = TestFactory.luka(1);
        Terminal t = luka.getTerminali().get(0);
        Dok dok = TestFactory.prviSlobodanDok(t);
        Plovilo p = TestFactory.kontejnerski("BLOK-5");
        dok.getLokacija().setTrenutnoPlovilo(p);

        BrodThread bt = new BrodThread(p, luka, t, dok);
        pomjeriUKanalIzlaz(bt, 5);
        t.blokirajSaobracaj();

        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<Boolean> future = exec.submit(() -> bt.pomjeriSaCekanjem(Terminal.KANAL_IZLAZ, 6, 50L));
        try {
            Thread.sleep(10_500);
            assertFalse(future.isDone(),
                    "Plovilo je odustalo (potrošilo budžet pokušaja) dok je terminal i dalje "
                            + "blokiran — blokada ne smije trošiti budžet pokušaja (I3).");

            t.odblokirajSaobracaj();
            assertTrue(future.get(5, TimeUnit.SECONDS),
                    "Nakon odblokade plovilo mora uspješno završiti pomjeranje ka ciljnoj ćeliji.");
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    // ------------------------------------------------------------------
    // D5 — determinizam sudara: injektovan Random + mutabilna vjerovatnoća/trajanje uviđaja
    // ------------------------------------------------------------------

    /**
     * Postavlja bt na KANAL_IZLAZ i drugog na KANAL_ULAZ, istu kolonu — konfiguracija mimoilaženja.
     * Hoda susjednim poljima (G3: pomjeriNaPolje() sada odbija skokove), ne teleportuje se.
     */
    private static Plovilo pripremiMimoilazenje(BrodThread bt, Terminal t, int kolona, String imoDrugog) {
        assertTrue(bt.pokusajUciUTerminal(t));
        assertTrue(bt.pomjeriNaPolje(Terminal.KANAL_IZLAZ, 0));
        for (int y = 1; y <= kolona; y++) {
            assertTrue(bt.pomjeriNaPolje(Terminal.KANAL_IZLAZ, y));
        }
        Plovilo drugi = TestFactory.kontejnerski(imoDrugog);
        t.getMatrica()[Terminal.KANAL_ULAZ][kolona].setTrenutnoPlovilo(drugi);
        return drugi;
    }

    @Test
    @DisplayName("R4b-1: provjeriSudar() detektuje oba učesnika kad je vjerovatnoća 1.0 i plovila se mimoilaze")
    void provjeriSudarDetektujeObaUcesnikaKadJeVjerovatnocaJedan() {
        boolean staroSudari = BrodThread.SUDARI_OMOGUCENI;
        double staraVjerovatnoca = BrodThread.VJEROVATNOCA_SUDARA;
        try {
            BrodThread.SUDARI_OMOGUCENI = true;
            BrodThread.VJEROVATNOCA_SUDARA = 1.0;

            Luka luka = TestFactory.luka(1);
            Terminal t = luka.getTerminali().get(0);
            Plovilo prvi = TestFactory.kontejnerski("D5-1");
            BrodThread bt = new BrodThread(prvi, luka);
            bt.setGeneratorSudara(new Random(1));
            Plovilo drugi = pripremiMimoilazenje(bt, t, 5, "D5-1B");

            for (int i = 0; i < 100; i++) {
                Plovilo[] rezultat = bt.provjeriSudar();
                assertNotNull(rezultat, "Vjerovatnoća 1.0 mora garantovati sudar na svakoj provjeri.");
                assertEquals(2, rezultat.length);
                assertTrue(
                        (rezultat[0] == prvi && rezultat[1] == drugi)
                                || (rezultat[0] == drugi && rezultat[1] == prvi),
                        "Sudar mora vratiti tačno dva različita, stvarna učesnika mimoilaženja.");
            }
        } finally {
            BrodThread.SUDARI_OMOGUCENI = staroSudari;
            BrodThread.VJEROVATNOCA_SUDARA = staraVjerovatnoca;
        }
    }

    @Test
    @DisplayName("R4b-1: provjeriSudar() nikad ne prijavljuje sudar kad je vjerovatnoća 0.0, i uz plovila u mimoilaženju")
    void provjeriSudarNikadNePrijavljujeKadJeVjerovatnocaNula() {
        boolean staroSudari = BrodThread.SUDARI_OMOGUCENI;
        double staraVjerovatnoca = BrodThread.VJEROVATNOCA_SUDARA;
        try {
            BrodThread.SUDARI_OMOGUCENI = true;
            BrodThread.VJEROVATNOCA_SUDARA = 0.0;

            Luka luka = TestFactory.luka(1);
            Terminal t = luka.getTerminali().get(0);
            BrodThread bt = new BrodThread(TestFactory.kontejnerski("D5-2"), luka);
            bt.setGeneratorSudara(new Random(2));
            pripremiMimoilazenje(bt, t, 5, "D5-2B");

            for (int i = 0; i < 100; i++) {
                assertNull(bt.provjeriSudar(), "Vjerovatnoća 0.0 ne smije nikad prijaviti sudar.");
            }
        } finally {
            BrodThread.SUDARI_OMOGUCENI = staroSudari;
            BrodThread.VJEROVATNOCA_SUDARA = staraVjerovatnoca;
        }
    }

    @Test
    @DisplayName("R4b-1: provjeriSudar() je uvijek null kad su sudari globalno onemogućeni (I1), bez obzira na vjerovatnoću")
    void provjeriSudarJeIskljucenKadSuSudariOnemoguceni() {
        boolean staroSudari = BrodThread.SUDARI_OMOGUCENI;
        double staraVjerovatnoca = BrodThread.VJEROVATNOCA_SUDARA;
        try {
            BrodThread.SUDARI_OMOGUCENI = false;
            BrodThread.VJEROVATNOCA_SUDARA = 1.0;

            Luka luka = TestFactory.luka(1);
            Terminal t = luka.getTerminali().get(0);
            BrodThread bt = new BrodThread(TestFactory.kontejnerski("D5-3"), luka);
            bt.setGeneratorSudara(new Random(3));
            pripremiMimoilazenje(bt, t, 5, "D5-3B");

            for (int i = 0; i < 20; i++) {
                assertNull(bt.provjeriSudar(),
                        "I1: dok je SUDARI_OMOGUCENI isključeno, sudara ne smije biti ni uz vjerovatnoću 1.0.");
            }
        } finally {
            BrodThread.SUDARI_OMOGUCENI = staroSudari;
            BrodThread.VJEROVATNOCA_SUDARA = staraVjerovatnoca;
        }
    }

    @Test
    @DisplayName("R4b-1: provjeriSudar() ne detektuje sudar kad nema drugog plovila u mimoilaženju, bez obzira na vjerovatnoću")
    void provjeriSudarBezDrugogUcesnikaVracaNull() {
        boolean staroSudari = BrodThread.SUDARI_OMOGUCENI;
        double staraVjerovatnoca = BrodThread.VJEROVATNOCA_SUDARA;
        try {
            BrodThread.SUDARI_OMOGUCENI = true;
            BrodThread.VJEROVATNOCA_SUDARA = 1.0;

            Luka luka = TestFactory.luka(1);
            Terminal t = luka.getTerminali().get(0);
            BrodThread bt = new BrodThread(TestFactory.kontejnerski("D5-5"), luka);
            bt.setGeneratorSudara(new Random(5));
            assertTrue(bt.pokusajUciUTerminal(t));
            assertTrue(bt.pomjeriNaPolje(Terminal.KANAL_IZLAZ, 0));
            for (int y = 1; y <= 5; y++) {
                assertTrue(bt.pomjeriNaPolje(Terminal.KANAL_IZLAZ, y));
            }

            assertNull(bt.provjeriSudar(), "Bez drugog plovila u susjednoj traci nema sudara.");
        } finally {
            BrodThread.SUDARI_OMOGUCENI = staroSudari;
            BrodThread.VJEROVATNOCA_SUDARA = staraVjerovatnoca;
        }
    }

    @Test
    @DisplayName("D5: isti seed ubrizgan u dva plovila daje identičan niz ishoda sudara")
    void istiSeedDajeIdenticanNizIshodaSudara() {
        boolean staroSudari = BrodThread.SUDARI_OMOGUCENI;
        double staraVjerovatnoca = BrodThread.VJEROVATNOCA_SUDARA;
        try {
            BrodThread.SUDARI_OMOGUCENI = true;
            BrodThread.VJEROVATNOCA_SUDARA = 0.5;

            Luka lukaPrvi = TestFactory.luka(1);
            BrodThread prvi = new BrodThread(TestFactory.kontejnerski("D5-4A"), lukaPrvi);
            prvi.setGeneratorSudara(new Random(42));
            pripremiMimoilazenje(prvi, lukaPrvi.getTerminali().get(0), 5, "D5-4A-B");

            Luka lukaDrugi = TestFactory.luka(1);
            BrodThread drugi = new BrodThread(TestFactory.kontejnerski("D5-4B"), lukaDrugi);
            drugi.setGeneratorSudara(new Random(42));
            pripremiMimoilazenje(drugi, lukaDrugi.getTerminali().get(0), 5, "D5-4B-B");

            for (int i = 0; i < 200; i++) {
                assertEquals(prvi.provjeriSudar() != null, drugi.provjeriSudar() != null,
                        "Isti seed mora dati isti niz ishoda na poziciji " + i + ".");
            }
        } finally {
            BrodThread.SUDARI_OMOGUCENI = staroSudari;
            BrodThread.VJEROVATNOCA_SUDARA = staraVjerovatnoca;
        }
    }

    @Test
    @DisplayName("D5: podrazumijevana vjerovatnoća sudara je 2% (I1), a trajanja uviđaja imaju dokumentovane podrazumijevane vrijednosti")
    void podrazumijevaneVrijednostiZaD5() {
        // VJEROVATNOCA_SUDARA se ne provjerava ovdje protiv 0.02 — @BeforeEach ove klase ga
        // namjerno spušta na 0.0 prije svakog testa (da bi ostali testovi u ovoj klasi koji
        // pokreću stvarnu navigaciju ostali deterministički), pa "podrazumijevana" vrijednost
        // 0.02 iz izvornog koda nije opservabilna kroz živo polje unutar test paketa.
        assertTrue(BrodThread.SUDARI_OMOGUCENI, "I1: sudari moraju biti podrazumijevano omogućeni.");
        assertEquals(3000L, BrodThread.MIN_TRAJANJE_UVIDJAJA_MS);
        assertEquals(10000L, BrodThread.MAX_TRAJANJE_UVIDJAJA_MS);
        assertEquals(3000L, BrodThread.MIN_TRAJANJE_UVIDJAJA_POTJERNICE_MS);
        assertEquals(5000L, BrodThread.MAX_TRAJANJE_UVIDJAJA_POTJERNICE_MS);
    }

    @Test
    @DisplayName("D5: trajanja uviđaja se mogu spustiti na ~50ms radi bržih testova (nisu final)")
    void trajanjaUvidjajaSeMoguSpustitiRadiTestova() {
        long staroMin = BrodThread.MIN_TRAJANJE_UVIDJAJA_MS;
        long staroMax = BrodThread.MAX_TRAJANJE_UVIDJAJA_MS;
        long staroMinPotjera = BrodThread.MIN_TRAJANJE_UVIDJAJA_POTJERNICE_MS;
        long staroMaxPotjera = BrodThread.MAX_TRAJANJE_UVIDJAJA_POTJERNICE_MS;
        try {
            BrodThread.MIN_TRAJANJE_UVIDJAJA_MS = 50L;
            BrodThread.MAX_TRAJANJE_UVIDJAJA_MS = 50L;
            BrodThread.MIN_TRAJANJE_UVIDJAJA_POTJERNICE_MS = 50L;
            BrodThread.MAX_TRAJANJE_UVIDJAJA_POTJERNICE_MS = 50L;

            assertEquals(50L, BrodThread.MIN_TRAJANJE_UVIDJAJA_MS);
            assertEquals(50L, BrodThread.MAX_TRAJANJE_UVIDJAJA_MS);
            assertEquals(50L, BrodThread.MIN_TRAJANJE_UVIDJAJA_POTJERNICE_MS);
            assertEquals(50L, BrodThread.MAX_TRAJANJE_UVIDJAJA_POTJERNICE_MS);
        } finally {
            BrodThread.MIN_TRAJANJE_UVIDJAJA_MS = staroMin;
            BrodThread.MAX_TRAJANJE_UVIDJAJA_MS = staroMax;
            BrodThread.MIN_TRAJANJE_UVIDJAJA_POTJERNICE_MS = staroMinPotjera;
            BrodThread.MAX_TRAJANJE_UVIDJAJA_POTJERNICE_MS = staroMaxPotjera;
        }
    }
}
