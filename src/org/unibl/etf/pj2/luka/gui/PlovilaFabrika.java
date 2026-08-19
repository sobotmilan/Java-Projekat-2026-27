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

/**
 * Konstruiše konkretnu instancu {@link Plovilo}-a na osnovu podataka unesenih u administratorsku
 * formu, birajući odgovarajuću klasu prema zadatom {@link TipPlovila}.
 *
 * @author Milan Šobot
 * @version 1.0
 * @see TipPlovila
 */
public final class PlovilaFabrika {

    private PlovilaFabrika() {
    }

    /**
     * Kreira plovilo zadatog tipa sa unesenim podacima. Kapacitet/broj putnika trupova koji
     * očekuju cjelobrojnu specifičnu vrijednost se dobija skraćivanjem proslijeđene realne
     * vrijednosti na cijeli broj, dok tankeri koriste realnu vrijednost neposredno. Spisak potjera
     * se koristi samo za tipove koji pripadaju obalskoj straži, inače se ignoriše.
     *
     * @param tip Tip plovila koje treba kreirati.
     * @param naziv Naziv plovila.
     * @param imo IMO broj plovila.
     * @param brojMotora Broj motora plovila.
     * @param registarskiBroj Registarska oznaka plovila.
     * @param fotografija Putanja do fotografije plovila.
     * @param specificnaVrijednost Vrijednost polja specifičnog za trup (kapacitet, broj putnika
     *                             ili zapremina, zavisno od tipa).
     * @param spisakPotjera Putanja do spiska potjera, relevantno samo za plovila obalske straže.
     * @return Novokreirano plovilo zadatog tipa.
     * @throws IllegalArgumentException Ako {@code tip} nije poznata vrijednost enumeracije.
     */
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
