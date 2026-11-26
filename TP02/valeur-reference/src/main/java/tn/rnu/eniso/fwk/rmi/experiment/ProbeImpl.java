package tn.rnu.eniso.fwk.rmi.experiment;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

class ProbeImpl extends UnicastRemoteObject implements Probe {
    private static final long serialVersionUID = 1L;

    private Box held;

    ProbeImpl() throws RemoteException {
        super();
    }

    @Override
    public synchronized int bump(Box b) {
        b.n = 99;
        held = b;
        return b.n;
    }

    @Override
    public synchronized Box last() {
        return held;
    }
}
