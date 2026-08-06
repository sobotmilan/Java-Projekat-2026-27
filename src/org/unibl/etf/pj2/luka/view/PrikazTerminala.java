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
 * Pretvara stanje {@link Terminal}-a u tekstualni prikaz (C6) — prazan dok {@code *}, slovo po
 * tipu plovila ({@code K}/{@code P}/{@code T}), {@code R} dodano za plovila pod rotacijom.
 *
 * <p>Identitet službe pobjeđuje tip trupa: provjera ide {@code Vatrogasci} → {@code ObalskaStraza}
 * → {@code Carina} (kroz {@link SluzbenoPlovilo}/markerske interfejse) prije pada na tip trupa,
 * pa npr. {@code TankerVatrogasci} pod rotacijom ispisuje {@code VR}, ne {@code T}.</p>
 *
 * <p>Ovaj paket je namjerno odvojen od {@code simulation} — nema ulogu u nitima, samo transformiše
 * model u tekst/matricu za prikaz u GUI-ju (C5, još TODO).</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see Terminal
 */
public final class PrikazTerminala {

    private PrikazTerminala() {
    }

    /**
     * Pravi snimak matrice terminala kao matricu tekstualnih oznaka, po jedno polje matrice po
     * ćeliji. Zaključan sa {@code synchronized (terminal)} — isti ključ koji koriste niti brodova
     * pri pomjeranju — kako render ne bi uhvatio polovičan potez (pola ažurirane matrice).
     *
     * <p><b>Napomena o životnom ciklusu:</b> ovo je jedini razlog zašto parkirano čekanje
     * ({@code BrodThread} u stanju {@code PRIVEZAN}) mora koristiti poseban lock objekat umjesto
     * {@code synchronized (terminal)} — inače bi {@code wait()} unutar te sinhronizacije zamrzao
     * ovu metodu (a time i GUI) za trajanje čekanja privezanog plovila (D4).</p>
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
     * Formatira {@link #render(Terminal)} rezultat kao jedan {@link String} pogodan za ispis na
     * konzoli — svako polje poravnato u koloni širine 3 karaktera, redovi razdvojeni sistemskim
     * prelomom reda.
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
     * Određuje oznaku za jedno polje matrice: ako je plovilo prisutno, njegova oznaka po tipu
     * (i rotaciji); inače oznaka praznog polja.
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
     * Određuje oznaku praznog (nezauzetog) polja: ulazna/izlazna kolona zadržava svoje strelice
     * ({@code v}/{@code ^}), prazan dok je {@code *}, a prazno polje kanala {@code .}.
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
     * Određuje oznaku plovila: identitet službe ({@code V}/{@code O}/{@code C}) ima prednost nad
     * tipom trupa ({@code K}/{@code P}/{@code T}), a {@code R} se dodaje ako je plovilo trenutno
     * pod rotacijom (C6).
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
