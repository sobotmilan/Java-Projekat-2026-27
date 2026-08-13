package org.unibl.etf.pj2.luka.simulation;

/**
 * Enumeracija trenutnog zadatka {@link BrodThread}-a.
 *
 * <p>Zamjenjuje podrazumijevanu pretpostavku da nit broda
 * završava čim se plovilo priveže, jer plovilo pod rotacijom mora ostati adresabilno i mora
 * moći biti pozvano da napusti terminal čak i nakon što se priveže na dok i privremeno suspenduje.</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see BrodThread
 */
public enum Zadatak {

    /** Plovilo se kreće ka doku, čeka u redu za ulazak ili plovi kanalom terminala. */
    KA_DOKU,
    /** Plovilo je usidreno i čeka, suštinski parkirano stanje iz kojeg je moguće reaktivirati plovilo.*/
    PRIVEZAN,
    /** Plovilo pod rotacijom je pozvano i plovi ka mjestu incidenta.*/
    KA_INCIDENTU,
    /** Plovilo je stiglo na mjesto incidenta i izvršava uviđaj. */
    NA_INCIDENTU,
    /** Plovilo se kreće ka izlazu iz terminala i napušta luku.*/
    NAPUSTA,
    PRACENJE,
    POD_PRATNJOM
}
