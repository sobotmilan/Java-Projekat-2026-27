package org.unibl.etf.pj2.luka.simulation;

import org.unibl.etf.pj2.luka.model.classes.Dok;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.util.GeneratorPlovila;
import org.unibl.etf.pj2.luka.util.LoggerUtil;
import org.unibl.etf.pj2.luka.util.PropertiesUtil;
import org.unibl.etf.pj2.luka.util.SerializationUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Priprema i pokreće početno stanje simulacije korisničke aplikacije (C1/C3/C4), oslanjajući
 * se na broj terminala pročitan iz {@code luka.properties} (T1).
 *
 * <p>Redoslijed pripreme, kako ga propisuje specifikacija: prvo se zatečena flota iz
 * {@code luka.ser} (ako postoji) postavlja na slučajne dokove nove strukture terminala (C3),
 * a zatim se svaki terminal dopunjava slučajno generisanim plovilima do minimuma koji korisnik
 * zadaje (C1/C4). Rezultat je {@link Luka} sa plovilima već fizički postavljenim u matricu —
 * niti za njih pokreće {@link #pokreniPrivezanaPlovila(Luka)}.
 *
 * @author Milan Šobot
 * @version 1.0
 */
public final class PokretacSimulacije {

    /**
     * Interval jednog koraka kretanja simulacije, u milisekundama. Imenovana konstanta umjesto
     * inline literala jer demonstracija ide na znatno slabijoj mašini i vrijednost će vjerovatno
     * trebati podesiti.
     */
    public static final long INTERVAL_TIKA_MS = 100L;

    /**
     * Interval osvježavanja GUI prikaza terminala ({@code PrikazTerminala}), u milisekundama.
     * Namjerno rjeđi od {@link #INTERVAL_TIKA_MS} — render ne mora pratiti svaki mikro-pomjeraj
     * da bi simulacija djelovala glatko, a rjeđe osvježavanje manje opterećuje slabiju mašinu.
     */
    public static final long INTERVAL_RENDEROVANJA_MS = 500L;

    private PokretacSimulacije() {
    }

    /**
     * Puna priprema za stvarno pokretanje aplikacije: broj terminala se čita iz
     * {@code luka.properties} (T1), a prethodno stanje (ako postoji) iz {@code luka.ser}.
     *
     * @param minimumPoTerminalu Minimalan broj plovila po terminalu koji zadaje korisnik (C1).
     * @return Nova {@link Luka}, spremna za {@link #pokreniPrivezanaPlovila(Luka)}.
     */
    public static Luka pripremiPocetnoStanje(int minimumPoTerminalu) {
        int brojTerminala = PropertiesUtil.getBrojTerminala();
        Luka postojeca = SerializationUtil.ucitajStanjeLuke();
        return pripremiPocetnoStanje(postojeca, brojTerminala, minimumPoTerminalu, ThreadLocalRandom.current());
    }

    /**
     * Čista varijanta pripreme, bez čitanja fajlova — testabilna bez dodirivanja diska.
     *
     * @param postojeca Prethodno stanje luke (npr. rezultat {@code SerializationUtil.ucitajStanjeLuke()}),
     *                  ili {@code null} ako je ovo prvo pokretanje.
     * @param brojTerminala Broj terminala koje treba izgraditi za novu sesiju (T1).
     * @param minimumPoTerminalu Minimalan broj plovila po terminalu koji zadaje korisnik (C1). Ne smije biti negativan.
     * @param rnd Izvor slučajnosti — omogućava ponovljive testove.
     * @return Nova {@link Luka} sa zatečenom i dopunskom flotom već postavljenom na dokove.
     */
    public static Luka pripremiPocetnoStanje(Luka postojeca, int brojTerminala, int minimumPoTerminalu, Random rnd) {
        if (minimumPoTerminalu < 0) {
            throw new IllegalArgumentException(
                    "Minimalan broj plovila po terminalu ne smije biti negativan: " + minimumPoTerminalu);
        }
        if (brojTerminala < 1) {
            throw new IllegalArgumentException("Broj terminala mora biti najmanje 1: " + brojTerminala);
        }

        List<Plovilo> zatecenaFlota = new ArrayList<>();
        Map<String, LocalDateTime> evidencija = new HashMap<>();

        if (postojeca != null) {
            // Mora se pozvati PRIJE generisanja bilo kojeg novog plovila (O1/S6: sprečava
            // koliziju IMO brojeva sa flotom koja se upravo izvlači iz stare luke).
            GeneratorPlovila.obezbijediJedinstvenostImoZa(postojeca);
            zatecenaFlota = izvuciDokovanaPlovila(postojeca);
            evidencija = new HashMap<>(postojeca.getEvidencijaUlaska());
        }

        List<Terminal> terminali = new ArrayList<>();
        for (int i = 0; i < brojTerminala; i++) {
            terminali.add(new Terminal(i));
        }
        Luka luka = new Luka(terminali, evidencija);

        rasporediNaSlucajneDokove(luka, zatecenaFlota, rnd);
        dopuniDoMinimuma(luka, minimumPoTerminalu, rnd);

        return luka;
    }

    /**
     * Pokreće niti za sva plovila koja su u {@code luka} već fizički privezana na dok (bilo
     * zatečena iz prethodne sesije, bilo novogenerisana da popune minimum). Svaka nit odmah
     * ulazi u parkirano stanje ({@link Zadatak#PRIVEZAN}) i registruje se u
     * {@link Luka#getAktivnaPlovila()}, umjesto da ponovo prolazi kroz ulazni kanal.
     *
     * @param luka Luka čija privezana plovila treba "oživiti" nitima.
     * @return Pokrenute niti, po jedna za svako trenutno privezano plovilo.
     */
    public static List<BrodThread> pokreniPrivezanaPlovila(Luka luka) {
        List<BrodThread> pokrenute = new ArrayList<>();
        for (Terminal t : luka.getTerminali()) {
            for (Dok d : t.getDokovi()) {
                Plovilo p = d.getLokacija().getTrenutnoPlovilo();
                if (p == null) {
                    continue;
                }
                BrodThread bt = new BrodThread(p, luka, t, d);
                Thread nit = new Thread(bt, "Brod-" + p.getImoBroj());
                nit.setDaemon(true);
                nit.start();
                pokrenute.add(bt);
            }
        }
        return pokrenute;
    }

    private static List<Plovilo> izvuciDokovanaPlovila(Luka luka) {
        List<Plovilo> plovila = new ArrayList<>();
        for (Terminal t : luka.getTerminali()) {
            for (Dok d : t.getDokovi()) {
                Plovilo p = d.getLokacija().getTrenutnoPlovilo();
                if (p != null) {
                    plovila.add(p);
                }
            }
        }
        return plovila;
    }

    private static void rasporediNaSlucajneDokove(Luka luka, List<Plovilo> flota, Random rnd) {
        for (Plovilo p : flota) {
            Dok dok = slucajanSlobodanDok(luka, rnd);
            if (dok == null) {
                LoggerUtil.logWarning("Zatečeno plovilo " + p.getImoBroj()
                        + " nije moglo biti smješteno — nema slobodnih vezova u novoj strukturi luke.");
                continue;
            }
            dok.getLokacija().setTrenutnoPlovilo(p);
        }
    }

    private static void dopuniDoMinimuma(Luka luka, int minimumPoTerminalu, Random rnd) {
        for (Terminal t : luka.getTerminali()) {
            int trenutno = brojDokovanihPlovila(t);
            while (trenutno < minimumPoTerminalu) {
                Dok dok = slucajanSlobodanDok(t, rnd);
                if (dok == null) {
                    LoggerUtil.logWarning("Terminal " + t.getIdTerminala()
                            + " nema dovoljno vezova da dostigne zadati minimum od "
                            + minimumPoTerminalu + " plovila.");
                    break;
                }
                dok.getLokacija().setTrenutnoPlovilo(GeneratorPlovila.generisiSlucajno(rnd));
                trenutno++;
            }
        }
    }

    private static int brojDokovanihPlovila(Terminal t) {
        int brojac = 0;
        for (Dok d : t.getDokovi()) {
            if (!d.isSlobodan()) {
                brojac++;
            }
        }
        return brojac;
    }

    private static Dok slucajanSlobodanDok(Luka luka, Random rnd) {
        List<Terminal> terminali = new ArrayList<>(luka.getTerminali());
        Collections.shuffle(terminali, rnd);
        for (Terminal t : terminali) {
            Dok dok = slucajanSlobodanDok(t, rnd);
            if (dok != null) {
                return dok;
            }
        }
        return null;
    }

    private static Dok slucajanSlobodanDok(Terminal t, Random rnd) {
        List<Dok> slobodni = new ArrayList<>();
        for (Dok d : t.getDokovi()) {
            if (d.isSlobodan()) {
                slobodni.add(d);
            }
        }
        if (slobodni.isEmpty()) {
            return null;
        }
        return slobodni.get(rnd.nextInt(slobodni.size()));
    }
}
