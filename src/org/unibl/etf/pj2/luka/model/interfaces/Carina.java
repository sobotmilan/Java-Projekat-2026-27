package org.unibl.etf.pj2.luka.model.interfaces;

/**
 * Markerski interfejs namijenjen za oznacavanje svake klase nasljednice klase Plovilo kao klase koja predstavlja plovilo koje je u upotrebi od strane carine kao carinski brod.
 * Nasljeđuje {@link SluzbenoPlovilo} jer je carina državna služba i njena plovila mogu imati upaljenu rotaciju.
 *
 * @author Milan Šobot
 * @version 1.0
 */
public interface Carina extends SluzbenoPlovilo {}
