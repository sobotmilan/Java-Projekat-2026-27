package org.unibl.etf.pj2.luka.gui;

import org.unibl.etf.pj2.luka.util.PokretacIzvjestaja;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class IzvjestajService {

    private IzvjestajService() {
    }

    public static boolean izvjestajPostoji() {
        return PokretacIzvjestaja.getPutanjaCsv().exists();
    }

    public static void preuzmiIzvjestaj(File odrediste) throws IOException {
        Files.copy(PokretacIzvjestaja.getPutanjaCsv().toPath(), odrediste.toPath(),
                StandardCopyOption.REPLACE_EXISTING);
    }
}
