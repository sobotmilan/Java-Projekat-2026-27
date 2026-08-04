package org.unibl.etf.pj2.luka.view;

import org.unibl.etf.pj2.luka.model.classes.KontejnerskiBrod;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Polje;
import org.unibl.etf.pj2.luka.model.classes.PutnickiKruzer;
import org.unibl.etf.pj2.luka.model.classes.Tanker;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.model.interfaces.Carina;
import org.unibl.etf.pj2.luka.model.interfaces.ObalskaStraza;
import org.unibl.etf.pj2.luka.model.interfaces.SluzbenoPlovilo;
import org.unibl.etf.pj2.luka.model.interfaces.Vatrogasci;

/**
 * Pretvara stanje {@link Terminal}-a (matricu polja) u tekstualni prikaz za korisnički GUI (C6).
 * Nema ulogu u simulaciji — samo čita trenutno stanje, zato je odvojena od paketa
 * {@code simulation}.
 *
 * @author Milan Šobot
 * @version 1.0
 */
public final class PrikazTerminala {

    private PrikazTerminala() {
    }

    /**
     * Snima trenutno stanje terminala u matricu oznaka spremnu za prikaz (4x17).
     * Identitet službe pobjeđuje tip trupa — vatrogasno plovilo na tankeru se prikazuje
     * kao {@code V}, ne {@code T} (vidi {@link #oznakaPlovila(Plovilo)}).
     *
     * @param terminal Terminal čije se stanje prikazuje.
     * @return Matrica 4x17 oznaka; svako polje je jedan od: {@code *}, {@code .}, {@code v},
     * {@code ^}, oznaka plovila (npr. {@code K}, {@code VR}).
     */
    public static String[][] render(Terminal terminal) {
        String[][] prikaz = new String[4][17];
        synchronized (terminal) {
            Polje[][] matrica = terminal.getMatrica();
            for (int red = 0; red < matrica.length; red++) {
                for (int kolona = 0; kolona < matrica[red].length; kolona++) {
                    prikaz[red][kolona] = oznakaZaPolje(matrica[red][kolona], red, kolona);
                }
            }
        }
        return prikaz;
    }

    /**
     * Isto kao {@link #render(Terminal)}, ali formatirano kao jedan string spreman za ispis
     * u konzoli — kolone su poravnate fiksnom širinom.
     *
     * @param terminal Terminal čije se stanje prikazuje.
     * @return Tekstualni prikaz terminala, jedan red matrice po liniji.
     */
    public static String renderAsText(Terminal terminal) {
        String[][] prikaz = render(terminal);
        StringBuilder sb = new StringBuilder();
        for (String[] red : prikaz) {
            for (String polje : red) {
                sb.append(String.format("%-3s", polje));
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    private static String oznakaZaPolje(Polje polje, int red, int kolona) {
        Plovilo plovilo = polje.getTrenutnoPlovilo();
        if (plovilo != null) {
            return oznakaPlovila(plovilo);
        }
        return praznaOznaka(red, kolona);
    }

    /**
     * Oznaka praznog polja po njegovoj poziciji: {@code v}/{@code ^} za ulaznu/izlaznu
     * kolonu (0 i 1), {@code *} za prazan dok (redovi 0 i 3), {@code .} za praznu vodu
     * plovnog kanala (redovi 1 i 2, kolone 2-16).
     */
    private static String praznaOznaka(int red, int kolona) {
        if (kolona == 0) {
            return "v";
        }
        if (kolona == 1) {
            return "^";
        }
        if (red == 0 || red == 3) {
            return "*";
        }
        return ".";
    }

    /**
     * Oznaka zauzetog polja: identitet službe (vatrogasci &gt; obalska straža &gt; carina)
     * pobjeđuje tip trupa, jer je to ono što je bezbjednosno relevantno za korisnika. Tip
     * trupa (K/P/T) je samo rezervni slučaj za komercijalna plovila koja ne pripadaju
     * nijednoj službi. Upaljena rotacija dodaje sufiks {@code R}.
     */
    private static String oznakaPlovila(Plovilo plovilo) {
        String slovo;
        if (plovilo instanceof Vatrogasci) {
            slovo = "V";
        } else if (plovilo instanceof ObalskaStraza) {
            slovo = "O";
        } else if (plovilo instanceof Carina) {
            slovo = "C";
        } else if (plovilo instanceof KontejnerskiBrod) {
            slovo = "K";
        } else if (plovilo instanceof PutnickiKruzer) {
            slovo = "P";
        } else if (plovilo instanceof Tanker) {
            slovo = "T";
        } else {
            slovo = "?";
        }

        if (plovilo instanceof SluzbenoPlovilo sluzbeno && sluzbeno.isRotacija()) {
            slovo += "R";
        }
        return slovo;
    }
}
