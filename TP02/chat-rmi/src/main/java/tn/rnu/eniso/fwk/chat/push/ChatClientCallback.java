package tn.rnu.eniso.fwk.chat.push;

import tn.rnu.eniso.fwk.chat.common.Message;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ChatClientCallback extends Remote {
    void onMessage(Message m) throws RemoteException;
}
