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

import java.io.File;

public final class PlovilaFabrika {

    private PlovilaFabrika() {
    }

    public static Plovilo napravi(TipPlovila tip, String naziv, String imo, String brojMotora,
                                   String registarskiBroj, File fotografija, double specificnaVrijednost,
                                   File spisakPotjera) {
        int celobrojna = (int) specificnaVrijednost;
        switch (tip) {
            case KONTEJNERSKI:
                return new KontejnerskiBrod(naziv, imo, brojMotora, registarskiBroj, fotografija, celobrojna);
            case KONTEJNERSKI_OBALSKA_STRAZA:
                return new KontejnerskiBrodObalskaStraza(naziv, imo, brojMotora, registarskiBroj, fotografija,
                        celobrojna, spisakPotjera);
            case KRUZER:
                return new PutnickiKruzer(naziv, imo, brojMotora, registarskiBroj, fotografija, celobrojna);
            case KRUZER_OBALSKA_STRAZA:
                return new PutnickiKruzerObalskaStraza(naziv, imo, brojMotora, registarskiBroj, fotografija,
                        celobrojna, spisakPotjera);
            case KRUZER_CARINA:
                return new PutnickiKruzerCarina(naziv, imo, brojMotora, registarskiBroj, fotografija, celobrojna);
            case TANKER:
                return new Tanker(naziv, imo, brojMotora, registarskiBroj, fotografija, specificnaVrijednost);
            case TANKER_OBALSKA_STRAZA:
                return new TankerObalskaStraza(naziv, imo, brojMotora, registarskiBroj, fotografija,
                        specificnaVrijednost, spisakPotjera);
            case TANKER_CARINA:
                return new TankerCarina(naziv, imo, brojMotora, registarskiBroj, fotografija, specificnaVrijednost);
            case TANKER_VATROGASCI:
                return new TankerVatrogasci(naziv, imo, brojMotora, registarskiBroj, fotografija, specificnaVrijednost);
            default:
                throw new IllegalArgumentException("Nepoznat tip plovila: " + tip);
        }
    }
}
