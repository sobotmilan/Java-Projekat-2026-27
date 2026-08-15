package org.unibl.etf.pj2.luka.gui;

import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.view.PrikazTerminala;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;

// Namjerno samo statički snimak (bez niti/auto-osvježavanja) — live prikaz je C5, budući zadatak.
public class KlijentskiProzor extends JFrame {

    private final Luka luka;
    private final JComboBox<Terminal> terminalCombo = new JComboBox<>();
    private final JTextArea prikazPolje = new JTextArea();

    public KlijentskiProzor(Luka luka) {
        super("Klijentska aplikacija — pregled luke");
        this.luka = luka;
        setSize(700, 500);
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

        DefaultComboBoxModel<Terminal> model = new DefaultComboBoxModel<>();
        for (Terminal t : luka.getTerminali()) {
            model.addElement(t);
        }
        terminalCombo.setModel(model);
        terminalCombo.addItemListener(e -> {
            if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                osvjeziPrikaz();
            }
        });

        prikazPolje.setEditable(false);
        prikazPolje.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        JPanel gornjaTraka = new JPanel();
        gornjaTraka.add(new javax.swing.JLabel("Terminal:"));
        gornjaTraka.add(terminalCombo);

        setLayout(new BorderLayout());
        add(gornjaTraka, BorderLayout.NORTH);
        add(new JScrollPane(prikazPolje), BorderLayout.CENTER);

        if (terminalCombo.getItemCount() > 0) {
            terminalCombo.setSelectedIndex(0);
            osvjeziPrikaz();
        }
    }

    private void osvjeziPrikaz() {
        Terminal t = (Terminal) terminalCombo.getSelectedItem();
        if (t == null) {
            prikazPolje.setText("");
            return;
        }
        prikazPolje.setText(PrikazTerminala.renderAsText(t));
    }
}
