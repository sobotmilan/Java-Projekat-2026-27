package org.unibl.etf.pj2.luka.gui;

import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.simulation.BrodThread;
import org.unibl.etf.pj2.luka.simulation.PokretacSimulacije;
import org.unibl.etf.pj2.luka.util.SerializationUtil;
import org.unibl.etf.pj2.luka.view.PrikazTerminala;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.*;

/**
 * Glavni prozor klijentske aplikacije: pokreće i prikazuje živu simulaciju kretanja plovila kroz
 * luku, omogućava dodavanje novih plovila u toku simulacije, i po završetku ponovo serijalizuje
 * stanje luke.
 *
 * @author Milan Šobot
 * @version 1.0
 * @see KlijentskaSimulacijaService
 */
public class KlijentskiProzor extends JFrame {

    /** Razmak između uzastopnih napuštanja terminala plovila označenih za odlazak, u milisekundama. */
    private static final int RAZMAK_ODLAZAKA_MS = 300;

    /** Trenutno stanje luke koje ovaj prozor prikazuje i kojim upravlja. */
    private Luka luka;

    /** Polje za unos minimalnog broja plovila po terminalu prije pokretanja simulacije. */
    private final JTextField minimumPolje = new JTextField("1", 4);
    /** Dugme koje pokreće simulaciju sa unesenim minimumom. */
    private final JButton pokreniDugme = new JButton("Pokreni simulaciju");

    /** Padajući meni za izbor terminala čiji se prikaz trenutno gleda. */
    private final JComboBox<Terminal> terminalCombo = new JComboBox<>();
    /** Tekstualno polje koje prikazuje trenutno stanje odabranog terminala. */
    private final JTextArea prikazPolje = new JTextArea();
    /** Oznaka koja prikazuje broj slobodnih vezova na trenutno odabranom terminalu. */
    private final JLabel slobodniVezoviLabel = new JLabel(" ");

    /** Padajući meni za izbor tipa plovila koje se dodaje tokom simulacije. */
    private final JComboBox<TipPlovila> tipCombo = new JComboBox<>(TipPlovila.values());
    /** Dugme koje otvara formu za dodavanje novog plovila tokom simulacije. */
    private final JButton dodajDugme = new JButton("Dodaj plovilo");

    /** IMO brojevi plovila označenih da napuste luku nakon pokretanja simulacije. */
    private final Set<String> imoZaOdlazak = new HashSet<>();
    /** IMO brojevi plovila dodatih tokom trajanja simulacije. */
    private final Set<String> imoDodataTokomSimulacije = new HashSet<>();

    /** Tajmer koji periodično osvježava prikaz terminala i provjerava kraj simulacije. */
    private Timer timer;
    /** Jednokratni tajmer koji, nekoliko sekundi nakon pokretanja simulacije, označava plovila za odlazak. */
    private Timer odgodaTimer;
    /** Ponavljajući tajmer koji redom, sa razmakom od {@link #RAZMAK_ODLAZAKA_MS}, poziva označena plovila da napuste terminal. */
    private Timer rasporedTimer;
    /** Postavlja se na {@code true} kad simulacija stigne do kraja, sprečava ponovno pokretanje završetka. */
    private boolean simulacijaZavrsena = false;
    /** Postavlja se na {@code true} nakon što su plovila za odlazak označena, uslov za provjeru kraja simulacije. */
    private boolean odlasciOznaceni = false;

    /**
     * Kreira klijentski prozor nad zadatim stanjem luke i izgrađuje njegov korisnički interfejs.
     *
     * @param luka Početno stanje luke koje prozor prikazuje.
     */
    public KlijentskiProzor(Luka luka) {
        super("Klijentska aplikacija");
        this.luka = luka;
        // DISPOSE_ON_CLOSE (ne podrazumijevani HIDE_ON_CLOSE) — AdminProzor osluškuje windowClosed
        // da ponovo učita luka.ser čim se klijent zatvori; taj događaj se ne diže na HIDE_ON_CLOSE
        // (samo sakriva prozor, ne diže WINDOW_CLOSED).
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 600);
        setLocationRelativeTo(null);

