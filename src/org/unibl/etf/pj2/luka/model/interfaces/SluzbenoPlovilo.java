package org.unibl.etf.pj2.luka.model.interfaces;

/**
 * Zajednički interfejs za sva državna (službena) plovila koja mogu uključiti rotaciju.
 *
 *
 * <p>Omogućava da se rotacija čita i postavlja polimorfno, bez obzira na konkretan tip plovila
 * (obalska straža, carina ili vatrogasci), umjesto da svaka klasa zasebno duplira isto polje i njen getter/setter.</p>
 *
 * @author Milan Šobot
 * @version 1.0
 */
public interface SluzbenoPlovilo {
    /**
     * Provjerava da li plovilo trenutno ima uključenu rotaciju. Upaljena rotacija daje prioritet
     * pri kretanju kroz plovne kanale terminala.
     *
     * @return {@code true} ako je rotacija aktivna, u suprotnom {@code false}.
     */
    boolean isRotacija();

    /**
     * Omogućava postavljanje rotacije na {@code true} ukoliko je rotacija aktivna, odnosno
     * {@code false} ako je rotacija ugašena.
     *
     * @param rotacija Vrijednost na koju se postavlja stanje rotacije plovila.
     */
    void setRotacija(boolean rotacija);
}
