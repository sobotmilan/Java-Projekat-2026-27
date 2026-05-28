package org.unibl.etf.pj2.luka.main;

import org.unibl.etf.pj2.luka.model.classes.Luka;
import org.unibl.etf.pj2.luka.util.SerializationUtil;

public class Main {
    public static void main(String[] args) {
        Luka luka = SerializationUtil.ucitajStanjeLuke();
        if(luka == null) {
            // calls into the creation of a new Luka object which will be assigned to luka.
        }

        // calls into main methods for working with Luka luka.

    }
}