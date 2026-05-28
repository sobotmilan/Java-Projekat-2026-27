package org.unibl.etf.pj2.luka.simulation;

import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Polje;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.util.LoggerUtil;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

public class BrodThread implements Runnable {
    private final Plovilo plovilo;
    private final Luka luka;
    private Terminal trenutniTerminal;
    private int x, y;
    private boolean isPrivezan;
    private boolean pokvaren;

    {
        this.x = this.y = -1;
        this.isPrivezan = this.pokvaren = false;
    }

    public BrodThread(Plovilo plovilo, Luka luka) {
        this.plovilo = plovilo;
        this.luka = luka;
    }

    @Override
    public void run() {
        try {
            boolean usaoUTerminal = false;
            while (!usaoUTerminal) {
                for (Terminal t : luka.getTerminali()) {
                    if (pokusajUciUTerminal(t)) {
                        usaoUTerminal = true;
                        synchronized (luka.getEvidencijaUlaska()) {
                            luka.getEvidencijaUlaska().put(plovilo.getImoBroj(), LocalDateTime.now());
                        }
                        break;
                    }
                }
                if (!usaoUTerminal) {
                    Thread.sleep(3000);
                }
            }

            long sleep = (long) (1000 / plovilo.getBrzina());
            Thread.sleep(sleep);

            while (this.x < 3) {
                int sledeciX = this.x + 1;
                boolean pomjeren = false;

                Terminal lockTerminal = this.trenutniTerminal;
                if (lockTerminal != null) {
                    synchronized (lockTerminal) {
                        Polje[][] matrica = lockTerminal.getMatrica();
                        Polje poljeIspred = matrica[sledeciX][0];

                        if (poljeIspred.getTrenutnoPlovilo() == null) {
                            pomjeriNaPolje(sledeciX, 0);
                            pomjeren = true;
                        }
                        else {
                            Polje lijevoPolje = matrica[this.x][1];
                            Polje lijevoNapred = matrica[sledeciX][1];

                            if (lijevoPolje.getTrenutnoPlovilo() == null && lijevoNapred.getTrenutnoPlovilo() == null) {
                                System.out.println("Brod " + plovilo.getNaziv() + " zapocinje preticanje sporijeg broda!");

                                if (pomjeriNaPolje(this.x, 1)) {
                                    Thread.sleep(sleep);
                                    if (pomjeriNaPolje(sledeciX, 1)) {
                                        Thread.sleep(sleep);
                                        if (matrica[sledeciX][0].getTrenutnoPlovilo() == null) {
                                            pomjeriNaPolje(sledeciX, 0);
                                        }
                                        pomjeren = true;
                                    }
                                }
                            }
                        }
                    }
                }

                if (pomjeren) {
                    proveriRizikOdUdesa();
                    Thread.sleep(sleep);
                } else {
                    Thread.sleep(500);
                }
            }

            int ciljniX = -1;
            int ciljniY = -1;

            Terminal lockTerminal = this.trenutniTerminal;
            if (lockTerminal != null) {
                synchronized (lockTerminal) {
                    for (int j = 2; j < 17; j++) {
                        if (lockTerminal.getMatrica()[3][j].getOznaka().equals("D") && lockTerminal.getMatrica()[3][j].getTrenutnoPlovilo() == null) {
                            ciljniX = 3;
                            ciljniY = j;
                            break;
                        }
                        if (lockTerminal.getMatrica()[0][j].getOznaka().equals("D") && lockTerminal.getMatrica()[0][j].getTrenutnoPlovilo() == null) {
                            ciljniX = 0;
                            ciljniY = j;
                            break;
                        }
                    }
                }
            }

            if (ciljniY != -1) {
                while (!pomjeriNaPolje(3, 1)) {
                    Thread.sleep(500);
                }
                Thread.sleep(sleep);

                while (this.y < ciljniY) {
                    if (pomjeriNaPolje(3, this.y + 1)) {
                        proveriRizikOdUdesa();
                        Thread.sleep(sleep);
                    } else {
                        Thread.sleep(500);
                    }
                }

                if (ciljniX == 0) {
                    while (this.x > 0) {
                        if (pomjeriNaPolje(this.x - 1, this.y)) {
                            proveriRizikOdUdesa();
                            Thread.sleep(sleep);
                        } else {
                            Thread.sleep(500);
                        }
                    }
                }

                this.isPrivezan = true;
                System.out.println("Brod " + plovilo.getNaziv() + " uspjesno usidren na poziciju (" + this.x + "," + this.y + ")!");
                return;
            }

            while (this.x > 0) {
                if (pomjeriNaPolje(this.x - 1, 1)) {
                    proveriRizikOdUdesa();
                    Thread.sleep(sleep);
                } else {
                    Thread.sleep(500);
                }
            }

            if (this.trenutniTerminal != null) {
                synchronized (this.trenutniTerminal) {
                    this.trenutniTerminal.getMatrica()[this.x][this.y].setTrenutnoPlovilo(null);
                }
            }
            System.out.println("Brod " + plovilo.getNaziv() + " je napustio luku jer nije bilo slobodnih mjesta.");

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LoggerUtil.logError("Kriticna greska u kretanju broda: " + plovilo.getNaziv(), e);
        }
    }

    public boolean pokusajUciUTerminal(Terminal terminal) {
        synchronized (terminal) {
            if (terminal.getMatrica()[0][0].getTrenutnoPlovilo() == null) {
                terminal.getMatrica()[0][0].setTrenutnoPlovilo(this.plovilo);
                this.trenutniTerminal = terminal;
                this.x = 0;
                this.y = 0;
                return true;
            }
        }
        return false;
    }

    private boolean pomjeriNaPolje(int targetX, int targetY) {
        Terminal lockTerminal = this.trenutniTerminal;
        if (lockTerminal == null) return false;

        synchronized (lockTerminal) {
            Polje[][] matrica = lockTerminal.getMatrica();
            Polje staroPolje = matrica[this.x][this.y];
            Polje novoPolje = matrica[targetX][targetY];

            if (novoPolje.getTrenutnoPlovilo() == null) {
                novoPolje.setTrenutnoPlovilo(this.plovilo);
                staroPolje.setTrenutnoPlovilo(null);

                this.x = targetX;
                this.y = targetY;
                return true;
            }
        }
        return false;
    }

    private void proveriRizikOdUdesa() {
        int sansa = ThreadLocalRandom.current().nextInt(100);
        if (sansa < 2) {
            this.pokvaren = true;
            this.plovilo.setRotacija(true);
            System.err.println("DESIO SE UDES! Brod " + plovilo.getNaziv() + " se pokvario na poziciji (" + this.x + "," + this.y + ") i blokira saobracaj!");

            while (this.pokvaren) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public synchronized void popraviBrod() {
        this.pokvaren = false;
        this.plovilo.setRotacija(false);
        System.out.println("Brod " + plovilo.getNaziv() + " je uspjesno popravljen i nastavlja plovidbu.");
    }
}