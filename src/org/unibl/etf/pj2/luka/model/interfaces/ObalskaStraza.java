package org.unibl.etf.pj2.luka.model.interfaces;

import java.io.File;
/**
 * Interfejs namijenjen za oznacavanje svake klase nasljednice klase Plovilo kao klase koja predstavlja plovilo koje je u upotrebi od strane obalske straže.
 *
 * @author Milan Šobot
 * @version 1.0
 */
public interface ObalskaStraza {
    /**
     * Metoda koja vraća referencu na objekat klase File koja sadrži apstraktnu putanju do datoteke u kojoj je sačuvan spisak IMO brojeva svih polovila za kojima je raspisana međunarodna potjernica.
     *
     * @author Milan Šobot
     * @return File objekat koji čuva apstraktnu putanju do datoteke unutar koje je sačuvan spisak IMO brojeva svih plovila za kojima je raspisana potjernica. Ako spisak nije dodijeljen, ova metoda moze vratiti null.
     */
    File getSpisakPotjera();
}
