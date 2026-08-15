package org.unibl.etf.pj2.luka.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SpisakPotjeraUtil {

    private static final Map<String, Set<String>> kesirano = new HashMap<>();

    private SpisakPotjeraUtil() {
    }

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

    public static synchronized void resetujKes() {
        kesirano.clear();
    }
}
