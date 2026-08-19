package org.unibl.etf.pj2.luka.main;

import org.unibl.etf.pj2.luka.gui.AdminProzor;

import javax.swing.SwingUtilities;

/**
 * Ulazna tačka čitave aplikacije.
 *
 * <p>Podrazumijevano se pokreće administratorski
 * dio aplikacije, sa dugmetom prema klijentskom dijelu.</p>
 *
 * @author Milan Šobot
 * @version 1.0
 */
public class Main {

    /**
     * Pokreće administratorsku aplikaciju na Swing niti za obradu događaja.
     *
     * @param args Argumenti komandne linije; ne koriste se.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminProzor().setVisible(true));
    }
}