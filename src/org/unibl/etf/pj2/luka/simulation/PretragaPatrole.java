package org.unibl.etf.pj2.luka.simulation;

import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.model.interfaces.Carina;
import org.unibl.etf.pj2.luka.model.interfaces.ObalskaStraza;
import org.unibl.etf.pj2.luka.model.interfaces.Vatrogasci;

import java.util.function.Predicate;

/**
 * Pretraga najbliže patrole (vatrogasci/obalska straža/carina) na nivou cijele luke, koju
 * dispečovanje koristi da odabere koje se službeno plovilo šalje na incident.
 *
 * <p>Pretraga je namjerno port-wide (preko {@link Luka#getAktivnaPlovila()}), ne ograničena na
 * terminal incidenta: sa ~2.5% vatrogasnih plovila u tipičnoj floti (vidi napomenu u
 * {@code ZAHTJEVI.md} uz {@code GeneratorPlovila}), terminal na kojem se incident desio vrlo često
 * neće imati nijedno vatrogasno plovilo, pa pretraga mora obuhvatiti i ostale terminale.</p>
 *
 * <p><b>Rastojanje preko granice terminala:</b> svaki {@link Terminal} ima svoju nezavisnu matricu
 * 4×17 — koordinate (x,y) plovila u različitim terminalima nisu u istom koordinatnom sistemu, pa
 * čisto Menhetn (Manhattan) rastojanje ima smisla samo unutar istog terminala. Profesor je potvrdio
 * da se prelazak plovila između terminala modeluje logički, ne kao kontinuirano kretanje kroz
 * fizički prostor između njih (vidi pitanje 1 u {@code dodatna_pojasnjenja.txt}), pa ni rastojanje
 * između terminala ne treba biti kontinuirano: koristi se broj terminala koje treba preći kao
 * dominantan član rastojanja (otežan sa {@link #TEZINA_PRELASKA_TERMINALA}, širinom matrice
 * terminala, tako da je prelazak u susjedni terminal grubo uporediv sa prelaskom cijele dužine
 * terminala), a lokalno Menhetn rastojanje unutar odredišnog/polaznog terminala služi kao
 * dodatni, sitniji član koji razdvaja kandidate na istoj terminalskoj udaljenosti.</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see BrodThread
 * @see Luka#getAktivnaPlovila()
 */
public final class PretragaPatrole {

    /**
     * "Cijena" prelaska jednog terminala u odnosu na jedno polje lokalnog Menhetn rastojanja —
     * širina matrice terminala ({@link Terminal#KOLONA_IZLAZ}/dokovi se protežu kroz svih 17
     * kolona), tako da terminalska razlika uvijek dominira nad lokalnim rastojanjem unutar
     * terminala, u skladu sa pretragom na nivou cijele luke, ne ograničenom na jedan terminal.
     */
    static final int TEZINA_PRELASKA_TERMINALA = 17;

    private PretragaPatrole() {
    }

    /**
     * Pronalazi najbliže aktivno (živo) plovilo koje implementira {@link Vatrogasci},
     * {@link ObalskaStraza} ili {@link Carina}, tražeći preko cijele luke
     * ({@link Luka#getAktivnaPlovila()}), ne samo unutar zadatog terminala.
     *
     * @param luka Luka čiji se registar aktivnih plovila pretražuje.
     * @param terminal Terminal u kojem se nalazi cilj (npr. mjesto incidenta).
     * @param x Red ciljne ćelije u matrici {@code terminal}-a.
     * @param y Kolona ciljne ćelije u matrici {@code terminal}-a.
     * @return Nit najbliže patrole, ili {@code null} ako nijedno plovilo obalske straže, carine ili
     *         vatrogasaca trenutno nije aktivno i pozicionirano u luci.
     */
    public static BrodThread najblizaPatrola(Luka luka, Terminal terminal, int x, int y) {
        return najblizaPatrola(luka, terminal, x, y, PretragaPatrole::jePatrola);
    }

