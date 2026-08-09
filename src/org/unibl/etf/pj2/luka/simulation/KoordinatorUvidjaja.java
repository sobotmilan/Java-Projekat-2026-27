package org.unibl.etf.pj2.luka.simulation;

import org.unibl.etf.pj2.luka.model.classes.Dok;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.model.interfaces.Carina;
import org.unibl.etf.pj2.luka.model.interfaces.ObalskaStraza;
import org.unibl.etf.pj2.luka.model.interfaces.SluzbenoPlovilo;
import org.unibl.etf.pj2.luka.model.interfaces.Vatrogasci;
import org.unibl.etf.pj2.luka.util.LoggerUtil;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class KoordinatorUvidjaja implements Runnable {

    public static volatile long INTERVAL_PROVJERE_DOLASKA_MS = 100L;
    public static volatile long MAX_CEKANJE_DOLASKA_MS = 15000L;

    private final Luka luka;
    private final Terminal terminal;
    private final List<Plovilo> ucesniciSudara;
    private final int incidentX;
    private final int incidentY;
    private final File direktorijumIncidenta;

    public KoordinatorUvidjaja(Luka luka, Terminal terminal, List<Plovilo> ucesniciSudara,
                                int incidentX, int incidentY) {
        this(luka, terminal, ucesniciSudara, incidentX, incidentY, null);
    }

    public KoordinatorUvidjaja(Luka luka, Terminal terminal, List<Plovilo> ucesniciSudara,
                                int incidentX, int incidentY, File direktorijumIncidenta) {
        this.luka = luka;
        this.terminal = terminal;
        this.ucesniciSudara = new ArrayList<>(ucesniciSudara);
        this.incidentX = incidentX;
        this.incidentY = incidentY;
        this.direktorijumIncidenta = direktorijumIncidenta;
    }

    @Override
    public void run() {
        terminal.blokirajSaobracaj();
        List<BrodThread> odazvane = new ArrayList<>();
        try {
            odazvane = pozoviPatrole();
            sacekajDolazakPatrola(odazvane);

            long trajanje = trajanjeUvidjaja();
            Thread.sleep(trajanje);

            Incident incident = new Incident(ucesniciSudara, odazvanaPlovila(odazvane),
                    LocalDateTime.now(), trajanje, terminal.getIdTerminala());
            if (direktorijumIncidenta != null) {
                incident.sacuvaj(direktorijumIncidenta);
            } else {
                incident.sacuvaj();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } finally {
            oznaciUcesnikeSudaraZaNapustanje();
            terminal.odblokirajSaobracaj();
            for (BrodThread patrola : odazvane) {
                if (patrola.getPlovilo() instanceof SluzbenoPlovilo sluzbeno) {
                    sluzbeno.setRotacija(false);
                }
            }
            raspetljajPatrole(odazvane);
        }
    }

    private void oznaciUcesnikeSudaraZaNapustanje() {
        for (Plovilo ucesnik : ucesniciSudara) {
            BrodThread nit = pronadjiNit(ucesnik);
            if (nit != null) {
                nit.oznaciKaoUcesnikaSudara();
            }
        }
    }

    private BrodThread pronadjiNit(Plovilo p) {
        for (BrodThread kandidat : luka.getAktivnaPlovila()) {
            if (kandidat.getPlovilo() == p) {
                return kandidat;
            }
        }
        return null;
    }

    private void raspetljajPatrole(List<BrodThread> odazvane) {
        for (BrodThread patrola : odazvane) {
            Dok noviDok = terminal.rezervisiSlobodanDok(patrola.getPlovilo());
            patrola.zavrsiUvidjaj(noviDok);
        }
    }

    private List<BrodThread> pozoviPatrole() {
        List<BrodThread> odazvane = new ArrayList<>();
        dodajAkoPostoji(odazvane, PretragaPatrole.najblizaPatrola(luka, terminal, incidentX, incidentY, Vatrogasci.class));
        dodajAkoPostoji(odazvane, PretragaPatrole.najblizaPatrola(luka, terminal, incidentX, incidentY, ObalskaStraza.class));
        dodajAkoPostoji(odazvane, PretragaPatrole.najblizaPatrola(luka, terminal, incidentX, incidentY, Carina.class));
        return odazvane;
    }

    private void dodajAkoPostoji(List<BrodThread> odazvane, BrodThread patrola) {
        if (patrola == null) {
            LoggerUtil.logWarning("Nema dostupne patrole trazene sluzbe za incident u terminalu "
                    + terminal.getIdTerminala() + ".");
            return;
        }
        if (patrola.getPlovilo() instanceof SluzbenoPlovilo sluzbeno) {
            sluzbeno.setRotacija(true);
        }
        int ciljnoY = incidentY > 0 ? incidentY - 1 : incidentY + 1;
        patrola.pozoviNaIncident(terminal, incidentX, ciljnoY);
        odazvane.add(patrola);
    }

    private void sacekajDolazakPatrola(List<BrodThread> odazvane) throws InterruptedException {
        long krajnjeVrijeme = System.currentTimeMillis() + MAX_CEKANJE_DOLASKA_MS;
        while (!sveStigle(odazvane) && System.currentTimeMillis() < krajnjeVrijeme) {
            Thread.sleep(INTERVAL_PROVJERE_DOLASKA_MS);
        }
    }

    private boolean sveStigle(List<BrodThread> odazvane) {
        for (BrodThread patrola : odazvane) {
            if (!stiglaPored(patrola)) {
                return false;
            }
        }
        return true;
    }

    private boolean stiglaPored(BrodThread patrola) {
        if (patrola.getTrenutniTerminal() != terminal) {
            return false;
        }
        int px = patrola.getX();
        int py = patrola.getY();
        if (px < 0 || py < 0) {
            return false;
        }
        return Math.abs(px - incidentX) + Math.abs(py - incidentY) <= 1;
    }

    private long trajanjeUvidjaja() {
        long min = BrodThread.MIN_TRAJANJE_UVIDJAJA_MS;
        long max = BrodThread.MAX_TRAJANJE_UVIDJAJA_MS;
        if (max <= min) {
            return min;
        }
        return min + ThreadLocalRandom.current().nextLong(max - min + 1);
    }

    private List<Plovilo> odazvanaPlovila(List<BrodThread> odazvane) {
        List<Plovilo> plovila = new ArrayList<>();
        for (BrodThread patrola : odazvane) {
            plovila.add(patrola.getPlovilo());
        }
        return plovila;
    }
}
