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
 * Zapis jednog incidenta (I6): plovila koja su učestvovala u sudaru, službena plovila koja su se
 * odazvala, vrijeme incidenta, apsolutne putanje do fotografija svih učesnika, trajanje uviđaja i
 * terminal na kojem se incident desio.
 *
 * <p>Ovo je čist model podataka i njegov I/O (R4a) — ne bira učesnike sudara ni najbližu patrolu,
 * ne pokreće blokadu terminala niti sam upravlja trajanjem uviđaja. Ko konstruiše ovu klasu i kada
 * (koordinator uviđaja, po D1 iz {@code ZAHTJEVI.md}) je predmet R4b.</p>
 *
 * <p><b>Fotografije (D6):</b> čuvaju se apsolutne putanje ({@link File#getAbsolutePath()}), ne
 * bajtovi fotografija — jednostavnije i manje od bajtova, a apsolutna forma čini zapis čitljivim
 * bez obzira iz kojeg je radnog direktorijuma simulacija pokrenuta u trenutku nastanka incidenta
 * (za razliku od nekih drugih putanja u projektu koje ostaju relativne, vidi O2 u
 * {@code ZAHTJEVI.md}).</p>
 *
 * @author Milan Šobot
 * @version 1.0
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

    /** Apsolutne putanje do fotografija svih učesnika (sudar + odazvana službena plovila), D6. */
    private final List<String> apsolutnePutanjeFotografija;

    /** Trajanje uviđaja, u milisekundama (I3/I5). */
    private final long trajanjeUvidjajaMs;

    /** Redni broj terminala na kojem se incident desio. */
    private final int idTerminala;

    /** Vrsta incidenta (I5) — sudar ili potjernica. */
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

    private List<String> prikupiApsolutnePutanje() {
        List<String> putanje = new ArrayList<>();
        dodajPutanje(putanje, ucesniciSudara);
        dodajPutanje(putanje, odazvanaSluzbenaPlovila);
        return putanje;
    }

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
     * Omogućava dobijanje apsolutnih putanja do fotografija svih učesnika (D6).
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

    public TipIncidenta getTip() {
        return tip;
    }

    /**
     * Upisuje ovaj incident kao binarni fajl u korisnički home direktorijum
     * ({@code System.getProperty("user.home")}), jedan fajl po slučaju (I7).
     *
     * @return Fajl u koji je incident upisan, ili {@code null} ako upis nije uspio.
     */
    public File sacuvaj() {
        return sacuvaj(new File(System.getProperty("user.home")));
    }

    /**
     * Upisuje ovaj incident kao binarni fajl u zadati direktorijum — preopterećenje radi
     * testabilnosti (isti princip kao S5/R3 u ostatku projekta), da testovi ne moraju pisati u
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
     * Učitava prethodno sačuvan incident iz zadatog fajla.
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
