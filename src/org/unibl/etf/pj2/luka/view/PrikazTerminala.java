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
 * Pretvara stanje {@link Terminal}-a u tekstualni prikaz
 *
 * <p>prazan dok je označen sa {@code *},
 * plovila sa slovom koje označava tip plovila ({@code K}/{@code P}/{@code T}) (NAPOMENA: ovakva reprezentacija NE UZIMA u obzir da li se radi o plovilu javnih službi),
 * a {@code R} se koristi za plovila sa upaljenom rotacijom.</p>
 *
 * <p>Identitet službe pobjeđuje tip plovila:
 * npr. {@code TankerVatrogasci} pod rotacijom ispisuje {@code VR}, ne {@code T}.</p>
 *
 * <p>Ovaj paket je namjerno odvojen od {@code simulation} jer nema nikakvu funkcionalnu ulogu u nitima i konkurentnim elementima programa,
 * samo transformiše trenutno stanje {@link Terminal}-a u tekst za prikaz u GUI-ju.</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see Terminal
 */
public final class PrikazTerminala {

    /**
     * Klasa je sadrzana striktno od metoda klase, ne posjeduje nijednu metodu/atribut instance,
     * premda je konstruktor deklarisan kao privatan i ostavljen praznog tijela.
     */
    private PrikazTerminala() {
    }

    /**
     * Kreira (renderuje) snimak matrice terminala kao matricu tekstualnih oznaka, jedno polje matrice za svaku
     * ćeliju. Zaključan sa {@code synchronized (terminal)}, tj. isti ključ koji koriste niti brodova
     * pri pomjeranju, kako render ne bi uhvatio polovičan potez broda/brodova (pola ažurirane matrice).
     *
     * @param terminal Terminal čiji se snimak pravi.
     * @return Matrica tekstualnih oznaka, istih dimenzija kao {@link Terminal#getMatrica()}.
     */
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

    
    /**
     * Formatira rezultat metode {@link #render(Terminal)} kao jedan {@link String} pogodan za ispis na
     * konzoli, svako polje poravnato u koloni širine 3 karaktera, redovi razdvojeni sistemskim prelomom reda.
     *
     * @param terminal Terminal koji se prikazuje.
     * @return Tekstualni prikaz terminala.
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

    /**
     * Određuje oznaku za jedno polje matrice: ako je plovilo prisutno na tom polju,
     * stavlja se njegova oznaka po tipu (i stanju rotaciji, ako je ima naravno).
     * Inače, oznaka praznog polja.
     *
     * @param polje Polje čija se oznaka određuje.
     * @param red Red polja u matrici.
     * @param kolona Kolona polja u matrici.
     * @return Tekstualna oznaka za prikaz polja.
     */
    private static String oznakaZaPolje(Polje polje, int red, int kolona) {

        Plovilo plovilo = polje.getTrenutnoPlovilo();

        if (plovilo != null) {
            return oznakaPlovila(plovilo);
        }

        return praznaOznaka(red, kolona);
    }

    /**
     * Određuje oznaku praznog polja:
     * prazan dok je {@code *},
     * ulazna/izlazna kolona je označena strelicama u zavisnosti od smjera ({@code v}/{@code ^}),
     * a prazno polje kanala sa {@code .}.
     *
     * @param red Red polja u matrici.
     * @param kolona Kolona polja u matrici.
     * @return Tekstualna oznaka praznog polja.
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
     * Određuje oznaku plovila:
     * identitet službe ({@code V}atrogasci/{@code O}balska straža/{@code C}arina)
     * ima prednost nad tipom plovila ({@code K}ontejnerski brod/{@code P}utnički kruzer/{@code T}anker),
     * a {@code R} se dodaje ako je plovilo trenutno pod rotacijom (podrazumijeva se da ga posjeduje).
     *
     * @param plovilo Plovilo čija se oznaka određuje.
     * @return Tekstualna oznaka plovila (jedno ili dva slova).
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
