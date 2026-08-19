package org.unibl.etf.pj2.luka.gui;

import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.util.GeneratorPlovila;
import org.unibl.etf.pj2.luka.util.PropertiesUtil;
import org.unibl.etf.pj2.luka.util.SerializationUtil;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Glavni prozor administratorske aplikacije: prikazuje sadržaj svakog terminala u tabeli, i
 * omogućava dodavanje, izmjenu i brisanje plovila, preuzimanje CSV izvještaja o taksama, i
 * pokretanje klijentske aplikacije.
 *
 * @author Milan Šobot
 * @version 1.0
 * @see KlijentskiProzor
 */
public class AdminProzor extends JFrame {

    /** Trenutno stanje luke koje ovaj prozor prikazuje i kojim upravlja. */
    private Luka luka;

    /** Padajući meni za izbor tipa plovila koje se dodaje. */
    private final JComboBox<TipPlovila> tipCombo = new JComboBox<>(TipPlovila.values());

    /** Padajući meni za izbor terminala čiji se sadržaj trenutno prikazuje u tabeli. */
    private final JComboBox<Terminal> terminalCombo = new JComboBox<>();

    /** Model tabele sa sadržajem odabranog terminala, sa svim ćelijama neuredivim direktno u tabeli. */
    private final DefaultTableModel tabelaModel = new DefaultTableModel(PregledTerminalaService.ZAGLAVLJA, 0) {
        @Override
        public boolean isCellEditable(int red, int kolona) {
            return false;
        }
    };

    /** Tabela koja prikazuje sadržaj odabranog terminala. */
    private final JTable tabela = new JTable(tabelaModel);

    /**
     * Kreira administratorski prozor i odmah pokreće učitavanje postojećeg stanja luke.
     */
    public AdminProzor() {
        super("Administratorska aplikacija");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 500);
        setLocationRelativeTo(null);

