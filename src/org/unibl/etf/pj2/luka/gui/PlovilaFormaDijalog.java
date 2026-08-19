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

/**
 * Jedinstvena forma za unos podataka o plovilu, sa poljima koja se dinamički prikazuju u
 * zavisnosti od izabranog tipa plovila, korištena i za dodavanje novog i za izmjenu postojećeg
 * plovila.
 *
 * @author Milan Šobot
 * @version 1.0
 * @see TipPlovila
 * @see PlovilaValidator
 */
public class PlovilaFormaDijalog extends JDialog {

    /**
     * Način na koji se novi kandidat "dodaje" u luku, zavisi od konteksta u kojem je forma
     * otvorena (miran administratorski unos ili živa simulacija).
     */
    // Kako se kandidat "dodaje" zavisi od konteksta u kojem je forma otvorena: admin dodaje
    // direktno u matricu (UredjivanjePlovilaService.dodajPlovilo — simulacija ne radi, bezbjedno),
    // a klijent tokom žive simulacije mora ići kroz punu BrodThread navigaciju —
    // vidi KlijentskaSimulacijaService.dodajTokomSimulacije(). Izmjena (postojece != null) uvijek
    // ide kroz UredjivanjePlovilaService.izmijeniPlovilo(), bez obzira na kontekst — izmjena
    // postojećeg plovila na terminalu ne pokreće novu nit.
    @FunctionalInterface
    interface DodavanjeStrategija {
        /**
         * Pokušava dodati kandidata u luku na zadatom terminalu.
         *
         * @param luka Luka u koju se plovilo dodaje.
         * @param terminal Terminal na koji se plovilo dodaje.
         * @param kandidat Kandidat koji se dodaje.
         * @return Prazna lista ako je dodavanje uspjelo, inače lista opisa grešaka.
         */
        List<String> dodaj(Luka luka, Terminal terminal, Plovilo kandidat);
    }

    /** Luka kojoj terminal na koji se plovilo dodaje ili izmjenjuje pripada. */
    private final Luka luka;
    /** Terminal na koji se plovilo dodaje ili na kojem se izmjenjuje. */
    private final Terminal terminal;
    /** Tip plovila koje se dodaje, ili tip postojećeg plovila koje se izmjenjuje. */
    private final TipPlovila tip;
    /** Plovilo koje se izmjenjuje, ili {@code null} ako se dodaje novo. */
    private final Plovilo postojece;
    /** Strategija dodavanja kandidata, zavisna od konteksta u kojem je forma otvorena. */
    private final DodavanjeStrategija dodavanjeStrategija;

    /** Polje za unos naziva plovila. */
    private final JTextField nazivPolje = new JTextField();
    /** Polje za unos IMO broja plovila. */
    private final JTextField imoPolje = new JTextField();
    /** Polje za unos broja motora plovila. */
    private final JTextField brojMotoraPolje = new JTextField();
    /** Polje za unos registarske oznake plovila. */
    private final JTextField registarskiBrojPolje = new JTextField();
    /** Neuredivo polje koje prikazuje putanju do izabrane fotografije. */
    private final JTextField fotografijaPrikaz = new JTextField();
    /** Polje za unos vrijednosti specifične za trup plovila (kapacitet, broj putnika ili zapremina). */
    private final JTextField specificnoPolje = new JTextField();
    /** Neuredivo polje koje prikazuje putanju do izabranog spiska potjera. */
    private final JTextField spisakPotjeraPrikaz = new JTextField();
    /** Checkbox za uključivanje rotacije, prikazan samo za tipove plovila koji pripadaju nekoj državnoj službi. */
    private final JCheckBox rotacijaCheckbox;

    /** Putanja do izabrane fotografije plovila. */
    private File fotografija;
    /** Putanja do izabranog spiska potjera, relevantno samo za plovila obalske straže. */
    private File spisakPotjera;
    /** Postavlja se na {@code true} kad je plovilo uspješno sačuvano (dodato ili izmijenjeno). */
    private boolean sacuvano = false;

