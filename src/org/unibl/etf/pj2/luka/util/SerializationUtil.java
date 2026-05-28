package org.unibl.etf.pj2.luka.util;

import org.unibl.etf.pj2.luka.model.classes.Luka;

import java.io.*;

public class SerializationUtil {
    private static final String DEFAULT_PATH = "luka.ser";

    public static void serijalizujStanjeLuke(Luka luka) {
        try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DEFAULT_PATH))) {
            oos.writeObject(luka);
        }catch(IOException ioe) {
            LoggerUtil.logError("Greska prilikom serijalizacije stanja luke!", ioe);
        }
    }

    public static Luka ucitajStanjeLuke() {
        File file = new File(DEFAULT_PATH);
        if(!file.exists()) {
            return null;
        }

        try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DEFAULT_PATH))) {
            return (Luka) ois.readObject();
        }catch(IOException | ClassNotFoundException e) {
            LoggerUtil.logError("Greska prilikom deserijalizacije stanja luke! Pokrece se nova konfiguracija", e);
            return null;
        }
    }
}
