package org.unibl.etf.pj2.luka.model.classes;

import java.io.File;
import java.io.Serializable;

abstract public class Plovilo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String naziv;
    private String imoBroj;
    private String brojMotora;
    private String registarskiBroj;
    private File fotografija;
    private double brzina;
    private boolean rotacija;

    {
        rotacija = false;
    }

    public Plovilo(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija) {
        this.naziv = naziv;
        this.imoBroj = imoBroj;
        this.brojMotora = brojMotora;
        this.registarskiBroj = registarskiBroj;
        this.fotografija = fotografija;
        this.brzina = java.util.concurrent.ThreadLocalRandom.current().nextDouble(1,50);
    }

    public String getNaziv() {
        return naziv;
    }

    public void setNaziv(String naziv) {
        this.naziv = naziv;
    }

    public String getImoBroj() {
        return imoBroj;
    }

    public void setImoBroj(String imoBroj) {
        this.imoBroj = imoBroj;
    }

    public String getBrojMotora() {
        return brojMotora;
    }

    public void setBrojMotora(String brojMotora) {
        this.brojMotora = brojMotora;
    }

    public String getRegistarskiBroj() {
        return registarskiBroj;
    }

    public void setRegistarskiBroj(String registarskiBroj) {
        this.registarskiBroj = registarskiBroj;
    }

    public File getFotografija() {
        return fotografija;
    }

    public void setFotografija(File fotografija) {
        this.fotografija = fotografija;
    }

    public double getBrzina() {
        return brzina;
    }

    public void setBrzina(double brzina) {
        this.brzina = brzina;
    }

    public boolean isRotacija() {
        return rotacija;
    }

    public void setRotacija(boolean rotacija) {
        this.rotacija = rotacija;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", this.imoBroj, this.naziv);
    }
}
