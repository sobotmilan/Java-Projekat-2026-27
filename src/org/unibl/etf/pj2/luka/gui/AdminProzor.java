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
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AdminProzor extends JFrame {

    private Luka luka;

    private final JComboBox<TipPlovila> tipCombo = new JComboBox<>(TipPlovila.values());
    private final JComboBox<Terminal> terminalCombo = new JComboBox<>();
    private final DefaultTableModel tabelaModel = new DefaultTableModel(PregledTerminalaService.ZAGLAVLJA, 0) {
        @Override
        public boolean isCellEditable(int red, int kolona) {
            return false;
        }
    };
    private final JTable tabela = new JTable(tabelaModel);

    public AdminProzor() {
        super("Administratorska aplikacija");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 500);
        setLocationRelativeTo(null);

        izgradiUI();
        ucitajStanje();
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
        terminalCombo.addItemListener(e -> {
            if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                osvjeziTabelu();
            }
        });

        JButton dodajDugme = new JButton("Dodaj plovilo");
        dodajDugme.addActionListener(e -> dodajAkcija());

        JButton izmijeniDugme = new JButton("Izmijeni");
        izmijeniDugme.addActionListener(e -> izmijeniAkciju());

        JButton obrisiDugme = new JButton("Obriši");
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

        JButton pokreniDugme = new JButton("Pokreni klijentsku aplikaciju");
        pokreniDugme.addActionListener(e -> pokreniKlijentskuAplikaciju());
        JPanel donjaTraka = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        donjaTraka.add(pokreniDugme);

        setLayout(new BorderLayout());
        add(gornjaTraka, BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        add(donjaTraka, BorderLayout.SOUTH);
    }

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
                    throw new IllegalStateException("Neuspješno učitavanje stanja luke.", ex);
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

    private Terminal odabraniTerminal() {
        return (Terminal) terminalCombo.getSelectedItem();
    }

    private String odabraniImo() {
        int red = tabela.getSelectedRow();
        if (red < 0) {
            return null;
        }
        return (String) tabelaModel.getValueAt(red, 0);
    }

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
                new KlijentskiProzor(luka).setVisible(true);
            }
        }.execute();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminProzor().setVisible(true));
    }
}