    /**
     * Pronalazi najbliže aktivno plovilo koje je instanca zadate klase (npr. isključivo obalska
     * straža), tražeći preko cijele luke na isti način kao
     * {@link #najblizaPatrola(Luka, Terminal, int, int)}.
     *
     * @param luka Luka čiji se registar aktivnih plovila pretražuje.
     * @param terminal Terminal u kojem se nalazi cilj (npr. mjesto incidenta).
     * @param x Red ciljne ćelije u matrici {@code terminal}-a.
     * @param y Kolona ciljne ćelije u matrici {@code terminal}-a.
     * @param tip Klasa ili interfejs kojoj kandidat mora pripadati.
     * @param <T> Tip traženog plovila.
     * @return Nit najbliže patrole traženog tipa, ili {@code null} ako nijedno odgovarajuće
     *         plovilo trenutno nije aktivno i pozicionirano u luci, ili je {@code tip null}.
     */
    public static <T> BrodThread najblizaPatrola(Luka luka, Terminal terminal, int x, int y, Class<T> tip) {
        if (tip == null) {
            return null;
        }
        return najblizaPatrola(luka, terminal, x, y, tip::isInstance);
    }

    private static BrodThread najblizaPatrola(Luka luka, Terminal terminal, int x, int y,
                                               Predicate<Plovilo> odgovaraTrazenojSluzbi) {
        if (luka == null || terminal == null) {
            return null;
        }

        BrodThread najbliza = null;
        long najmanjeRastojanje = Long.MAX_VALUE;

        for (BrodThread kandidat : luka.getAktivnaPlovila()) {
            if (!odgovaraTrazenojSluzbi.test(kandidat.getPlovilo()) || !jeDostupna(kandidat)) {
                continue;
            }
            Terminal kandidatTerminal = kandidat.getTrenutniTerminal();
            int kandidatX = kandidat.getX();
            int kandidatY = kandidat.getY();
            if (kandidatTerminal == null || kandidatX < 0 || kandidatY < 0) {
                continue;
            }

            long rastojanje = rastojanje(terminal, x, y, kandidatTerminal, kandidatX, kandidatY);
            if (najbliza == null || rastojanje < najmanjeRastojanje || (rastojanje == najmanjeRastojanje
                    && kandidat.getPlovilo().getImoBroj().compareTo(najbliza.getPlovilo().getImoBroj()) < 0)) {
                najmanjeRastojanje = rastojanje;
                najbliza = kandidat;
            }
        }

        return najbliza;
    }

    /**
     * Provjerava da li plovilo pripada nekoj od tri patrolne službe.
     *
     * @param p Plovilo koje se provjerava.
     * @return {@code true} ako je {@code p} vatrogasno, obalske straže ili carinsko plovilo.
     */
    private static boolean jePatrola(Plovilo p) {
        return p instanceof Vatrogasci || p instanceof ObalskaStraza || p instanceof Carina;
    }

    /**
     * Provjerava da li je kandidat trenutno slobodan da bude poslat na novi zadatak, plovilo koje
     * je već na putu ka incidentu, koje je već na mjestu incidenta, ili koje napušta luku se ne
     * uzima u obzir.
     *
     * @param kandidat Plovilo čija se dostupnost provjerava.
     * @return {@code true} ako kandidat trenutno nije angažovan na drugom zadatku.
     */
    private static boolean jeDostupna(BrodThread kandidat) {
        Zadatak zadatak = kandidat.getZadatak();
        return zadatak != Zadatak.KA_INCIDENTU && zadatak != Zadatak.NA_INCIDENTU && zadatak != Zadatak.NAPUSTA;
    }

    /**
     * Računa rastojanje između ciljne ćelije i pozicije kandidata — čisto Menhetn rastojanje ako su
     * u istom terminalu, inače terminalska razlika (otežana {@link #TEZINA_PRELASKA_TERMINALA}) plus
     * lokalno Menhetn rastojanje kao sitniji, sekundarni član (vidi napomenu uz klasu).
     *
     * @param ciljniTerminal Terminal u kojem se nalazi ciljna ćelija.
     * @param ciljX Red ciljne ćelije.
     * @param ciljY Kolona ciljne ćelije.
     * @param kandidatTerminal Terminal u kojem se trenutno nalazi kandidat.
     * @param kandidatX Red kandidata u njegovom terminalu.
     * @param kandidatY Kolona kandidata u njegovom terminalu.
     * @return Rastojanje kandidata od ciljne ćelije, po opisanoj metrici.
     */
    private static long rastojanje(Terminal ciljniTerminal, int ciljX, int ciljY,
                                    Terminal kandidatTerminal, int kandidatX, int kandidatY) {
        long lokalno = Math.abs(ciljX - kandidatX) + (long) Math.abs(ciljY - kandidatY);
        if (ciljniTerminal == kandidatTerminal) {
            return lokalno;
        }
        long terminalskaRazlika = Math.abs(ciljniTerminal.getIdTerminala() - kandidatTerminal.getIdTerminala());
        return terminalskaRazlika * TEZINA_PRELASKA_TERMINALA + lokalno;
    }
}
