package org.unibl.etf.pj2.luka.gui;

import org.unibl.etf.pj2.luka.model.classes.KontejnerskiBrod;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.PutnickiKruzer;
import org.unibl.etf.pj2.luka.model.classes.Tanker;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.model.interfaces.ObalskaStraza;
import org.unibl.etf.pj2.luka.model.interfaces.SluzbenoPlovilo;

import javax.swing.JButton;
import javax.swing.JCheckBox;
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
    private final JCheckBox rotacijaCheckbox;

    private File fotografija;
    private File spisakPotjera;
    private boolean sacuvano = false;

    public PlovilaFormaDijalog(Frame vlasnik, Luka luka, Terminal terminal, TipPlovila tip, Plovilo postojece) {
        super(vlasnik, postojece == null ? "Dodaj plovilo" : "Izmijeni plovilo", true);
        this.luka = luka;
        this.terminal = terminal;
        this.tip = tip;
        this.postojece = postojece;
        this.rotacijaCheckbox = tip.getSluzba() != null ? new JCheckBox("Rotacija uključena") : null;

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

        if (rotacijaCheckbox != null) {
            polja.add(new JLabel("Rotacija:"));
            polja.add(rotacijaCheckbox);
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

        if (rotacijaCheckbox != null && postojece instanceof SluzbenoPlovilo sluzbeno) {
            rotacijaCheckbox.setSelected(sluzbeno.isRotacija());
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

    // Paket-privatna vidljivost radi direktnog testiranja checkbox-a rotacije, bez potrebe za
    // stvarnim klikanjem kroz UI (isti obrazac kao provjeriSudar()/primijeniPauzu()).
    boolean imaRotacijuCheckbox() {
        return rotacijaCheckbox != null;
    }

    void postaviRotacijuZaTest(boolean vrijednost) {
        if (rotacijaCheckbox != null) {
            rotacijaCheckbox.setSelected(vrijednost);
        }
    }

    boolean jeRotacijaOznacenaZaTest() {
        return rotacijaCheckbox != null && rotacijaCheckbox.isSelected();
    }

    void popuniZaTest(String naziv, String imo, String brojMotora, String registarskiBroj,
                       File foto, String specificnoTekst, File spisak) {
        nazivPolje.setText(naziv);
        imoPolje.setText(imo);
        brojMotoraPolje.setText(brojMotora);
        registarskiBrojPolje.setText(registarskiBroj);
        fotografija = foto;
        specificnoPolje.setText(specificnoTekst);
        spisakPotjera = spisak;
    }

    void pokusajSacuvaj() {
        List<String> greske = new ArrayList<>();

        double specificnaVrijednost = 0;
        String tekst = specificnoPolje.getText().trim();
        if (tekst.isBlank()) {
            greske.add("Polje " + tip.getNazivSpecificnogPolja() + " ne smije biti prazno.");
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

        if (rotacijaCheckbox != null && kandidat instanceof SluzbenoPlovilo sluzbeno) {
            sluzbeno.setRotacija(rotacijaCheckbox.isSelected());
        }

        String stariImo = postojece == null ? null : postojece.getImoBroj();
        greske.addAll(PlovilaValidator.validiraj(luka, kandidat, stariImo));

        if (!greske.isEmpty()) {
            JOptionPane.showMessageDialog(this, String.join("\n", greske),
                    "Greška", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<String> rezultat = postojece == null
                ? UredjivanjePlovilaService.dodajPlovilo(luka, terminal, kandidat)
                : UredjivanjePlovilaService.izmijeniPlovilo(luka, terminal, stariImo, kandidat,
                rotacijaCheckbox != null);

        if (!rezultat.isEmpty()) {
            JOptionPane.showMessageDialog(this, String.join("\n", rezultat),
                    "Greška", JOptionPane.ERROR_MESSAGE);
            return;
        }

        sacuvano = true;
        dispose();
    }
}
