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
 * Priprema i pokreće početno stanje simulacije, oslanjajući se na broj terminala pročitan iz {@code luka.properties}.
 *
 * <p>Redoslijed pripreme, kako ga propisuje specifikacija: prvo se zatečena flota iz
 * {@code luka.ser} (ako postoji) postavlja na slučajne dokove nove strukture terminala,
 * a zatim se svaki terminal dopunjava slučajno generisanim plovilima do minimuma koji korisnik ručno zadaje.
 * Rezultat je {@link Luka} sa plovilima već fizički postavljenim u matricu,
 * a njihove niti za njih pokreće {@link #pokreniPrivezanaPlovila(Luka)}.
 *
 * @author Milan Šobot
 * @version 1.0
 */
public final class PokretacSimulacije {

    /**
     * Interval jednog koraka kretanja simulacije, u milisekundama.
     * Imenovana konstanta kako bi se vrijednost mogla podesiti u zavisnosti od okruženja u kom se izvršava simulacija.
     */
    public static final long INTERVAL_TIKA_MS = 100L;

    /**
     * Interval osvježavanja GUI-ja, izražen u milisekundama.
     * Namjerno veći od {@link #INTERVAL_TIKA_MS}, jer GUI ne mora nužno pratiti svaki pomjeraj
     * da bi simulacija djelovala glatko, a i rjeđe osvježavanje manje opterećuje slabiju mašinu.
     */
    public static final long INTERVAL_RENDEROVANJA_MS = 150L;

    private PokretacSimulacije() {
    }

    /**
     * Puna priprema za pokretanje aplikacije: broj terminala se čita iz
     * {@code luka.properties}, a prethodno stanje iz {@code luka.ser} (ako postoji).
     *
     * @param minimumPoTerminalu Minimalan broj plovila po terminalu koji zadaje korisnik.
     * @return Nova instanca objekta klase {@link Luka}.
     */
    public static Luka pripremiPocetnoStanje(int minimumPoTerminalu) {
        int brojTerminala = PropertiesUtil.getBrojTerminala();
        Luka postojeca = SerializationUtil.ucitajStanjeLuke();
        return pripremiPocetnoStanje(postojeca, brojTerminala, minimumPoTerminalu, ThreadLocalRandom.current());
    }

    /**
     * Čista varijanta pripreme, bez čitanja fajlova.
     *
     * @param postojeca Prethodno stanje luke (npr. rezultat {@code SerializationUtil.ucitajStanjeLuke()}), ili {@code null} ako je ovo prvo pokretanje.
     * @param brojTerminala Broj terminala koje treba stvoriti za novu sesiju izvršavanja. Mora biti bar 1.
     * @param minimumPoTerminalu Minimalan broj plovila po terminalu koji zadaje korisnik. Ne smije biti negativan.
     * @param rnd Izvor slučajnosti, omogućava ponovljivost testova.
     *
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
            // Mora se pozvati PRIJE generisanja bilo kojeg novog plovila — sprečava
            // koliziju IMO brojeva sa flotom koja se upravo izvlači iz stare luke.
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

    /**
     * Skuplja sva plovila trenutno privezana na dokove luke, obilazeći matrice svih
     * terminala.
     *
     * @param luka Luka čija se privezana plovila skupljaju.
     * @return Lista zatečenih plovila.
     */
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

    /**
     * Raspoređuje zatečenu flotu na slučajne slobodne dokove nove strukture terminala,
     * direktno postavljajući plovilo u ćeliju matrice (ne preko {@link Terminal#rezervisiSlobodanDok}).
     * Ako nova struktura nema dovoljno kapaciteta, višak plovila se tiho izostavlja (uz upozorenje
     * u logu) i briše iz evidencije ulaska — nema specifikacije šta drugo raditi u tom rubnom slučaju.
     *
     * <p>WARNING: SETUP-ONLY METODA, NE POZIVATI DOK TRAJE SIMULACIJE I POSTOJE AKTIVNE KORISNICKE NITI!</p>
     *
     * @param luka Luka čija se nova struktura terminala popunjava.
     * @param flota Zatečena plovila koja treba rasporediti.
     * @param rnd Izvor slučajnosti.
     */
    private static void rasporediNaSlucajneDokove(Luka luka, List<Plovilo> flota, Random rnd) {
        for (Plovilo p : flota) {
            Dok dok = slucajanSlobodanDok(luka, rnd);
            if (dok == null) {
                LoggerUtil.logWarning("Zatečeno plovilo " + p.getImoBroj()
                        + " nije moglo biti smješteno — nema slobodnih vezova u novoj strukturi luke.");
                luka.getEvidencijaUlaska().remove(p.getImoBroj());
                continue;
            }
            dok.getLokacija().setTrenutnoPlovilo(p);
        }
    }

    /**
     * Dopunjava svaki terminal slučajno generisanim plovilima ({@link GeneratorPlovila#generisiSlucajno(Random)})
     * dok broj dokovanih plovila ne dostigne {@code minimumPoTerminalu}. Zatečena flota
     * se računa u minimum, ne dodaje preko njega. Ako terminal nema dovoljno vezova da dostigne
     * minimum, prekida se uz upozorenje u logu.
     *
     * <p>WARNING: SETUP-ONLY METODA, NE POZIVATI DOK TRAJE SIMULACIJE I POSTOJE AKTIVNE KORISNICKE NITI!</p>
     *
     * @param luka Luka čiji se terminali dopunjuju.
     * @param minimumPoTerminalu Minimalan broj plovila po terminalu.
     * @param rnd Izvor slučajnosti.
     */
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

    /**
     * Broji plovila trenutno dokovana na zadatom terminalu.
     *
     * @param t Terminal koji se broji.
     * @return Broj zauzetih vezova terminala.
     */
    private static int brojDokovanihPlovila(Terminal t) {
        int brojac = 0;
        for (Dok d : t.getDokovi()) {
            if (!d.isSlobodan()) {
                brojac++;
            }
        }
        return brojac;
    }

    /**
     * Bira slučajan slobodan dok bilo gdje u luci — prvo miješa redoslijed terminala, pa unutar
     * prvog terminala koji ima slobodan vez bira slučajan dok među njima.
     *
     * @param luka Luka u kojoj se traži slobodan dok.
     * @param rnd Izvor slučajnosti.
     * @return Slučajan slobodan dok, ili {@code null} ako nijedan terminal nema slobodan vez.
     */
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

    /**
     * Bira slučajan slobodan dok unutar jednog terminala.
     *
     * @param t Terminal u kojem se traži slobodan dok.
     * @param rnd Izvor slučajnosti.
     * @return Slučajan slobodan dok, ili {@code null} ako terminal nema slobodnih vezova.
     */
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
