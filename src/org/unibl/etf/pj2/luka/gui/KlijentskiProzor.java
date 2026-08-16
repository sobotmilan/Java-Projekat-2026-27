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

public class KlijentskiProzor extends JFrame {

    private static final int RAZMAK_ODLAZAKA_MS = 3000;

    private Luka luka;

    private final JTextField minimumPolje = new JTextField("1", 4);
    private final JButton pokreniDugme = new JButton("Pokreni simulaciju");

    private final JComboBox<Terminal> terminalCombo = new JComboBox<>();
    private final JTextArea prikazPolje = new JTextArea();
    private final JLabel slobodniVezoviLabel = new JLabel(" ");

    private final JComboBox<TipPlovila> tipCombo = new JComboBox<>(TipPlovila.values());
    private final JButton dodajDugme = new JButton("Dodaj plovilo");

    private final Set<String> imoZaOdlazak = new HashSet<>();
    private final Set<String> imoDodataTokomSimulacije = new HashSet<>();

    private Timer timer;
    private boolean simulacijaZavrsena = false;
    private boolean odlasciOznaceni = false;

    public KlijentskiProzor(Luka luka) {
        super("Klijentska aplikacija");
        this.luka = luka;
        setSize(800, 550);
        setLocationRelativeTo(null);

        izgradiUI();
    }

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

                Timer odgoda = new Timer(3000, e -> oznaciZaOdlazak(sveNiti));
                odgoda.setRepeats(false);
                odgoda.start();
            }
        }.execute();
    }

    // ------------------------------------------------------------------
    // Korak 3 — odlazak 15% plovila po terminalu (C7)
    // ------------------------------------------------------------------

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
        Timer raspored = new Timer(RAZMAK_ODLAZAKA_MS, null);
        raspored.addActionListener(e -> {
            if (it.hasNext()) {
                it.next().zatraziNapustanje();
            } else {
                raspored.stop();
            }
        });
        raspored.start();
    }

    // ------------------------------------------------------------------
    // Korak 2 — živi prikaz (C5)
    // ------------------------------------------------------------------

    private void zapocniZiviPrikaz() {
        timer = new Timer((int) PokretacSimulacije.INTERVAL_RENDEROVANJA_MS, e -> tik());
        timer.start();
    }

    private void tik() {
        osvjeziPrikaz();
        if (odlasciOznaceni && !simulacijaZavrsena
                && KlijentskaSimulacijaService.jeSimulacijaZavrsena(luka, imoZaOdlazak, imoDodataTokomSimulacije)) {
            simulacijaZavrsena = true;
            zavrsiSimulaciju();
        }
    }

    // ------------------------------------------------------------------
    // Korak 4 — dodavanje plovila tokom simulacije (C8/C9)
    // ------------------------------------------------------------------

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
    // Korak 5 — kraj simulacije i serijalizacija (E1/E2)
    // ------------------------------------------------------------------

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

    // Paket-privatna, override-abilna kuka za sve JOptionPane dijaloge (K10-stil samodovoljnosti):
    // JOptionPane.showMessageDialog() pumpa svoju UGNIJEŽDENU petlju događaja i blokira dok se
    // dijalog ne zatvori — u pravoj upotrebi to radi jer korisnik klikne "OK" na EDT-u, ali pozvano
    // direktno sa test niti (bez EDT-a, bez ijednog klika) blokira ZAUVIJEK. Testovi preklapaju ovu
    // metodu (potklasa u istom paketu) umjesto da se oslanjaju na stvaran modalni dijalog.
    void prikaziPoruku(String poruka, String naslov, int tip) {
        JOptionPane.showMessageDialog(this, poruka, naslov, tip);
    }

    // Paket-privatna vidljivost radi direktnog testiranja, isti obrazac kao PlovilaFormaDijalog.
    void postaviMinimumZaTest(String vrijednost) {
        minimumPolje.setText(vrijednost);
    }

    void pokreniSimulacijuZaTest() {
        pokreniSimulaciju();
    }

    Luka getLukaZaTest() {
        return luka;
    }

    Set<String> getImoZaOdlazakZaTest() {
        return imoZaOdlazak;
    }

    Set<String> getImoDodataTokomSimulacijeZaTest() {
        return imoDodataTokomSimulacije;
    }

    boolean jeSimulacijaZavrsenaZaTest() {
        return simulacijaZavrsena;
    }

    void tikZaTest() {
        tik();
    }

    boolean jeDodajDugmeOmoguceno() {
        return dodajDugme.isEnabled();
    }

    void postaviOdabraniTerminalZaTest(Terminal t) {
        terminalCombo.setSelectedItem(t);
    }

    JTextArea getPrikazPoljeZaTest() {
        return prikazPolje;
    }
}
