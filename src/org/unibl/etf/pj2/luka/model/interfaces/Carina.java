package org.unibl.etf.pj2.luka.model.interfaces;

/**
 * Markerski interfejs namijenjen za označavanje svake klase nasljednice klase Plovilo kao klase koja predstavlja plovilo koje je u upotrebi od strane carine kao carinsko plovilo.
 * Nasljeđuje {@link SluzbenoPlovilo} jer je carina državna služba i njena plovila mogu imati upaljenu rotaciju koja je definisana ugovorom interfejsa {@link SluzbenoPlovilo}.
 *
 * @author Milan Šobot
 * @version 1.0
 */
public interface Carina extends SluzbenoPlovilo {}
