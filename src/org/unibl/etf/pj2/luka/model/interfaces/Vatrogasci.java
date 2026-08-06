package org.unibl.etf.pj2.luka.model.interfaces;

/**
 * Markerski interfejs namijenjen za označavanje svake klase nasljednice klase Plovilo kao klase koja predstavlja tip plovila koje je u upotrebi od strane vatrogasaca kao službeno vatrogasno plovilo.
 *
 * <p>Nasljeđuje {@link SluzbenoPlovilo} jer su vatrogasci državna služba i njihova plovila mogu imati uključenu rotaciju.</p>
 *
 *
 * @author Milan Šobot
 * @version 1.0
 */
public interface Vatrogasci extends SluzbenoPlovilo {}
