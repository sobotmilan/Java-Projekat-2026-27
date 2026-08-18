package org.unibl.etf.pj2.luka.main;
import org.unibl.etf.pj2.luka.gui.AdminProzor;
import javax.swing.SwingUtilities;
/**
 * Ulazna tačka čitave aplikacije
 *
 * <p>Iako je administratorska GUI aplikacija tehnički stvarna ulazna tačka,
 * za svrhu jednostavnosti i jasnoće kreacija Admin GUI prozora je izvršena iz main() metode.</p>
 *
 *
 * @author Milan Šobot
 * @version 1.0
 */


public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminProzor().setVisible(true));
    }
}