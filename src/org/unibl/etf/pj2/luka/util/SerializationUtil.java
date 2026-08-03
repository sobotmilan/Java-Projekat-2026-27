package org.unibl.etf.pj2.luka.util;

import org.unibl.etf.pj2.luka.model.classes.Luka;

import java.io.*;

public class SerializationUtil {
    private static final String DEFAULT_PATH = "luka.ser";

    public static void serijalizujStanjeLuke(Luka luka) {
        try(FileOutputStream fos = new FileOutputStream(DEFAULT_PATH); ObjectOutputStream oos = new ObjectOutputStream(fos)) {
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

        try(FileInputStream fis = new FileInputStream(DEFAULT_PATH); ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (Luka) ois.readObject();
        }catch(IOException | ClassNotFoundException e) {
            LoggerUtil.logError("Greska prilikom deserijalizacije stanja luke! Pokrece se nova konfiguracija", e);
            return null;
        }
    }
}
