package org.unibl.etf.pj2.luka.simulation;

import org.unibl.etf.pj2.luka.model.classes.Dok;
import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Polje;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.util.LoggerUtil;

import java.time.LocalDateTime;

public class BrodThread implements Runnable {
    private static final int MAX_POKUSAJA = 100;
    private static final long CEKANJE_MS = 100L;
    private static final int PRAG_PRETICANJA = 3;
    private static final int PRIORITET_BEZ_ROTACIJE = 10;
    public static volatile boolean SUDARI_OMOGUCENI = false;

    private final Plovilo plovilo;
    private final Luka luka;
    private Terminal trenutniTerminal;
    private int x, y;
    private boolean isPrivezan;
    private boolean moraNapustiti;

    {
        this.x = this.y = -1;
        this.isPrivezan = false;
        this.moraNapustiti = false;
    }

    public BrodThread(Plovilo plovilo, Luka luka) {
        this.plovilo = plovilo;
        this.luka = luka;
    }

    @Override
    public void run() {
        try {
            int idx = 0;
            boolean usidren = false;

            while (!usidren && idx < luka.getTerminali().size()) {
                Terminal t = luka.getTerminali().get(idx);

                Dok rezervisan = t.rezervisiSlobodanDok(plovilo);
                if (rezervisan == null) {
                    log("Terminal " + (idx + 1) + " je pun, nastavlja pravo.");
                    idx++;
                    continue;
                }

                if (!udjiUTerminal(t)) {
                    t.otkaziRezervaciju(rezervisan);
                    log("Nije uspio ući u terminal " + (idx + 1) + ", nastavlja pravo.");
                    idx++;
                    continue;
                }
                evidentirajUlazak();
                log("Ušao u terminal " + (idx + 1) + ".");

                if (doploviDoDoka(rezervisan)) {
                    this.isPrivezan = true;
                    usidren = true;
                    log("Usidren na vezu " + rezervisan.getOznakaVezova()
                            + " (" + this.x + "," + this.y + ") u terminalu " + (idx + 1) + ".");
                } else {
                    t.otkaziRezervaciju(rezervisan);
                    log("Ne može doći do veza u terminalu " + (idx + 1) + ", nastavlja dalje.");
                    napustiTerminal();
                    idx++;
                }
            }

            if (!usidren) {
                log("Obišao sve terminale i napustio luku — nema slobodnih vezova.");
            }

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LoggerUtil.logError("Kriticna greska u kretanju broda: " + plovilo.getNaziv(), e);
        }
    }

    public boolean pokusajUciUTerminal(Terminal terminal) {
        synchronized (terminal) {
            if (terminal.getMatrica()[0][Terminal.KOLONA_ULAZ].getTrenutnoPlovilo() == null) {
                terminal.getMatrica()[0][Terminal.KOLONA_ULAZ].setTrenutnoPlovilo(this.plovilo);
                this.trenutniTerminal = terminal;
                this.x = 0;
                this.y = Terminal.KOLONA_ULAZ;
                return true;
            }
        }
        return false;
    }

    private boolean udjiUTerminal(Terminal t) throws InterruptedException {
        for (int i = 0; i < MAX_POKUSAJA; i++) {
            if (pokusajUciUTerminal(t)) {
                return true;
            }
            Thread.sleep(CEKANJE_MS);
        }
        return false;
    }

    private void evidentirajUlazak() {
        synchronized (luka.getEvidencijaUlaska()) {
            if (!luka.getEvidencijaUlaska().containsKey(plovilo.getImoBroj())) {
                luka.getEvidencijaUlaska().put(plovilo.getImoBroj(), LocalDateTime.now());
            }
        }
    }

    private boolean doploviDoDoka(Dok cilj) throws InterruptedException {
        int ciljX = cilj.getLokacija().getX();
        int ciljY = cilj.getLokacija().getY();
        long korak = trajanjeKoraka();

        if (!sidjiDoKanala(korak)) {
            return false;
        }

        if (!ploviIstocno(ciljY, korak)) {
            return false;
        }

        if (ciljX == 3) {
            return pomjeriSaCekanjem(3, ciljY, korak);
        }

        if (!pomjeriSaCekanjem(Terminal.KANAL_IZLAZ, ciljY, korak)) {
            return false;
        }
        Thread.sleep(korak);
        return pomjeriSaCekanjem(0, ciljY, korak);
    }

