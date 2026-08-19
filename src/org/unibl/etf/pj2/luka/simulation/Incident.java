package org.unibl.etf.pj2.luka.simulation;

import org.unibl.etf.pj2.luka.model.classes.Plovilo;
import org.unibl.etf.pj2.luka.util.LoggerUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Klasa koja predstavlja zapis jednog incidenta.
 *
 * <p>plovila koja su učestvovala u sudaru, službena plovila koja su se
 * odazvala, vrijeme incidenta, apsolutne putanje do fotografija svih učesnika, trajanje uviđaja i
 * terminal na kojem se incident desio.</p>
 *
 * <p>Ovo je čist model podataka i njegov API, ne bira učesnike sudara ni najbližu patrolu,
 * ne pokreće blokadu terminala niti sam upravlja trajanjem uviđaja. Ko konstruiše ovu klasu i kada
 * (koordinator uviđaja) je nerelevantno u kontekstu definicije ove klase.</p>
 *
 * <p><b>Fotografije:</b> čuvaju se apsolutne putanje ({@link File#getAbsolutePath()}), ne
 * bajtovi fotografija, jer je jednostavnije i manje od bajtova, a apsolutna forma čini zapis čitljivim
 * bez obzira iz kojeg je radnog direktorijuma simulacija pokrenuta u trenutku nastanka incidenta
 * (za razliku od nekih drugih putanja u projektu koje ostaju relativne).</p>
 *
 * @author Milan Šobot
 * @version 1.0
 * @see KoordinatorUvidjaja
 */
public class Incident implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** Plovila koja su učestvovala u sudaru. */
    private final List<Plovilo> ucesniciSudara;

    /** Službena plovila (obalska straža/carina/vatrogasci) koja su se odazvala na incident. */
    private final List<Plovilo> odazvanaSluzbenaPlovila;

    /** Vrijeme kada je incident zabilježen. */
    private final LocalDateTime vrijeme;

    /** Apsolutne putanje do fotografija svih učesnika (sudar + odazvana službena plovila). */
    private final List<String> apsolutnePutanjeFotografija;

    /** Trajanje uviđaja, u milisekundama. */
    private final long trajanjeUvidjajaMs;

    /** Redni broj terminala na kojem se incident desio. */
    private final int idTerminala;

    /** Vrsta incidenta (sudar ili potjernica). */
    private final TipIncidenta tip;

    /**
     * Kreira zapis incidenta. Apsolutne putanje fotografija se izvode automatski iz
     * {@link Plovilo#getFotografija()} svih proslijeđenih plovila u trenutku konstrukcije.
     *
     * @param ucesniciSudara Plovila koja su učestvovala u sudaru.
     * @param odazvanaSluzbenaPlovila Službena plovila koja su se odazvala na incident.
     * @param vrijeme Vrijeme incidenta.
     * @param trajanjeUvidjajaMs Trajanje uviđaja, u milisekundama.
     * @param idTerminala Redni broj terminala na kojem se incident desio.
     */
    public Incident(List<Plovilo> ucesniciSudara, List<Plovilo> odazvanaSluzbenaPlovila,
                     LocalDateTime vrijeme, long trajanjeUvidjajaMs, int idTerminala) {
        this(ucesniciSudara, odazvanaSluzbenaPlovila, vrijeme, trajanjeUvidjajaMs, idTerminala, TipIncidenta.SUDAR);
    }

    /**
     * Kreira zapis incidenta zadatog tipa. Apsolutne putanje fotografija se izvode automatski iz
     * {@link Plovilo#getFotografija()} svih proslijeđenih plovila u trenutku konstrukcije.
     *
     * @param ucesniciSudara Plovila koja su učestvovala u sudaru, odnosno traženo plovilo (jedini
     *                       element liste) u slučaju potjernice.
     * @param odazvanaSluzbenaPlovila Službena plovila koja su se odazvala na incident, odnosno
     *                                obalska straža koja je izvršila potjeru (jedini element liste).
     * @param vrijeme Vrijeme incidenta.
     * @param trajanjeUvidjajaMs Trajanje uviđaja, u milisekundama.
     * @param idTerminala Redni broj terminala na kojem se incident desio.
     * @param tip Vrsta incidenta.
     */
    public Incident(List<Plovilo> ucesniciSudara, List<Plovilo> odazvanaSluzbenaPlovila,
                     LocalDateTime vrijeme, long trajanjeUvidjajaMs, int idTerminala, TipIncidenta tip) {
        this.ucesniciSudara = new ArrayList<>(ucesniciSudara);
        this.odazvanaSluzbenaPlovila = new ArrayList<>(odazvanaSluzbenaPlovila);
        this.vrijeme = vrijeme;
        this.trajanjeUvidjajaMs = trajanjeUvidjajaMs;
        this.idTerminala = idTerminala;
        this.tip = tip;
        this.apsolutnePutanjeFotografija = prikupiApsolutnePutanje();
    }

    /**
     * Sakuplja apsolutne putanje do fotografija svih učesnika sudara i svih odazvanih službenih
     * plovila.
     *
     * @return Lista apsolutnih putanja do fotografija, redoslijedom učesnici pa odazvana plovila.
     */
    private List<String> prikupiApsolutnePutanje() {
        List<String> putanje = new ArrayList<>();
        dodajPutanje(putanje, ucesniciSudara);
        dodajPutanje(putanje, odazvanaSluzbenaPlovila);
        return putanje;
    }

    /**
     * Dodaje apsolutnu putanju do fotografije svakog plovila iz zadate liste u {@code putanje},
     * preskačući plovila bez dodijeljene fotografije.
     *
     * @param putanje Lista u koju se putanje dodaju.
     * @param plovila Plovila čije se fotografije dodaju.
     */
    private static void dodajPutanje(List<String> putanje, List<Plovilo> plovila) {
        for (Plovilo p : plovila) {
            if (p != null && p.getFotografija() != null) {
                putanje.add(p.getFotografija().getAbsolutePath());
            }
        }
    }

    /**
     * Omogućava dobijanje plovila koja su učestvovala u sudaru.
     *
     * @return Lista učesnika sudara.
     */
    public List<Plovilo> getUcesniciSudara() {
        return ucesniciSudara;
    }

    /**
     * Omogućava dobijanje službenih plovila koja su se odazvala na incident.
     *
     * @return Lista odazvanih službenih plovila.
     */
    public List<Plovilo> getOdazvanaSluzbenaPlovila() {
        return odazvanaSluzbenaPlovila;
    }

    /**
     * Omogućava dobijanje vremena incidenta.
     *
     * @return Vrijeme incidenta.
     */
    public LocalDateTime getVrijeme() {
        return vrijeme;
    }

    /**
     * Omogućava dobijanje apsolutnih putanja do fotografija svih učesnika.
     *
     * @return Lista apsolutnih putanja do fotografija.
     */
    public List<String> getApsolutnePutanjeFotografija() {
        return apsolutnePutanjeFotografija;
    }

    /**
     * Omogućava dobijanje trajanja uviđaja.
     *
     * @return Trajanje uviđaja, u milisekundama.
     */
    public long getTrajanjeUvidjajaMs() {
        return trajanjeUvidjajaMs;
    }

    /**
     * Omogućava dobijanje rednog broja terminala na kojem se incident desio.
     *
     * @return Redni broj terminala.
     */
    public int getIdTerminala() {
        return idTerminala;
    }

    /**
     * Omogućava dobijanje tipa incidenta (sudar ili potjernica)
     *
     * @return Tip incidenta.
     *
     */
    public TipIncidenta getTip() {
        return tip;
    }

    /**
     * Upisuje ovaj incident kao binarni fajl u korisnički direktorijum
     * ({@code System.getProperty("user.home")}), jedan fajl po slučaju.
     *
     * @return Fajl u koji je incident upisan, ili {@code null} ako upis nije uspio.
     */
    public File sacuvaj() {
        return sacuvaj(new File(System.getProperty("user.home")));
    }

    /**
     * Upisuje ovaj incident kao binarni fajl u zadati direktorijum, <i>overload</i>-ovana metoda radi
     * testiranja, da testovi ne moraju pisati u
     * stvarni korisnički home direktorijum. Naziv fajla je jedinstven po pozivu
     * ({@code incident-<uuid>.ser}), tako da uzastopni incidenti na istom terminalu ne prepisuju
     * jedan drugog.
     *
     * @param direktorijum Direktorijum u koji se fajl upisuje. Mora već postojati.
     * @return Fajl u koji je incident upisan, ili {@code null} ako upis nije uspio.
     */
    public File sacuvaj(File direktorijum) {
        File fajl = new File(direktorijum, "incident-" + UUID.randomUUID() + ".ser");
        try (FileOutputStream fos = new FileOutputStream(fajl);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(this);
            return fajl;
        } catch (IOException ioe) {
            LoggerUtil.logError("Greska prilikom upisa incidenta u binarni fajl, ", ioe);
            return null;
        }
    }

    /**
     * Deserijalizuje prethodno sačuvan incident iz zadatog fajla.
     *
     * @param fajl Fajl iz kojeg se incident učitava.
     * @return Učitan incident, ili {@code null} ako čitanje nije uspjelo.
     */
    public static Incident ucitaj(File fajl) {
        try (FileInputStream fis = new FileInputStream(fajl);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (Incident) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            LoggerUtil.logError("Greska prilikom ucitavanja incidenta iz binarnog fajla, ", e);
            return null;
        }
    }
}
