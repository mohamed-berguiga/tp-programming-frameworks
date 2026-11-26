package tn.rnu.eniso.fwk.rmi.experiment;

import java.io.Serializable;

public class Box implements Serializable {
    private static final long serialVersionUID = 1L;

    int n;

    Box(int n) {
        this.n = n;
    }
}