    private boolean sidjiDoKanala(long korak) throws InterruptedException {
        while (this.x < Terminal.KANAL_ULAZ) {
            if (!pomjeriSaCekanjem(this.x + 1, Terminal.KOLONA_ULAZ, korak)) {
                return false;
            }
            provjeriSudar();
            Thread.sleep(korak);
        }
        return true;
    }

    private boolean ploviIstocno(int ciljY, long korak) throws InterruptedException {
        int neuspjesi = 0;
        int ukupnoPokusaja = 0;
        boolean imamPrioritet = plovilo.getPrioritet() < PRIORITET_BEZ_ROTACIJE;

        while (this.y < ciljY) {
            if (++ukupnoPokusaja > MAX_POKUSAJA * 4) {
                return false;
            }

            boolean pomjeren = false;

            if (this.x == Terminal.KANAL_ULAZ) {
                boolean moraUstupitiProlaz = !imamPrioritet
                        && ustupaProlaz(this.trenutniTerminal, this.x, this.y, this.plovilo);

                if (!moraUstupitiProlaz) {
                    pomjeren = pomjeriNaPolje(Terminal.KANAL_ULAZ, this.y + 1);
                }

                boolean pragZaPreticanjeIspunjen = imamPrioritet || neuspjesi >= PRAG_PRETICANJA;
                if (!pomjeren && pragZaPreticanjeIspunjen && smijePreticati(this.y + 1)) {
                    pomjeren = pomjeriNaPolje(Terminal.KANAL_IZLAZ, this.y);
                    if (pomjeren) {
                        log("Započinje preticanje" + (imamPrioritet ? " (prioritet pod rotacijom)." : "."));
                    }
                }
            } else {
                pomjeren = pomjeriNaPolje(Terminal.KANAL_IZLAZ, this.y + 1);
                if (pomjeren) {
                    Thread.sleep(korak);
                    pomjeriNaPolje(Terminal.KANAL_ULAZ, this.y);
                }
            }

            if (pomjeren) {
                neuspjesi = 0;
                provjeriSudar();
                Thread.sleep(korak);
            } else {
                neuspjesi++;
                Thread.sleep(CEKANJE_MS);
            }
        }


        if (this.x == Terminal.KANAL_IZLAZ) {
            pomjeriSaCekanjem(Terminal.KANAL_ULAZ, this.y, korak);
        }
        return true;
    }

    private boolean smijePreticati(int sledeciY) {
        Terminal t = this.trenutniTerminal;
        if (t == null) {
            return false;
        }
        synchronized (t) {
            Polje[][] m = t.getMatrica();
            return m[Terminal.KANAL_IZLAZ][this.y].getTrenutnoPlovilo() == null
                    && m[Terminal.KANAL_IZLAZ][sledeciY].getTrenutnoPlovilo() == null;
        }
    }

    /**
     * Provjerava da li plovilo na poziciji ({@code x}, {@code y}) treba da ustupi prolaz plovilu
     * pod aktivnom rotacijom koje se nalazi neposredno iza njega u istoj traci kanala. Plovilo pod
     * rotacijom ima prioritet pri preticanju, pa ostala plovila moraju da se zaustave na postojećem
     * polju dok ono ne prođe (R5, {@link Plovilo#getPrioritet()}).
     *
     * @param terminal Terminal čija se matrica provjerava.
     * @param x Red u kojem se plovilo trenutno nalazi.
     * @param y Kolona u kojoj se plovilo trenutno nalazi.
     * @param trenutni Plovilo čije se kretanje provjerava.
     * @return true ako trenutni treba da ustupi prolaz, u suprotnom false.
     */
    static boolean ustupaProlaz(Terminal terminal, int x, int y, Plovilo trenutni) {
        if (terminal == null || y <= 0 || trenutni.getPrioritet() < PRIORITET_BEZ_ROTACIJE) {
            return false;
        }
        synchronized (terminal) {
            Plovilo iza = terminal.getMatrica()[x][y - 1].getTrenutnoPlovilo();
            return iza != null && iza.getPrioritet() < PRIORITET_BEZ_ROTACIJE;
        }
    }