    /**
     * Kreira formu za dodavanje novog ili izmjenu postojećeg plovila, koristeći podrazumijevanu
     * strategiju dodavanja (miran administratorski unos direktno u matricu terminala).
     *
     * @param vlasnik Prozor vlasnik ovog modalnog dijaloga.
     * @param luka Luka kojoj terminal pripada.
     * @param terminal Terminal na koji se plovilo dodaje ili na kojem se izmjenjuje.
     * @param tip Tip plovila koje se dodaje, ili tip postojećeg plovila koje se izmjenjuje.
     * @param postojece Plovilo koje se izmjenjuje, ili {@code null} za dodavanje novog.
     */
    public PlovilaFormaDijalog(Frame vlasnik, Luka luka, Terminal terminal, TipPlovila tip, Plovilo postojece) {
        this(vlasnik, luka, terminal, tip, postojece, UredjivanjePlovilaService::dodajPlovilo);
    }

    /**
     * Kreira formu za dodavanje novog ili izmjenu postojećeg plovila, sa zadatom strategijom
     * dodavanja.
     *
     * @param vlasnik Prozor vlasnik ovog modalnog dijaloga.
     * @param luka Luka kojoj terminal pripada.
     * @param terminal Terminal na koji se plovilo dodaje ili na kojem se izmjenjuje.
     * @param tip Tip plovila koje se dodaje, ili tip postojećeg plovila koje se izmjenjuje.
     * @param postojece Plovilo koje se izmjenjuje, ili {@code null} za dodavanje novog.
     * @param dodavanjeStrategija Strategija kojom se novi kandidat dodaje u luku.
     */
    PlovilaFormaDijalog(Frame vlasnik, Luka luka, Terminal terminal, TipPlovila tip, Plovilo postojece,
                        DodavanjeStrategija dodavanjeStrategija) {
        super(vlasnik, postojece == null ? "Dodaj plovilo" : "Izmijeni plovilo", true);
        this.luka = luka;
        this.terminal = terminal;
        this.tip = tip;
        this.postojece = postojece;
        this.dodavanjeStrategija = dodavanjeStrategija;
        this.rotacijaCheckbox = tip.getSluzba() != null ? new JCheckBox("Rotacija uključena") : null;

        izgradiUI();
        if (postojece != null) {
            popuniPostojecimVrijednostima();
        }

        pack();
        setLocationRelativeTo(vlasnik);
    }

    /**
     * Provjerava da li je plovilo uspješno sačuvano (dodato ili izmijenjeno) prije zatvaranja
     * dijaloga.
     *
     * @return {@code true} ako je sačuvano.
     */
    public boolean jeSacuvano() {
        return sacuvano;
    }

    /**
     * Omogućava dobijanje IMO broja sačuvanog plovila.
     *
     * @return IMO broj unesen u polje za IMO.
     */
    public String getSacuvaniImo() {
        return imoPolje.getText().trim();
    }

    /**
     * Sastavlja i raspoređuje sva polja forme, prilagođena tipu plovila (specifično polje trupa,
     * spisak potjera samo za obalsku stražu, checkbox rotacije samo za državna plovila), i dugmad
     * za čuvanje/otkazivanje.
     */
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

    /**
     * Sastavlja jedan red forme koji se sastoji od neuredivog polja za prikaz putanje i dugmeta
     * koje otvara dijalog za izbor fajla.
     *
     * @param prikaz Polje koje prikazuje izabranu putanju.
     * @param natpisDugmeta Tekst na dugmetu.
     * @param akcija Radnja koju dugme pokreće.
     * @return Sastavljeni red forme.
     */
    private JPanel redSaDugmetom(JTextField prikaz, String natpisDugmeta, java.awt.event.ActionListener akcija) {
        JPanel red = new JPanel(new BorderLayout(4, 0));
        red.add(prikaz, BorderLayout.CENTER);
        JButton dugme = new JButton(natpisDugmeta);
        dugme.addActionListener(akcija);
        red.add(dugme, BorderLayout.EAST);
        return red;
    }

