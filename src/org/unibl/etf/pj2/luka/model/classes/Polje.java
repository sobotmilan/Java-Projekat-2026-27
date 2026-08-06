package org.unibl.etf.pj2.luka.model.classes;

import java.io.Serial;
import java.io.Serializable;

/**
 * Jedna ćelija matrice {@link Terminal}-a (4×17).
 *
 *
 * <p>Nosi svoju poziciju, tekstualnu oznaku za prikaz (kanal, dok, prazno polje...) i referencu na objekat klase {@link Plovilo} koje trenutno zauzima ćeliju.
 * {@link Dok} ne nasljeđuje ovu klasu nego je samo sadrži - jedan {@code Polje} objekat je tako
 * istovremeno i pozicija u matrici i mjesto gdje se čuva zauzetost tog veza.</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see Terminal
 * @see Dok
 */
public class Polje implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** X koordinata polja u matrici terminala. */
    private final int x;

    /** Y koordinata polja u matrici terminala. */
    private final int y;

    /** Tekstualna oznaka polja korištena pri ispisu terminala (npr. [K]anal, [D]ok, prazno polje). */
    private String oznaka;

    /** Plovilo koje trenutno zauzima ovo polje, odnosno {@code null} ako je polje slobodno. */
    private Plovilo trenutnoPlovilo;

    /**
     * Kreira polje na zadatim koordinatama sa zadatom oznakom i (opciono) plovilom koje se tu trenutno nalazi.
     *
     * @param x X koordinata polja.
     * @param y Y koordinata polja.
     * @param oznaka Tekstualna oznaka polja.
     * @param trenutnoPlovilo Plovilo koje zauzima polje, tj. {@code null} ako je slobodno.
     */
    public Polje(int x, int y, String oznaka, Plovilo trenutnoPlovilo) {
        this.x = x;
        this.y = y;
        this.oznaka = oznaka;
        this.trenutnoPlovilo = trenutnoPlovilo;
    }

    /**
     * Getterska metoda za dobijanje X koordinate polja.
     *
     * @return X koordinata polja.
     */
    public int getX() {
        return x;
    }

    /**
     * Getterska metoda za dobijanje Y koordinate polja.
     *
     * @return Y koordinata polja.
     */
    public int getY() {
        return y;
    }

    /**
     *Getterska metoda za dobijanje tekstualne oznake polja.
     *
     * @return Oznaka polja.
     */
    public String getOznaka() {
        return oznaka;
    }

    /**
     * Setterska metoda koja omogućava postavljanje tekstualne oznake polja.
     *
     * @param oznaka Nova oznaka polja.
     */
    public void setOznaka(String oznaka) {
        this.oznaka = oznaka;
    }

    /**
     * Getterska metoda za dobijanje referenca na objekat klase {@link Plovilo} koji se trenutno nalazi na ovom polju.
     *
     * @return Plovilo na polju, tj. {@code null} ako je polje slobodno.
     */
    public Plovilo getTrenutnoPlovilo() {
        return trenutnoPlovilo;
    }

    /**
     * Setterska metoda za postavljanje reference na objekat klase {@link Plovilo} koje se trenutno nalazi ovom polju.
     *
     * @param trenutnoPlovilo Plovilo koje se postavlja na polje, ili {@code null} da se polje oslobodi.
     */
    public void setTrenutnoPlovilo(Plovilo trenutnoPlovilo) {
        this.trenutnoPlovilo = trenutnoPlovilo;
    }

    /**
     * Redefinisana metoda {@code toString()} koja vraća oznaku polja, korištena pri
     * tekstualnom ispisivanju terminala.
     *
     * @return Oznaka polja.
     */
    @Override
    public String toString() {
        return oznaka;
    }
}
