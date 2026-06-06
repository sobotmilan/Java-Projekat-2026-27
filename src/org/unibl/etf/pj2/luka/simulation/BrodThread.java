package org.unibl.etf.pj2.luka.simulation;

import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.Polje;
import org.unibl.etf.pj2.luka.model.classes.Terminal;
import org.unibl.etf.pj2.luka.model.interfaces.Carina;
import org.unibl.etf.pj2.luka.model.interfaces.ObalskaStraza;
import org.unibl.etf.pj2.luka.model.interfaces.Vatrogasci;
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
    private boolean moraNapustiti;

    {
        this.x = this.y = -1;
        this.isPrivezan = this.pokvaren = this.moraNapustiti = false;
    }

    public BrodThread(Plovilo plovilo, Luka luka) {
        this.plovilo = plovilo;
        this.luka = luka;
    }

    @Override
    public void run() {
        try {
            int trenutniTerminal = 0;
            boolean usidren = false;

            while (!usidren && trenutniTerminal < luka.getTerminali().size()) {
                Terminal t = luka.getTerminali().get(trenutniTerminal);

                boolean usaoUTerminal = false;
                while (!usaoUTerminal) {
                    if (pokusajUciUTerminal(t)) {
                        usaoUTerminal = true;

                        synchronized (luka.getEvidencijaUlaska()) {
                            if (!luka.getEvidencijaUlaska().containsKey(plovilo.getImoBroj())) {
                                luka.getEvidencijaUlaska().put(plovilo.getImoBroj(), LocalDateTime.now());
                            }
                        }
                        break;
                    }

                    Thread.sleep(1000);
                }

                System.out.println("Brod " + plovilo.getNaziv() + " je usao u Terminal " + (trenutniTerminal + 1));
                long sleep = (long) (1000 / plovilo.getBrzina());
                Thread.sleep(sleep);

                while (this.x < 3 && !this.moraNapustiti) {
                    int sledeciX = this.x + 1;
                    boolean pomjeren = false;

                    synchronized (t) {
                        Polje[][] matrica = t.getMatrica();
                        Polje poljeIspred = matrica[sledeciX][0];

                        if (poljeIspred.getTrenutnoPlovilo() == null) {
                            pomjeriNaPolje(sledeciX, 0);
                            pomjeren = true;
                        } else {
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

                    if (pomjeren) {
                        proveriRizikOdUdesa();
                        Thread.sleep(sleep);
                    } else {
                        Thread.sleep(500);
                    }
                }

                if (this.moraNapustiti) {
                    izadjiIzTrenutnogTerminala();
                    trenutniTerminal++;
                    this.moraNapustiti = false;
                    continue;
                }

                int ciljniX = -1;
                int ciljniY = -1;

                synchronized (t) {
                    for (int j = 2; j < 17; j++) {
                        if (t.getMatrica()[3][j].getOznaka().equals("D") && t.getMatrica()[3][j].getTrenutnoPlovilo() == null) {
                            ciljniX = 3;
                            ciljniY = j;
                            break;
                        }
                        if (t.getMatrica()[0][j].getOznaka().equals("D") && t.getMatrica()[0][j].getTrenutnoPlovilo() == null) {
                            ciljniX = 0;
                            ciljniY = j;
                            break;
                        }
                    }
                }

                if (ciljniY != -1) {
                    while (!pomjeriNaPolje(3, 1) && !this.moraNapustiti) {
                        Thread.sleep(500);
                    }
                    Thread.sleep(sleep);

                    while (this.y < ciljniY && !this.moraNapustiti) {
                        if (pomjeriNaPolje(3, this.y + 1)) {
                            proveriRizikOdUdesa();
                            Thread.sleep(sleep);
                        } else {
                            Thread.sleep(500);
                        }
                    }

                    if (!this.moraNapustiti && ciljniX == 0) {
                        while (this.x > 0 && !this.moraNapustiti) {
                            if (pomjeriNaPolje(this.x - 1, this.y)) {
                                proveriRizikOdUdesa();
                                Thread.sleep(sleep);
                            } else {
                                Thread.sleep(500);
                            }
                        }
                    }

                    if (this.moraNapustiti) {
                        izadjiIzTrenutnogTerminala();
                        trenutniTerminal++;
                        this.moraNapustiti = false;
                        continue;
                    }

                    this.isPrivezan = true;
                    System.out.println("Brod " + plovilo.getNaziv() + " uspjesno usidren na poziciju (" + this.x + "," + this.y + ") u Terminalu " + (trenutniTerminal + 1) + ".");
                    usidren = true;
                    return;
                } else {
                    System.out.println("Nema slobodnih dokova u terminalu " + (trenutniTerminal + 1) + ". Brod " + plovilo.getNaziv() + " nastavlja ka sljedecem terminalu.");
                    izadjiIzTrenutnogTerminala();
                    trenutniTerminal++;
                }
            }

            if (!usidren) {
                System.out.println("Brod " + plovilo.getNaziv() + " je obisao sve terminale i napustio luku jer nema slobodnih mjesta.");
            }

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

    /**
     * Pomoćna metoda koja bezbjedno izvodi brod iz trenutne pozicije unutar matrice
     * i izvodi ga kroz odlazni kanal (kolona 1) napolje, oslobađajući resurse terminala.
     */
    private void izadjiIzTrenutnogTerminala() throws InterruptedException {
        if (this.trenutniTerminal == null) return;
        long sleep = (long) (1000 / plovilo.getBrzina());

        if (this.y == 0) {
            while (this.x < 3) {
                if (pomjeriNaPolje(this.x + 1, 0)) {
                    Thread.sleep(sleep);
                } else {
                    Thread.sleep(500);
                }
            }
            while (!pomjeriNaPolje(3, 1)) {
                Thread.sleep(500);
            }
            Thread.sleep(sleep);
        }

        if (this.y > 1) {
            if (this.x != 3) {
                while (this.x < 3) {
                    if (pomjeriNaPolje(this.x + 1, this.y)) {
                        Thread.sleep(sleep);
                    } else {
                        Thread.sleep(500);
                    }
                }
            }

            while (this.y > 1) {
                if (pomjeriNaPolje(3, this.y - 1)) {
                    Thread.sleep(sleep);
                } else {
                    Thread.sleep(500);
                }
            }
        }

        while (this.x > 0) {
            if (pomjeriNaPolje(this.x - 1, 1)) {
                Thread.sleep(sleep);
            } else {
                Thread.sleep(500);
            }
        }

        synchronized (this.trenutniTerminal) {
            this.trenutniTerminal.getMatrica()[this.x][this.y].setTrenutnoPlovilo(null);
        }
        System.out.println("Brod " + plovilo.getNaziv() + " je napustio trenutni terminal.");
        this.trenutniTerminal = null;
        this.x = -1;
        this.y = -1;
    }

    private void proveriRizikOdUdesa() {
        if (plovilo instanceof ObalskaStraza || plovilo instanceof Carina || plovilo instanceof Vatrogasci) {
            return;
        }

        int sansa = ThreadLocalRandom.current().nextInt(100);
        if (sansa < 2) {
            this.pokvaren = true;
            // this.plovilo.setRotacija(true);
            System.err.println("UDES! Brod " + plovilo.getNaziv() + " se pokvario na poziciji (" + this.x + "," + this.y + ") i blokira saobracaj.");

            int trajanjeUvidjaja = ThreadLocalRandom.current().nextInt(3000, 10001);
            try {
                System.out.println("Sluzbena vozila hitno upucena na mjesto nesrece (" + this.x + "," + this.y + ").");
                Thread.sleep(1500);
                System.out.println("Uvidjaj u toku na brodu " + plovilo.getNaziv() + " (Trajanje: " + (trajanjeUvidjaja / 1000) + "s).");
                Thread.sleep(trajanjeUvidjaja);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            this.moraNapustiti = true;
            popraviBrod();
        }
    }

    public synchronized void popraviBrod() {
        this.pokvaren = false;
        // this.plovilo.setRotacija(false);
        System.out.println("Brod " + plovilo.getNaziv() + " je uspjesno popravljen, slijedi izlazak sa terminala.");
    }
}