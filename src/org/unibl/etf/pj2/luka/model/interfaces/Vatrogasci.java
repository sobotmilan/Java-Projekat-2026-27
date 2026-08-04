package org.unibl.etf.pj2.luka.model.interfaces;

/**
 * Markerski interfejs namijenjen za oznacavanje svake klase nasljednice klase Plovilo kao klase koja predstavlja plovilo koje je u upotrebi od strane vatrogasaca kao vatrogasno plovilo.
 * Nasljeđuje {@link SluzbenoPlovilo} jer su vatrogasci državna služba i njihova plovila mogu imati upaljenu rotaciju.
 *
 */
public interface Vatrogasci extends SluzbenoPlovilo {}
