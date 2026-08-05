package org.unibl.etf.pj2.luka.simulation;

/**
 * Trenutni zadatak {@link BrodThread}-a. Zamjenjuje pravolinijsku pretpostavku da nit broda
 * završava čim se plovilo priveže — plovilo pod rotacijom mora ostati adresabilno (R4) i mora
 * moći biti pozvano da napusti terminal (C7/C8) i nakon što se veže.
 *
 * @author Milan Šobot
 * @version 1.0
 */
public enum Zadatak {
    /** Plovilo se kreće ka doku — u redu je za ulazak ili plovi kanalom terminala. */
    KA_DOKU,
    /** Plovilo je usidreno i čeka — parkirano stanje iz kojeg ga R4/C7/C8 mogu reaktivirati. */
    PRIVEZAN,
    /** Plovilo pod rotacijom je pozvano i kreće se ka mjestu incidenta. */
    KA_INCIDENTU,
    /** Plovilo je stiglo na mjesto incidenta i učestvuje u uviđaju. */
    NA_INCIDENTU,
    /** Plovilo se kreće ka izlazu iz terminala i napušta luku. */
    NAPUSTA
}
