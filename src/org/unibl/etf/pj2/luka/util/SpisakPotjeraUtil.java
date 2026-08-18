package org.unibl.etf.pj2.luka.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Pomoćna klasa namijenjena za učitavanje spiska IMO brojeva plovila za kojima je raspisana potjernica.
 *
 * @author Milan Šobot
 * @version 1.0
 */
public final class SpisakPotjeraUtil {

    /**
     * Mapa koja kao ključeve koristi apstraktne putanje do (tekstualnih) datoteka koje predstavljaju spiskove IMO brojeva plovila za kojima je raspisana potjernica,
     * dok vrijednosti pridružene tim ključevima jesu Set objekti koji se sastoje od bar jednog String objekta koji predstavljaju IMO brojeve vozila za kojima je raspisana potjernica,
     * a koji su učitani sa putanje predstavljene odgovarajućim ključem.
     *
     */
    private static final Map<String, Set<String>> kesirano = new HashMap<>();

    /**
     * Klasa sadrži striktno metode/atribute klase,
     * premda konstruktor je deklarisan kao privatan
     * kako bi se redefinisalo ponašanje JVM-a pri kreaciji implicitnog podrazumijevanog konstruktora.
     */
    private SpisakPotjeraUtil() {
    }

    /**
     *
     * Na osnovu proslijeđene reference na objekat klase File, metoda vrši učitavanje spiska potjera sa datoteke reprezentovane apstraktnom putanjom
     * sadržanom u parametru metode. Nakon toga, vrši se upis novih IMO brojeva u mapu keširanih IMO brojeva.
     *
     * @param fajl Referenca na objekat klase File koji čuva apstraktnu putanju do (tekstualne) datoteke koja sadrži spisak IMO brojeva za kojima je raspisana potjernica.
     * @return Set koji sadrži sve IMO brojeve pročitane iz zadate datoteke.
     */
    public static synchronized Set<String> ucitaj(File fajl) {
        if (fajl == null) {
            return Set.of();
        }
        String kljuc = fajl.getPath();
        Set<String> postojece = kesirano.get(kljuc);
        if (postojece != null) {
            return postojece;
        }

        Set<String> rezultat = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fajl))) {
            String linija;
            while ((linija = br.readLine()) != null) {
                String trim = linija.trim();
                if (trim.isEmpty() || trim.startsWith("#")) {
                    continue;
                }
                rezultat.add(trim);
            }
        } catch (IOException e) {
            LoggerUtil.logWarning("Ne moze se ucitati spisak potjera iz " + fajl.getPath()
                    + ", koristi se prazan spisak.");
            rezultat = new HashSet<>();
        }

        rezultat = Set.copyOf(rezultat);
        kesirano.put(kljuc, rezultat);
        return rezultat;
    }

    /**
     * Čisti keširane IMO brojeve.
     */
    public static synchronized void resetujKes() {
        kesirano.clear();
    }
}
