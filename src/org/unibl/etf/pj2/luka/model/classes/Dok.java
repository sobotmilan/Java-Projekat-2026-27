package org.unibl.etf.pj2.luka.model.classes;

import java.io.Serializable;
/**
 * Vez (dok) unutar jednog {@link Terminal}-a, na koji se može privezati jedno {@link Plovilo}.
 *
 * <p>Dok ne nasljeđuje {@link Polje} nego ga sadrži: pozicija veza u matrici {@link Terminal}-a i
 * podatak o tome koje je plovilo trenutno privezano čuvaju se u istom objektu {@link Polje},
 * pa se zauzetost veza i zauzetost matrice ne mogu razići.</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see Terminal
 */
public class Dok implements Serializable {

    /** Polje u matrici {@link Terminal}-a na kojem se vez nalazi. */
    private final Polje lokacija;

    /** Redni broj veza unutar {@link Terminal}-a, jedinstven u okviru tog {@link Terminal}-a. */
    private final int oznakaVezova;

    /**
     * Kreira objekat tipa Dok (vez) na poziciji zadatoj objektom tipa {@link Polje} proslijeđenom kao prvi parametar poziva konstruktora.
     *
     * @param lokacija Polje matrice terminala na kojem se vez nalazi.
     * @param oznakaVezova Redni broj veza unutar terminala.
     */
    public Dok(Polje lokacija, int oznakaVezova) {
        this.lokacija = lokacija;
        this.oznakaVezova = oznakaVezova;
    }

    /**
     *
     * Getter funkcija koja vraća objekat klase {@link Polje} koji predstavlja polje matrice {@link Terminal}-a na kojem se vez nalazi.
     *
     * @return Objekat klase {@link Polje} koji predstavlja polje matrice {@link Terminal}-a na kojem se vez nalazi.
     */
    public Polje getLokacija() {
        return lokacija;
    }

    /**
     *
     * Getter funkcija koja vraća redni broj veza unutar {@link Terminal}-a.
     *
     * @return Redni broj veza unutar {@link Terminal}-a.
     */
    public int getOznakaVezova() {
        return oznakaVezova;
    }

    /**
     *
     * Getter funkcija koja vraća logički podatak u zavisnosti od prisustva objekta klase {@link Plovilo} na vezu.
     *
     * @return {@code true} ako na vezu trenutno nije privezano nijedno plovilo, u suprotnom {@code false}.
     */
    public boolean isSlobodan() {
        return lokacija.getTrenutnoPlovilo() == null;
    }
}
