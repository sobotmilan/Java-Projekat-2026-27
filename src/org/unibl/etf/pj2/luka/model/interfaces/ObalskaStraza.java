package org.unibl.etf.pj2.luka.model.interfaces;

import java.io.File;
/**
 * Interfejs namijenjen za označavanje svake klase nasljednice klase Plovilo kao klase koja predstavlja tip plovila koje je u upotrebi od strane obalske straže.
 * Nasljeđuje {@link SluzbenoPlovilo} jer je obalska straža državna služba i njena plovila mogu imati upaljenu rotaciju koja je definisana ugovorom interfejsa {@link SluzbenoPlovilo}.
 *
 * @author Milan Šobot
 * @version 1.0
 */
public interface ObalskaStraza extends SluzbenoPlovilo {
    /**
     * Metoda koja vraća referencu na objekat klase {@link  File} koja sadrži apstraktnu putanju do datoteke u kojoj je sačuvan spisak IMO brojeva svih plovila za kojima je raspisana međunarodna potjernica.
     *
     * @return File objekat koji čuva apstraktnu putanju do datoteke unutar koje je sačuvan spisak IMO brojeva svih plovila za kojima je raspisana potjernica. Ako spisak nije dodijeljen, ova metoda može vratiti null.
     *
     */
    File getSpisakPotjera();
}
