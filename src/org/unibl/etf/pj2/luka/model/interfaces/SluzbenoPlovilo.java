package org.unibl.etf.pj2.luka.model.interfaces;

/**
 * Zajednički interfejs za sva državna (službena) plovila koja mogu uključiti rotaciju (sirenu).
 * Omogućava da se rotacija čita i postavlja polimorfno, bez obzira na konkretan tip plovila
 * (obalska straža, carina ili vatrogasci), umjesto da svaka klasa zasebno duplira ista polje i metode.
 *
 * @author Milan Šobot
 * @version 1.0
 */
public interface SluzbenoPlovilo {
    boolean isRotacija();
    void setRotacija(boolean rotacija);
}
