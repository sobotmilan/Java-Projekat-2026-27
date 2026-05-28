package org.unibl.etf.pj2.luka.model.classes;

import org.unibl.etf.pj2.luka.model.interfaces.Vatrogasci;

import java.io.File;

public class TankerVatrogasci extends Tanker implements Vatrogasci {
    public TankerVatrogasci(String naziv, String imoBroj, String brojMotora, String registarskiBroj, File fotografija, double zapreminaBarel) {
        super(naziv, imoBroj, brojMotora, registarskiBroj, fotografija, zapreminaBarel);
    }
}
