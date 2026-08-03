package org.unibl.etf.pj2.luka.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.unibl.etf.pj2.luka.model.classes.*;
import org.unibl.etf.pj2.luka.testutil.TestFactory;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testovi kretanja brodova i konkurentnog pristupa matrici terminala.
 *
 * <p>Ovi testovi su najvažniji u paketu jer je {@link BrodThread} do sada bio pisan
 * bez ijednog izvršavanja pod opterećenjem — {@code Main} je ostao prazan.</p>
 *
 * <p>Testovi koriste {@link Uzorkovac}, pomoćnu nit koja periodično snima stanje matrice
 * i akumulira prekršaje invarijanti. Time se provjeravaju svojstva koja se ne mogu
 * provjeriti tek na kraju simulacije (npr. "brod nikada ne smije proći kroz dok").</p>
 */
@DisplayName("BrodThread — kretanje i konkurentnost")
class BrodThreadTest {

    private static final int TIMEOUT_SEC = 40;

    // ==================================================================
    // Pomoćna infrastruktura
    // ==================================================================

    /**
     * Nit koja periodično snima stanje svih terminala i biljezi prekršaje invarijanti.
     */
    private static final class Uzorkovac implements Runnable {
        private final Luka luka;
        private volatile boolean radi = true;

        /** Prekršaji tipa: dvije reference na isto plovilo istovremeno u matrici. */
        final List<String> duplikati = Collections.synchronizedList(new ArrayList<String>());

        /** Za svako plovilo, skup dok-polja koje je ikada zauzelo (idTerminala:x,y). */
        final Map<String, Set<String>> dokPoljaPoPlovilu = new ConcurrentHashMap<>();

        /** Da li je ijedno plovilo ikada viđeno u horizontalnom kanalu (red 1 ili 2, kolona >= 3). */
        volatile boolean vidjenUHorizontalnomKanalu = false;

        /** Minimalan broj slobodnih vezova ikada izmjeren, po terminalu. */
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

    /** Pokreće zadata plovila kao niti i čeka da sve završe ili istekne timeout. */
    private static boolean pokreniIsacekaj(Luka luka, List<Plovilo> plovila, Uzorkovac uzorkovac)
            throws InterruptedException {

        ExecutorService exec = Executors.newFixedThreadPool(Math.max(2, plovila.size()));
        Thread nitUzorkovaca = null;

        if (uzorkovac != null) {
            nitUzorkovaca = new Thread(uzorkovac, "uzorkovac");
            nitUzorkovaca.setDaemon(true);
            nitUzorkovaca.start();
        }

        for (Plovilo p : plovila) {
            exec.submit(new BrodThread(p, luka));
        }
        exec.shutdown();

        boolean zavrsilo = exec.awaitTermination(TIMEOUT_SEC, TimeUnit.SECONDS);
        exec.shutdownNow();

        if (uzorkovac != null) {
            uzorkovac.stani();
            nitUzorkovaca.join(1000);
        }
        return zavrsilo;
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

    // ==================================================================
    // BUCKET A — ulazak u terminal
    // ==================================================================

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

    // ==================================================================
    // BUCKET A — osnovna simulacija
    // ==================================================================

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

    // ==================================================================
    // BUCKET B — prekršaji pravila kretanja
    // ==================================================================

    @Test
    @Tag("bug")
    @DisplayName("BUG: brod prolazi kroz dokove umjesto kroz horizontalni kanal")
    void brodNeSmijeProlazitiKrozDokove() throws Exception {
        // Svako plovilo smije zauzeti najviše JEDNO dok-polje u cijelom svom životu:
        // ono na kojem se konačno priveže. Ako je zauzelo više njih, kretalo se
        // uzduž reda 3 (ili 0), tj. kroz dokove drugih brodova.
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

    @Test
    @Tag("bug")
    @DisplayName("BUG: sudar mora uključiti dva plovila pri mimoilaženju, ne jedno samo")
    void sudarUkljucujeDvaPlovila() {
        // proveriRizikOdUdesa() je privatna metoda koja jednostrano proglašava kvar
        // na jednom brodu, uspava sopstvenu nit i ne obavještava nikoga.
        // Specifikacija traži: sudar pri MIMOILAŽENJU dva broda (2%), slanje najbliže
        // patrole obalske straže, carine i vatrogasaca pod rotacijom, blokadu saobraćaja
        // na tom terminalu i evidenciju u binarnom fajlu.
        //
        // Ovaj test namjerno pada dok ne postoji zaseban model incidenta.
        fail("Nedostaje klasa Incident i koordinator uviđaja — vidi refaktor R4 u PRONALASCI.md. "
                + "Trenutna implementacija je Thread.sleep() unutar jedne niti, bez ijednog drugog učesnika.");
    }

    @Test
    @Tag("bug")
    @DisplayName("BUG: prioritet pod rotacijom ne utiče na kretanje")
    void rotacijaDajePrednostUKanalu() throws Exception {
        // getPrioritet() postoji na svim službenim klasama i vraća ispravne vrijednosti,
        // ali BrodThread ga nikada ne čita. Vatrogasno plovilo pod rotacijom
        // trenutno čeka u redu isto kao i teretni brod.
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

        pokreniIsacekaj(luka, plovila, null);

        // Provjera da kod uopšte konsultuje prioritet:
        String izvor = BrodThread.class.getName();
        assertTrue(false,
                "BrodThread nikada ne poziva getPrioritet() — pretraži " + izvor
                        + " i uvjeri se. Pravilo preticanja pod rotacijom nije implementirano (refaktor R5).");
    }
}