        izgradiUI();
    }

    /**
     * Sastavlja i raspoređuje sve komponente prozora: gornju traku sa unosom i dugmadima, prikaz
     * terminala u sredini, i donju traku sa oznakom slobodnih vezova.
     */
    private void izgradiUI() {
        terminalCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object vrijednost, int indeks,
                                                            boolean izabran, boolean fokus) {
                super.getListCellRendererComponent(list, vrijednost, indeks, izabran, fokus);
                if (vrijednost instanceof Terminal t) {
                    setText("Terminal " + (t.getIdTerminala() + 1));
                }
                return this;
            }
        });

        osvjeziTerminalCombo();
        terminalCombo.addItemListener(e -> {
            if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                osvjeziPrikaz();
            }
        });

        prikazPolje.setEditable(false);
        prikazPolje.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        pokreniDugme.addActionListener(e -> pokreniSimulaciju());

        tipCombo.setEnabled(false);
        dodajDugme.setEnabled(false);
        dodajDugme.addActionListener(e -> dodajPloviloTokomSimulacije());

        JPanel gornjaTraka = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gornjaTraka.add(new JLabel("Minimalan broj plovila po terminalu:"));
        gornjaTraka.add(minimumPolje);
        gornjaTraka.add(pokreniDugme);
        gornjaTraka.add(new JLabel("Terminal:"));
        gornjaTraka.add(terminalCombo);
        gornjaTraka.add(new JLabel("Tip plovila:"));
        gornjaTraka.add(tipCombo);
        gornjaTraka.add(dodajDugme);

        JPanel donjaTraka = new JPanel(new FlowLayout(FlowLayout.LEFT));
        donjaTraka.add(slobodniVezoviLabel);

        setLayout(new BorderLayout());
        add(gornjaTraka, BorderLayout.NORTH);
        add(new JScrollPane(prikazPolje), BorderLayout.CENTER);
        add(donjaTraka, BorderLayout.SOUTH);

        osvjeziPrikaz();
    }

    /**
     * Ponovo puni padajući meni terminala prema trenutnoj listi terminala u {@link #luka}, i bira
     * prvi terminal ako meni prethodno nije imao izbor.
     */
    private void osvjeziTerminalCombo() {
        DefaultComboBoxModel<Terminal> model = new DefaultComboBoxModel<>();
        for (Terminal t : luka.getTerminali()) {
            model.addElement(t);
        }
        terminalCombo.setModel(model);
        if (terminalCombo.getItemCount() > 0) {
            terminalCombo.setSelectedIndex(0);
        }
    }

    /**
     * Osvježava tekstualni prikaz i oznaku slobodnih vezova prema trenutnom stanju odabranog
     * terminala.
     */
    private void osvjeziPrikaz() {
        Terminal t = (Terminal) terminalCombo.getSelectedItem();
        if (t == null) {
            prikazPolje.setText("");
            slobodniVezoviLabel.setText(" ");
            return;
        }
        prikazPolje.setText(PrikazTerminala.renderAsText(t));
        slobodniVezoviLabel.setText("Slobodni vezovi na odabranom terminalu: " + t.getBrojSlobodnihVezova());
    }

    // ------------------------------------------------------------------
    // Korak 1 — unos i pokretanje simulacije
    // ------------------------------------------------------------------

    /**
     * Validira uneseni minimum, pa u pozadinskoj niti priprema početno stanje simulacije i
     * pokreće niti privezanih plovila, kako aplikacija ne bi "zamrzla" korisnički interfejs dok
     * traje priprema. Nakon uspješne pripreme omogućava dodavanje plovila i pokreće živi prikaz.
     */
    private void pokreniSimulaciju() {
        List<String> greske = KlijentskaSimulacijaService.validirajMinimum(minimumPolje.getText());
        if (!greske.isEmpty()) {
            prikaziPoruku(String.join("\n", greske), "Greška", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int minimum = Integer.parseInt(minimumPolje.getText().trim());

        pokreniDugme.setEnabled(false);
        minimumPolje.setEnabled(false);

        new SwingWorker<Luka, Void>() {
            @Override
            protected Luka doInBackground() {
                return PokretacSimulacije.pripremiPocetnoStanje(minimum);
            }

            @Override
            protected void done() {
                try {
                    luka = get();
                } catch (Exception ex) {
                    throw new IllegalStateException("Neuspješna priprema simulacije.", ex);
                }

                osvjeziTerminalCombo();
                List<BrodThread> sveNiti = PokretacSimulacije.pokreniPrivezanaPlovila(luka);
                // oznaciZaOdlazak(sveNiti);

                dodajDugme.setEnabled(true);
                tipCombo.setEnabled(true);

                zapocniZiviPrikaz();

                odgodaTimer = new Timer(3000, e -> oznaciZaOdlazak(sveNiti));
                odgodaTimer.setRepeats(false);
                odgodaTimer.start();
            }
        }.execute();
    }

    // ------------------------------------------------------------------
    // Korak 3 — odlazak 15% plovila po terminalu
    // ------------------------------------------------------------------

    /**
     * Grupiše sve niti po terminalu na kojem su privezane, bira dio plovila sa svakog terminala
     * za odlazak, i zakazuje njihovo napuštanje jedno po jedno, sa razmakom od
     * {@link #RAZMAK_ODLAZAKA_MS} između uzastopnih odlazaka.
     *
     * @param sveNiti Sve niti pokrenute za privezana plovila.
     */
    private void oznaciZaOdlazak(List<BrodThread> sveNiti) {
        Map<Terminal, List<BrodThread>> poTerminalu = new HashMap<>();
        for (BrodThread bt : sveNiti) {
            poTerminalu.computeIfAbsent(bt.getTrenutniTerminal(), k -> new ArrayList<>()).add(bt);
        }

        List<BrodThread> zaOdlazak = new ArrayList<>();
        for (List<BrodThread> naTerminalu : poTerminalu.values()) {
            for (BrodThread bt : KlijentskaSimulacijaService.odaberiZaOdlazak(naTerminalu)) {
                imoZaOdlazak.add(bt.getPlovilo().getImoBroj());
                zaOdlazak.add(bt);
            }
        }
        odlasciOznaceni = true;

        Iterator<BrodThread> it = zaOdlazak.iterator();
        rasporedTimer = new Timer(RAZMAK_ODLAZAKA_MS, null);
        rasporedTimer.addActionListener(e -> {
            if (it.hasNext()) {
                it.next().zatraziNapustanje();
            } else {
                rasporedTimer.stop();
            }
        });
        rasporedTimer.start();
    }

    // ------------------------------------------------------------------
    // Korak 2 — živi prikaz
    // ------------------------------------------------------------------

    /**
     * Pokreće tajmer koji periodično poziva {@link #tik()} dok simulacija traje.
     */
    private void zapocniZiviPrikaz() {
        timer = new Timer((int) PokretacSimulacije.INTERVAL_RENDEROVANJA_MS, e -> tik());
        timer.start();
    }

    /**
     * Osvježava prikaz i provjerava da li je simulacija stigla do kraja, pozivano periodično dok
     * je {@link #timer} aktivan.
     */
    private void tik() {
        osvjeziPrikaz();
        if (odlasciOznaceni && !simulacijaZavrsena
                && KlijentskaSimulacijaService.jeSimulacijaZavrsena(luka, imoZaOdlazak, imoDodataTokomSimulacije)) {
            simulacijaZavrsena = true;
            zavrsiSimulaciju();
        }
    }

    // ------------------------------------------------------------------
    // Korak 4 — dodavanje plovila tokom simulacije
    // ------------------------------------------------------------------

    /**
     * Otvara formu za dodavanje novog plovila zadatog tipa na odabrani terminal, i ako je
     * plovilo uspješno sačuvano, pamti njegov IMO broj kao dodat tokom simulacije.
     */
    private void dodajPloviloTokomSimulacije() {
        TipPlovila tip = (TipPlovila) tipCombo.getSelectedItem();
        Terminal odabraniTerminal = (Terminal) terminalCombo.getSelectedItem();

        PlovilaFormaDijalog dijalog = new PlovilaFormaDijalog(this, luka, odabraniTerminal, tip, null,
                (l, t, kandidat) -> KlijentskaSimulacijaService.dodajTokomSimulacije(l, kandidat));
        dijalog.setVisible(true);

        if (dijalog.jeSacuvano()) {
            imoDodataTokomSimulacije.add(dijalog.getSacuvaniImo());
        }
    }

    // ------------------------------------------------------------------
    // Korak 5 — kraj simulacije i serijalizacija
    // ------------------------------------------------------------------

    /**
     * Zaustavlja živi prikaz, onemogućava dalje dodavanje plovila, i u pozadinskoj niti
     * serijalizuje stanje luke, a nakon toga obavještava korisnika da je simulacija završena.
     */
    private void zavrsiSimulaciju() {
        timer.stop();
        dodajDugme.setEnabled(false);
        tipCombo.setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                SerializationUtil.serijalizujStanjeLuke(luka);
                return null;
            }

            @Override
            protected void done() {
                prikaziPoruku("Simulacija je završena. Stanje luke je sačuvano u luka.ser.",
                        "Kraj simulacije", JOptionPane.INFORMATION_MESSAGE);
            }
        }.execute();
    }

    /**
     * Zatvara prozor, prethodno zaustavljajući sve tajmere koje je pokrenuo.
     */
    // Zaustavlja sve javax.swing.Timer instance koje ovaj prozor može pokrenuti — bez ovoga,
    // pokreniSimulaciju()/oznaciZaOdlazak() zakazani tajmeri (render, jednokratni "odgoda" prije
    // označavanja za odlazak, i ponavljajući "raspored" koji redom zove zatraziNapustanje()) nastave
    // da tiču na Swing-ovoj dijeljenoj niti i nakon što je ovaj prozor zatvoren/odbačen — dispose()
    // ih ne dira jer nisu vezani za životni ciklus prozora. U testovima to znači da niti pokrenute
    // za jedan test mogu, mnogo kasnije, tokom sasvim drugog testa, izazvati stvaran odlazak i
    // naplatu za plovilo iz prvog — otud se ova kuka mora pozvati u @AfterEach prije dispose().
    @Override
    public void dispose() {
        zaustaviSveTajmereZaTest();
        super.dispose();
    }

    /**
     * Zaustavlja render tajmer i oba tajmera vezana za odlazak plovila, ako su pokrenuti.
     */
    void zaustaviSveTajmereZaTest() {
        if (timer != null) {
            timer.stop();
        }
        if (odgodaTimer != null) {
            odgodaTimer.stop();
        }
        if (rasporedTimer != null) {
            rasporedTimer.stop();
        }
    }

    /**
     * Prikazuje poruku korisniku u dijalogu.
     *
     * @param poruka Tekst poruke.
     * @param naslov Naslov dijaloga.
     * @param tip Tip poruke ({@link JOptionPane} konstanta, npr. {@code ERROR_MESSAGE}).
     */
    // Paket-privatna, override-abilna kuka za sve JOptionPane dijaloge (samodovoljna, ne oslanja se na spoljni kod):
    // JOptionPane.showMessageDialog() pumpa svoju UGNIJEŽDENU petlju događaja i blokira dok se
    // dijalog ne zatvori — u pravoj upotrebi to radi jer korisnik klikne "OK" na EDT-u, ali pozvano
    // direktno sa test niti (bez EDT-a, bez ijednog klika) blokira ZAUVIJEK. Testovi preklapaju ovu
    // metodu (potklasa u istom paketu) umjesto da se oslanjaju na stvaran modalni dijalog.
    void prikaziPoruku(String poruka, String naslov, int tip) {
        JOptionPane.showMessageDialog(this, poruka, naslov, tip);
    }

    /**
     * Postavlja tekst polja za minimalan broj plovila po terminalu, paket-privatna vidljivost
     * radi direktnog testiranja, isti obrazac kao PlovilaFormaDijalog.
     *
     * @param vrijednost Tekst koji se postavlja u polje.
     */
    void postaviMinimumZaTest(String vrijednost) {
        minimumPolje.setText(vrijednost);
    }

    /**
     * Poziva {@link #pokreniSimulaciju()} direktno, bez potrebe za simuliranjem klika na dugme.
     */
    void pokreniSimulacijuZaTest() {
        pokreniSimulaciju();
    }

    /**
     * Omogućava dobijanje trenutnog stanja luke.
     *
     * @return Trenutna luka.
     */
    Luka getLukaZaTest() {
        return luka;
    }

    /**
     * Omogućava dobijanje skupa IMO brojeva plovila označenih za odlazak.
     *
     * @return Skup IMO brojeva.
     */
    Set<String> getImoZaOdlazakZaTest() {
        return imoZaOdlazak;
    }

    /**
     * Omogućava dobijanje skupa IMO brojeva plovila dodatih tokom simulacije.
     *
     * @return Skup IMO brojeva.
     */
    Set<String> getImoDodataTokomSimulacijeZaTest() {
        return imoDodataTokomSimulacije;
    }

    /**
     * Provjerava da li je simulacija označena kao završena.
     *
     * @return {@code true} ako je simulacija završena.
     */
    boolean jeSimulacijaZavrsenaZaTest() {
        return simulacijaZavrsena;
    }

    /**
     * Poziva {@link #tik()} direktno, umjesto čekanja na sljedeći okidaj tajmera.
     */
    void tikZaTest() {
        tik();
    }

    /**
     * Provjerava da li je dugme za dodavanje plovila trenutno omogućeno.
     *
     * @return {@code true} ako je dugme omogućeno.
     */
    boolean jeDodajDugmeOmoguceno() {
        return dodajDugme.isEnabled();
    }

    /**
     * Postavlja odabrani terminal u padajućem meniju.
     *
     * @param t Terminal koji treba odabrati.
     */
    void postaviOdabraniTerminalZaTest(Terminal t) {
        terminalCombo.setSelectedItem(t);
    }

    /**
     * Omogućava dobijanje tekstualnog polja koje prikazuje stanje terminala.
     *
     * @return Polje sa prikazom terminala.
     */
    JTextArea getPrikazPoljeZaTest() {
        return prikazPolje;
    }
}
