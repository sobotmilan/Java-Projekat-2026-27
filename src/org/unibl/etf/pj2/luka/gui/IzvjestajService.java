package org.unibl.etf.pj2.luka.gui;

import org.unibl.etf.pj2.luka.util.PokretacIzvjestaja;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Omogućava administratoru da preuzme CSV izvještaj o naplaćenim lučkim taksama na proizvoljno
 * odabranu lokaciju.
 *
 * @author Milan Šobot
 * @version 1.0
 * @see PokretacIzvjestaja
 */
public final class IzvjestajService {

    private IzvjestajService() {
    }

    /**
     * Provjerava da li CSV izvještaj trenutno postoji na disku.
     *
     * @return {@code true} ako je fajl izvještaja prisutan.
     */
    public static boolean izvjestajPostoji() {
        return PokretacIzvjestaja.getPutanjaCsv().exists();
    }

    /**
     * Kopira trenutni CSV izvještaj na zadatu lokaciju, prepisujući postojeći fajl ako već
     * postoji.
     *
     * @param odrediste Lokacija na koju se izvještaj kopira.
     * @throws FileNotFoundException Ako CSV izvještaj još ne postoji.
     * @throws IOException Ako kopiranje ne uspije iz nekog drugog razloga.
     */
    public static void preuzmiIzvjestaj(File odrediste) throws IOException {
        File izvor = PokretacIzvjestaja.getPutanjaCsv();
        if (!izvor.exists()) {
            throw new FileNotFoundException("CSV izvještaj (" + izvor.getPath() + ") još ne postoji.");
        }
        Files.copy(izvor.toPath(), odrediste.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
