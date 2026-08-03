package org.unibl.etf.pj2.luka.model.classes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.unibl.etf.pj2.luka.model.classes.Polje;

public class Terminal implements Serializable {
    private static final long serialVersionUID;
    private final Polje[][] matrica;
    private final List<Dok> dokovi;
    private final int idTerminala;
    public static final int KANAL_ULAZ = 2;
    public static final int KANAL_IZLAZ = 1;
    public static final int KOLONA_ULAZ = 0;
    public static final int KOLONA_IZLAZ = 1;
    private transient java.util.Set<Integer> rezervisaniVezovi;

    static{
        serialVersionUID = 1L;
    }

    {
        this.matrica = new Polje[4][17];
        this.dokovi = new ArrayList<>();
    }

    public Terminal(int idTerminala) {
        this.idTerminala = idTerminala;
        initializeMatrix();
    }

    public Polje[][] getMatrica() {
        return matrica;
    }

    public List<Dok> getDokovi() {
        return dokovi;
    }

    public int getIdTerminala() {
        return idTerminala;
    }


    public int getBrojSlobodnihVezova() {
        int counter = 0;
        for(Dok d: dokovi) {
            if(d.isSlobodan()) {
                counter++;
            }
        }
        return counter;
    }

    public synchronized int getBrojRaspolozivihVezova() {
        int counter = 0;
        for (Dok d : dokovi) {
            if (d.isSlobodan() && !rezervisani().contains(d.getOznakaVezova())) {
                counter++;
            }
        }
        return counter;
    }


    private java.util.Set<Integer> rezervisani() {
        if (rezervisaniVezovi == null) {
            rezervisaniVezovi = new java.util.HashSet<>();
        }
        return rezervisaniVezovi;
    }

    public synchronized Dok rezervisiSlobodanDok(Plovilo p) {
        if (p == null) {
            return null;
        }
        for (Dok d : dokovi) {
            if (d.isSlobodan() && !rezervisani().contains(d.getOznakaVezova())) {
                rezervisani().add(d.getOznakaVezova());
                return d;
            }
        }
        return null;
    }

    public synchronized void otkaziRezervaciju(Dok d) {
        if (d != null) {
            rezervisani().remove(d.getOznakaVezova());
        }
    }

    private void initializeMatrix() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 17; j++) {
                matrica[i][j] = new Polje(i, j, "", null);
            }
        }

        for (int i = 0; i < 4; i++) {
            matrica[i][0].setOznaka("v");
            matrica[i][1].setOznaka("^");
        }

        int vezCounter = 1;
        for (int j = 2; j < 17; j++) {
            matrica[0][j].setOznaka("D");
            Dok d1 = new Dok(matrica[0][j], vezCounter++);
            dokovi.add(d1);

            matrica[3][j].setOznaka("D");
            Dok d2 = new Dok(matrica[3][j], vezCounter++);
            dokovi.add(d2);
        }

        for (int j = 2; j < 17; j++) {
            matrica[KANAL_IZLAZ][j].setOznaka("<-");
            matrica[KANAL_ULAZ][j].setOznaka("->");
        }
    }
}
