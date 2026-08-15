package org.unibl.etf.pj2.luka.gui;

import org.unibl.etf.pj2.luka.model.classes.KontejnerskiBrod;
import org.unibl.etf.pj2.luka.model.classes.KontejnerskiBrodObalskaStraza;
import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.model.classes.PutnickiKruzer;
import org.unibl.etf.pj2.luka.model.classes.PutnickiKruzerCarina;
import org.unibl.etf.pj2.luka.model.classes.PutnickiKruzerObalskaStraza;
import org.unibl.etf.pj2.luka.model.classes.Tanker;
import org.unibl.etf.pj2.luka.model.classes.TankerCarina;
import org.unibl.etf.pj2.luka.model.classes.TankerObalskaStraza;
import org.unibl.etf.pj2.luka.model.classes.TankerVatrogasci;

public enum TipPlovila {
    KONTEJNERSKI("Kontejnerski brod", null, "Kapacitet (TEU)"),
    KONTEJNERSKI_OBALSKA_STRAZA("Kontejnerski brod — Obalska straža", "Obalska straža", "Kapacitet (TEU)"),
    KRUZER("Putnički kruzer", null, "Broj putnika"),
    KRUZER_OBALSKA_STRAZA("Putnički kruzer — Obalska straža", "Obalska straža", "Broj putnika"),
    KRUZER_CARINA("Putnički kruzer — Carina", "Carina", "Broj putnika"),
    TANKER("Tanker", null, "Zapremina (barel)"),
    TANKER_OBALSKA_STRAZA("Tanker — Obalska straža", "Obalska straža", "Zapremina (barel)"),
    TANKER_CARINA("Tanker — Carina", "Carina", "Zapremina (barel)"),
    TANKER_VATROGASCI("Tanker — Vatrogasci", "Vatrogasci", "Zapremina (barel)");

    private final String naziv;
    private final String sluzba;
    private final String nazivSpecificnogPolja;

    TipPlovila(String naziv, String sluzba, String nazivSpecificnogPolja) {
        this.naziv = naziv;
        this.sluzba = sluzba;
        this.nazivSpecificnogPolja = nazivSpecificnogPolja;
    }

    public String getNaziv() {
        return naziv;
    }

    public String getSluzba() {
        return sluzba;
    }

    public String getNazivSpecificnogPolja() {
        return nazivSpecificnogPolja;
    }

    public boolean zahtijevaSpisakPotjera() {
        return "Obalska straža".equals(sluzba);
    }

    @Override
    public String toString() {
        return naziv;
    }

    public static TipPlovila odObjekta(Plovilo p) {
        if (p instanceof KontejnerskiBrodObalskaStraza) {
            return KONTEJNERSKI_OBALSKA_STRAZA;
        }
        if (p instanceof KontejnerskiBrod) {
            return KONTEJNERSKI;
        }
        if (p instanceof PutnickiKruzerObalskaStraza) {
            return KRUZER_OBALSKA_STRAZA;
        }
        if (p instanceof PutnickiKruzerCarina) {
            return KRUZER_CARINA;
        }
        if (p instanceof PutnickiKruzer) {
            return KRUZER;
        }
        if (p instanceof TankerObalskaStraza) {
            return TANKER_OBALSKA_STRAZA;
        }
        if (p instanceof TankerCarina) {
            return TANKER_CARINA;
        }
        if (p instanceof TankerVatrogasci) {
            return TANKER_VATROGASCI;
        }
        if (p instanceof Tanker) {
            return TANKER;
        }
        throw new IllegalArgumentException("Nepoznat tip plovila: " + (p == null ? "null" : p.getClass()));
    }
}
