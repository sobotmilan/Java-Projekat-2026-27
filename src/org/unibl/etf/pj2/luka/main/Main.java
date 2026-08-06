package org.unibl.etf.pj2.luka.main;

import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.util.SerializationUtil;

/**
 * Ulazna tačka aplikacije.
 *
 * <p><b>Napomena:</b> ovo je trenutno skelet zadužen samo za pokušaj učitavanja prethodnog
 * stanja luke iz {@code luka.ser} preko {@link SerializationUtil#ucitajStanjeLuke()}. Stvarno
 * pokretanje administratorske (A1) i korisničke (C5) GUI aplikacije, kao i wireovanje
 * {@link org.unibl.etf.pj2.luka.simulation.PokretacSimulacije}-a na ovu tačku, još nije
 * implementirano.
 *
 * @author Milan Šobot
 * @version 1.0
 */
public class Main {
    /**
     * Pokušava učitati postojeće stanje luke sa diska; ako ne postoji ({@code luka == null}),
     * podrazumijeva se prvo pokretanje aplikacije.
     *
     * @param args Argumenti komandne linije (trenutno se ne koriste).
     */
    public static void main(String[] args) {
        Luka luka = SerializationUtil.ucitajStanjeLuke();
        if(luka == null) {
            // calls into the creation of a new Luka object which will be assigned to luka.
        }

        // calls into main methods for working with Luka luka.

    }
}