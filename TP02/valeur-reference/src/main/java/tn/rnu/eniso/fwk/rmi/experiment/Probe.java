package tn.rnu.eniso.fwk.rmi.experiment;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Probe extends Remote {
    int bump(Box b) throws RemoteException;

    Box last() throws RemoteException;
}