        izgradiUI();
        ucitajStanje();
    }

    /**
     * Sastavlja i raspoređuje sve komponente prozora: gornju traku sa izborom terminala/tipa i
     * dugmadima za dodavanje/izmjenu/brisanje, tabelu u sredini, i donju traku sa preuzimanjem
     * izvještaja i pokretanjem klijentske aplikacije.
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
        terminalCombo.addItemListener(e -> {
            if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                osvjeziTabelu();
            }
        });

        JButton dodajDugme = new JButton("Dodaj plovilo");
        dodajDugme.addActionListener(e -> dodajAkcija());

        JButton izmijeniDugme = new JButton("Izmijeni");
        izmijeniDugme.addActionListener(e -> izmijeniAkciju());

        JButton obrisiDugme = new JButton("Izbriši");
        obrisiDugme.addActionListener(e -> obrisiAkciju());

        JPanel gornjaTraka = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gornjaTraka.add(new javax.swing.JLabel("Terminal:"));
        gornjaTraka.add(terminalCombo);
        gornjaTraka.add(new javax.swing.JLabel("Tip plovila:"));
        gornjaTraka.add(tipCombo);
        gornjaTraka.add(dodajDugme);
        gornjaTraka.add(izmijeniDugme);
        gornjaTraka.add(obrisiDugme);

        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    izmijeniAkciju();
                }
            }
        });

        JButton preuzmiCsvDugme = new JButton("Preuzmi CSV izvještaj");
        preuzmiCsvDugme.addActionListener(e -> preuzmiCsvIzvjestaj());

        JButton pokreniDugme = new JButton("Pokreni klijentsku aplikaciju");
        pokreniDugme.addActionListener(e -> pokreniKlijentskuAplikaciju());
        JPanel donjaTraka = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        donjaTraka.add(preuzmiCsvDugme);
        donjaTraka.add(pokreniDugme);

        setLayout(new BorderLayout());
        add(gornjaTraka, BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        add(donjaTraka, BorderLayout.SOUTH);
    }

    /**
     * Učitava stanje luke sa diska u pozadinskoj niti (a ako nikad nije sačuvano, gradi praznu
     * luku sa brojem terminala iz konfiguracije), pa nakon učitavanja popunjava padajući meni
     * terminala i tabelu.
     */
    private void ucitajStanje() {
        new SwingWorker<Luka, Void>() {
            @Override
            protected Luka doInBackground() {
                Luka postojeca = SerializationUtil.ucitajStanjeLuke();
                if (postojeca == null) {
                    int brojTerminala = PropertiesUtil.getBrojTerminala();
                    List<Terminal> terminali = new ArrayList<>();
                    for (int i = 0; i < brojTerminala; i++) {
                        terminali.add(new Terminal(i));
                    }
                    postojeca = new Luka(terminali, new HashMap<>());
                }
                GeneratorPlovila.obezbijediJedinstvenostImoZa(postojeca);
                return postojeca;
            }

            @Override
            protected void done() {
                try {
                    luka = get();
                } catch (Exception ex) {
                    throw new IllegalStateException("Neuspjesno ucitavanje stanja luke.", ex);
                }
                DefaultComboBoxModel<Terminal> model = new DefaultComboBoxModel<>();
                for (Terminal t : luka.getTerminali()) {
                    model.addElement(t);
                }
                terminalCombo.setModel(model);
                if (terminalCombo.getItemCount() > 0) {
                    terminalCombo.setSelectedIndex(0);
                }
                osvjeziTabelu();
            }
        }.execute();
    }

    /**
     * Ponovo puni tabelu sadržajem trenutno odabranog terminala.
     */
    private void osvjeziTabelu() {
        tabelaModel.setRowCount(0);
        Terminal t = (Terminal) terminalCombo.getSelectedItem();
        if (t == null) {
            return;
        }
        for (String[] red : PregledTerminalaService.redovi(t)) {
            tabelaModel.addRow(red);
        }
    }

    /**
     * Omogućava dobijanje trenutno odabranog terminala u padajućem meniju.
     *
     * @return Odabrani terminal, ili {@code null} ako ništa nije odabrano.
     */
    private Terminal odabraniTerminal() {
        return (Terminal) terminalCombo.getSelectedItem();
    }

    /**
     * Omogućava dobijanje IMO broja plovila iz trenutno odabranog reda tabele.
     *
     * @return IMO broj odabranog plovila, ili {@code null} ako nijedan red nije odabran.
     */
    private String odabraniImo() {
        int red = tabela.getSelectedRow();
        if (red < 0) {
            return null;
        }
        return (String) tabelaModel.getValueAt(red, 0);
    }

    /**
     * Otvara formu za dodavanje novog plovila odabranog tipa na odabrani terminal, i osvježava
     * tabelu ako je plovilo uspješno sačuvano.
     */
    private void dodajAkcija() {
        Terminal t = odabraniTerminal();
        if (t == null || luka == null) {
            return;
        }
        TipPlovila tip = (TipPlovila) tipCombo.getSelectedItem();
        PlovilaFormaDijalog dijalog = new PlovilaFormaDijalog(this, luka, t, tip, null);
        dijalog.setVisible(true);
        if (dijalog.jeSacuvano()) {
            osvjeziTabelu();
        }
    }

    /**
     * Otvara formu (unaprijed popunjenu postojećim podacima) za izmjenu plovila odabranog u tabeli, i
     * osvježava tabelu ako je izmjena uspješno sačuvana.
     */
    private void izmijeniAkciju() {
        Terminal t = odabraniTerminal();
        String imo = odabraniImo();
        if (t == null || imo == null) {
            return;
        }
        Plovilo postojece = PregledTerminalaService.pronadjiPlovilo(t, imo);
        if (postojece == null) {
            return;
        }
        TipPlovila tip = TipPlovila.odObjekta(postojece);
        PlovilaFormaDijalog dijalog = new PlovilaFormaDijalog(this, luka, t, tip, postojece);
        dijalog.setVisible(true);
        if (dijalog.jeSacuvano()) {
            osvjeziTabelu();
        }
    }

    /**
     * Traži potvrdu, pa nakon nje briše plovilo odabrano u tabeli i osvježava prikaz.
     */
    private void obrisiAkciju() {
        Terminal t = odabraniTerminal();
        String imo = odabraniImo();
        if (t == null || imo == null) {
            return;
        }
        int potvrda = JOptionPane.showConfirmDialog(this, "Obrisati plovilo " + imo + "?",
                "Potvrda brisanja", JOptionPane.YES_NO_OPTION);
        if (potvrda == JOptionPane.YES_OPTION) {
            UredjivanjePlovilaService.obrisiPlovilo(t, imo);
            osvjeziTabelu();
        }
    }

    /**
     * Otvara dijalog za izbor odredišta, pa u pozadinskoj niti kopira CSV izvještaj tamo. Ako
     * izvještaj još ne postoji, samo obavještava korisnika bez otvaranja dijaloga za izbor
     * odredišta.
     */
    private void preuzmiCsvIzvjestaj() {
        if (!IzvjestajService.izvjestajPostoji()) {
            JOptionPane.showMessageDialog(this, "Nema još evidentiranih taksi.",
                    "Preuzimanje CSV izvještaja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        FileDialog fd = new FileDialog(this, "Sačuvaj CSV izvještaj kao...", FileDialog.SAVE);
        fd.setFile("takse.csv");
        fd.setVisible(true);
        if (fd.getFile() == null) {
            return;
        }
        File odrediste = new File(fd.getDirectory(), fd.getFile()).getAbsoluteFile();

        new SwingWorker<Void, Void>() {
            private IOException greska;

            @Override
            protected Void doInBackground() {
                try {
                    IzvjestajService.preuzmiIzvjestaj(odrediste);
                } catch (IOException ioe) {
                    greska = ioe;
                }
                return null;
            }

            @Override
            protected void done() {
                if (greska != null) {
                    JOptionPane.showMessageDialog(AdminProzor.this,
                            "Greška pri preuzimanju CSV izvještaja: " + greska.getMessage(),
                            "Greška", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /**
     * Serijalizuje trenutno stanje luke u pozadinskoj niti, pa nakon uspješnog upisa otvara
     * klijentski prozor.
     */
    private void pokreniKlijentskuAplikaciju() {
        if (luka == null) {
            return;
        }
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                SerializationUtil.serijalizujStanjeLuke(luka);
                return null;
            }

            @Override
            protected void done() {
                napraviKlijentskiProzor().setVisible(true);
            }
        }.execute();
    }

    /**
     * Kreira klijentski prozor nad trenutnim stanjem luke, i registruje osluškivač koji, čim se
     * klijentski prozor zatvori, ponovo učitava stanje luke sa diska.
     *
     * @return Novokreirani, još nevidljivi klijentski prozor.
     */
    // Klijent radi nad SOPSTVENOM kopijom luke (KlijentskiProzor.pokreniSimulaciju() zamjenjuje
    // svoje polje luka rezultatom PokretacSimulacije.pripremiPocetnoStanje(), admin i klijent od
    // tog trenutka gledaju DVA RAZLIČITA objekta). Admin-ovo polje luka bi bez ovog listenera
    // ostalo zauvijek zastarjelo (postavljeno samo jednom, u ucitajStanje()), sljedeći klik na
    // "Pokreni klijentsku aplikaciju" bi tim zastarjelim objektom prepisao ono što je klijent na
    // kraju simulacije upisao, vraćajući otišla plovila i njihove STARE zapise u
    // evidencijaUlaska (otud apsurdne takse pri sljedećoj naplati). Zato admin
    // ponovo učitava luka.ser sa diska čim se klijentski prozor zatvori, umjesto da nastavi da
    // vjeruje sopstvenom, potencijalno zastarjelom stanju u memoriji.
    KlijentskiProzor napraviKlijentskiProzor() {
        KlijentskiProzor klijent = new KlijentskiProzor(luka);
        klijent.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                ucitajStanje();
            }
        });
        return klijent;
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
     * Ulazna tačka administratorske aplikacije: otvara administratorski prozor na niti za
     * događaje korisničkog interfejsa.
     *
     * @param args Argumenti komandne linije, trenutno se ne koriste.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminProzor().setVisible(true));
    }
}
