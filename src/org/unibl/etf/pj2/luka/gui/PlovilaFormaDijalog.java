package org.unibl.etf.pj2.luka.gui;

import org.unibl.etf.pj2.luka.model.classes.KontejnerskiBrod;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.PutnickiKruzer;
import org.unibl.etf.pj2.luka.model.classes.Tanker;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.model.interfaces.ObalskaStraza;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlovilaFormaDijalog extends JDialog {

    private final Luka luka;
    private final Terminal terminal;
    private final TipPlovila tip;
    private final Plovilo postojece;

    private final JTextField nazivPolje = new JTextField();
    private final JTextField imoPolje = new JTextField();
    private final JTextField brojMotoraPolje = new JTextField();
    private final JTextField registarskiBrojPolje = new JTextField();
    private final JTextField fotografijaPrikaz = new JTextField();
    private final JTextField specificnoPolje = new JTextField();
    private final JTextField spisakPotjeraPrikaz = new JTextField();

    private File fotografija;
    private File spisakPotjera;
    private boolean sacuvano = false;

    public PlovilaFormaDijalog(Frame vlasnik, Luka luka, Terminal terminal, TipPlovila tip, Plovilo postojece) {
        super(vlasnik, postojece == null ? "Dodaj plovilo" : "Izmijeni plovilo", true);
        this.luka = luka;
        this.terminal = terminal;
        this.tip = tip;
        this.postojece = postojece;

        izgradiUI();
        if (postojece != null) {
            popuniPostojecimVrijednostima();
        }

        pack();
        setLocationRelativeTo(vlasnik);
    }

    public boolean jeSacuvano() {
        return sacuvano;
    }

    private void izgradiUI() {
        fotografijaPrikaz.setEditable(false);
        spisakPotjeraPrikaz.setEditable(false);

        JPanel polja = new JPanel(new GridLayout(0, 2, 6, 6));

        polja.add(new JLabel("Tip:"));
        polja.add(new JLabel(tip.getNaziv()));

        polja.add(new JLabel("Naziv:"));
        polja.add(nazivPolje);

        polja.add(new JLabel("IMO broj:"));
        polja.add(imoPolje);

        polja.add(new JLabel("Broj motora:"));
        polja.add(brojMotoraPolje);

        polja.add(new JLabel("Registarski broj:"));
        polja.add(registarskiBrojPolje);

        polja.add(new JLabel("Fotografija:"));
        polja.add(redSaDugmetom(fotografijaPrikaz, "Izaberi...", e -> izaberiFotografiju()));

        polja.add(new JLabel(tip.getNazivSpecificnogPolja() + ":"));
        polja.add(specificnoPolje);

        if (tip.zahtijevaSpisakPotjera()) {
            polja.add(new JLabel("Spisak potjera:"));
            polja.add(redSaDugmetom(spisakPotjeraPrikaz, "Izaberi...", e -> izaberiSpisakPotjera()));
        }

        JButton sacuvajDugme = new JButton("Sačuvaj");
        sacuvajDugme.addActionListener(e -> pokusajSacuvaj());

        JButton otkaziDugme = new JButton("Otkaži");
        otkaziDugme.addActionListener(e -> dispose());

        JPanel dugmad = new JPanel();
        dugmad.add(sacuvajDugme);
        dugmad.add(otkaziDugme);

        setLayout(new BorderLayout(8, 8));
        add(polja, BorderLayout.CENTER);
        add(dugmad, BorderLayout.SOUTH);
    }

    private JPanel redSaDugmetom(JTextField prikaz, String natpisDugmeta, java.awt.event.ActionListener akcija) {
        JPanel red = new JPanel(new BorderLayout(4, 0));
        red.add(prikaz, BorderLayout.CENTER);
        JButton dugme = new JButton(natpisDugmeta);
        dugme.addActionListener(akcija);
        red.add(dugme, BorderLayout.EAST);
        return red;
    }

    private void izaberiFotografiju() {
        FileDialog fd = new FileDialog(this, "Izaberi fotografiju", FileDialog.LOAD);
        fd.setVisible(true);
        if (fd.getFile() != null) {
            fotografija = new File(fd.getDirectory(), fd.getFile()).getAbsoluteFile();
            fotografijaPrikaz.setText(fotografija.getAbsolutePath());
        }
    }

    private void izaberiSpisakPotjera() {
        FileDialog fd = new FileDialog(this, "Izaberi spisak potjera", FileDialog.LOAD);
        fd.setVisible(true);
        if (fd.getFile() != null) {
            spisakPotjera = new File(fd.getDirectory(), fd.getFile()).getAbsoluteFile();
            spisakPotjeraPrikaz.setText(spisakPotjera.getAbsolutePath());
        }
    }

    private void popuniPostojecimVrijednostima() {
        nazivPolje.setText(postojece.getNaziv());
        imoPolje.setText(postojece.getImoBroj());
        brojMotoraPolje.setText(postojece.getBrojMotora());
        registarskiBrojPolje.setText(postojece.getRegistarskiBroj());

        fotografija = postojece.getFotografija();
        if (fotografija != null) {
            fotografijaPrikaz.setText(fotografija.getAbsolutePath());
        }

        specificnoPolje.setText(specificnaVrijednostZa(postojece));

        if (tip.zahtijevaSpisakPotjera() && postojece instanceof ObalskaStraza os) {
            spisakPotjera = os.getSpisakPotjera();
            if (spisakPotjera != null) {
                spisakPotjeraPrikaz.setText(spisakPotjera.getAbsolutePath());
            }
        }
    }

    private String specificnaVrijednostZa(Plovilo p) {
        if (p instanceof KontejnerskiBrod kb) {
            return String.valueOf(kb.getKapacitetTEU());
        }
        if (p instanceof PutnickiKruzer pk) {
            return String.valueOf(pk.getBrojPutnika());
        }
        if (p instanceof Tanker t) {
            return String.format(Locale.US, "%.2f", t.getZapreminaBarel());
        }
        return "";
    }

    private void pokusajSacuvaj() {
        List<String> greske = new ArrayList<>();

        double specificnaVrijednost = 0;
        String tekst = specificnoPolje.getText().trim();
        if (tekst.isBlank()) {
            greske.add(tip.getNazivSpecificnogPolja() + " ne smije biti prazno.");
        } else {
            try {
                specificnaVrijednost = Double.parseDouble(tekst);
            } catch (NumberFormatException ex) {
                greske.add(tip.getNazivSpecificnogPolja() + " mora biti broj.");
            }
        }

        Plovilo kandidat = PlovilaFabrika.napravi(tip, nazivPolje.getText().trim(),
                imoPolje.getText().trim(), brojMotoraPolje.getText().trim(),
                registarskiBrojPolje.getText().trim(), fotografija,
                specificnaVrijednost, spisakPotjera);

        greske.addAll(postojece == null
                ? UredjivanjePlovilaService.dodajPlovilo(luka, terminal, kandidat)
                : UredjivanjePlovilaService.izmijeniPlovilo(luka, terminal,
                postojece.getImoBroj(), kandidat));

        if (!greske.isEmpty()) {
            JOptionPane.showMessageDialog(this, String.join("\n", greske),
                    "Greška", JOptionPane.ERROR_MESSAGE);
            return;
        }
        sacuvano = true;
        dispose();
    }
}