    /**
     * Otvara dijalog za izbor fotografije i, ako je fajl izabran, postavlja njegovu apsolutnu
     * putanju u {@link #fotografija} i prikazuje je u {@link #fotografijaPrikaz}.
     */
    private void izaberiFotografiju() {
        FileDialog fd = new FileDialog(this, "Izaberi fotografiju", FileDialog.LOAD);
        fd.setVisible(true);
        if (fd.getFile() != null) {
            fotografija = new File(fd.getDirectory(), fd.getFile()).getAbsoluteFile();
            fotografijaPrikaz.setText(fotografija.getAbsolutePath());
        }
    }

    /**
     * Otvara dijalog za izbor spiska potjera i, ako je fajl izabran, postavlja njegovu apsolutnu
     * putanju u {@link #spisakPotjera} i prikazuje je u {@link #spisakPotjeraPrikaz}.
     */
    private void izaberiSpisakPotjera() {
        FileDialog fd = new FileDialog(this, "Izaberi spisak potjera", FileDialog.LOAD);
        fd.setVisible(true);
        if (fd.getFile() != null) {
            spisakPotjera = new File(fd.getDirectory(), fd.getFile()).getAbsoluteFile();
            spisakPotjeraPrikaz.setText(spisakPotjera.getAbsolutePath());
        }
    }

    /**
     * Predpopunjava sva polja forme vrijednostima iz {@link #postojece}, korišteno pri otvaranju
     * forme za izmjenu postojećeg plovila.
     */
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

    /**
     * Formatira vrijednost polja specifičnog za trup plovila kao tekst pogodan za predpopunjavanje
     * forme.
     *
     * @param p Plovilo čiji se specifičan atribut formatira.
     * @return Tekstualni prikaz specifičnog atributa, ili prazan tekst ako trup nije prepoznat.
     */
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

    /**
     * Provjerava da li ova forma prikazuje checkbox rotacije, tj. da li tip plovila pripada
     * nekoj državnoj službi.
     *
     * @return {@code true} ako je checkbox rotacije prisutan.
     */
    // Paket-privatna vidljivost radi direktnog testiranja checkbox-a rotacije, bez potrebe za
    // stvarnim klikanjem kroz UI (isti obrazac kao provjeriSudar()/primijeniPauzu()).
    boolean imaRotacijuCheckbox() {
        return rotacijaCheckbox != null;
    }

    /**
     * Postavlja stanje checkbox-a rotacije, bez efekta ako checkbox nije prisutan.
     *
     * @param vrijednost Nova vrijednost checkbox-a.
     */
    void postaviRotacijuZaTest(boolean vrijednost) {
        if (rotacijaCheckbox != null) {
            rotacijaCheckbox.setSelected(vrijednost);
        }
    }

    /**
     * Provjerava da li je checkbox rotacije trenutno označen.
     *
     * @return {@code true} ako je checkbox prisutan i označen.
     */
    boolean jeRotacijaOznacenaZaTest() {
        return rotacijaCheckbox != null && rotacijaCheckbox.isSelected();
    }

    /**
     * Direktno popunjava sva polja forme zadatim vrijednostima, bez potrebe za stvarnim
     * kucanjem/klikanjem kroz korisnički interfejs.
     *
     * @param naziv Naziv plovila.
     * @param imo IMO broj plovila.
     * @param brojMotora Broj motora plovila.
     * @param registarskiBroj Registarska oznaka plovila.
     * @param foto Putanja do fotografije plovila.
     * @param specificnoTekst Tekst za polje specifično za trup plovila.
     * @param spisak Putanja do spiska potjera.
     */
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

    /**
     * Pokušava sačuvati kandidata sastavljenog od trenutnih vrijednosti polja forme: parsira
     * specifično polje trupa, validira kandidata, pa ga dodaje ili izmjenjuje preko odgovarajuće
     * strategije. Ako bilo koji korak prijavi grešku, prikazuje je korisniku i ostavlja dijalog
     * otvorenim; u suprotnom označava plovilo kao sačuvano i zatvara dijalog.
     */
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
                ? dodavanjeStrategija.dodaj(luka, terminal, kandidat)
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
