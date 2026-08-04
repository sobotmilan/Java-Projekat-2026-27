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

public final class PrikazTerminala {

    private PrikazTerminala() {
    }

    public static String[][] render(Terminal terminal) {
        synchronized (terminal) {
            Polje[][] matrica = terminal.getMatrica();
            String[][] prikaz = new String[matrica.length][matrica[0].length];
            for (int red = 0; red < matrica.length; red++) {
                for (int kolona = 0; kolona < matrica[red].length; kolona++) {
                    prikaz[red][kolona] = oznakaZaPolje(matrica[red][kolona], red, kolona);
                }
            }
            return prikaz;
        }
    }

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