    private void napustiTerminal() throws InterruptedException {
        Terminal t = this.trenutniTerminal;
        if (t == null) {
            return;
        }
        long korak = trajanjeKoraka();

        if (this.y > Terminal.KOLONA_IZLAZ) {
            if (this.x == 3) {
                pomjeriSaCekanjem(Terminal.KANAL_ULAZ, this.y, korak);
                Thread.sleep(korak);
            }
            if (this.x != Terminal.KANAL_IZLAZ) {
                pomjeriSaCekanjem(Terminal.KANAL_IZLAZ, this.y, korak);
                Thread.sleep(korak);
            }

            int pokusaja = 0;
            while (this.y > Terminal.KOLONA_IZLAZ && pokusaja++ < MAX_POKUSAJA * 4) {
                if (pomjeriNaPolje(Terminal.KANAL_IZLAZ, this.y - 1)) {
                    Thread.sleep(korak);
                } else {
                    Thread.sleep(CEKANJE_MS);
                }
            }
        }

        int pokusaja = 0;
        while (this.x > 0 && pokusaja++ < MAX_POKUSAJA * 2) {
            if (pomjeriNaPolje(this.x - 1, Terminal.KOLONA_IZLAZ)) {
                Thread.sleep(korak);
            } else {
                Thread.sleep(CEKANJE_MS);
            }
        }

        oslobodiTrenutnoPolje();
        log("Napustio terminal.");
    }

    private void oslobodiTrenutnoPolje() {
        Terminal t = this.trenutniTerminal;
        if (t == null || this.x < 0 || this.y < 0) {
            return;
        }
        synchronized (t) {
            Polje p = t.getMatrica()[this.x][this.y];
            if (p.getTrenutnoPlovilo() == this.plovilo) {
                p.setTrenutnoPlovilo(null);
            }
        }
        this.trenutniTerminal = null;
        this.x = -1;
        this.y = -1;
    }

    private boolean pomjeriNaPolje(int targetX, int targetY) {
        Terminal t = this.trenutniTerminal;
        if (t == null || this.x < 0 || this.y < 0) {
            return false;
        }

        synchronized (t) {
            Polje[][] matrica = t.getMatrica();
            Polje staro = matrica[this.x][this.y];
            Polje novo = matrica[targetX][targetY];

            if (novo.getTrenutnoPlovilo() == null) {
                novo.setTrenutnoPlovilo(this.plovilo);
                if (staro.getTrenutnoPlovilo() == this.plovilo) {
                    staro.setTrenutnoPlovilo(null);
                }
                this.x = targetX;
                this.y = targetY;
                return true;
            }
        }
        return false;
    }

    private boolean pomjeriSaCekanjem(int targetX, int targetY, long korak) throws InterruptedException {
        for (int i = 0; i < MAX_POKUSAJA; i++) {
            if (pomjeriNaPolje(targetX, targetY)) {
                return true;
            }
            Thread.sleep(CEKANJE_MS);
        }
        return false;
    }

    private long trajanjeKoraka() {
        long korak = (long) (1000.0 / plovilo.getBrzina());
        return Math.max(20L, Math.min(korak, 400L));
    }

    private void provjeriSudar() {
        if (!SUDARI_OMOGUCENI) {
            return;
        }
    }

    public Plovilo getPlovilo() {
        return plovilo;
    }

    public boolean isPrivezan() {
        return isPrivezan;
    }

    public boolean isMoraNapustiti() {
        return moraNapustiti;
    }

    public void setMoraNapustiti(boolean moraNapustiti) {
        this.moraNapustiti = moraNapustiti;
    }

    private void log(String poruka) {
        System.out.println("[" + plovilo.getNaziv() + "] " + poruka);
    }
}