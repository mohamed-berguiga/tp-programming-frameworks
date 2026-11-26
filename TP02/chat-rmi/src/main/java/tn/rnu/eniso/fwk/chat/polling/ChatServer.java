package tn.rnu.eniso.fwk.chat.polling;

import tn.rnu.eniso.fwk.chat.common.Message;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ChatServer extends Remote {
    void send(Message m) throws RemoteException;

    Message read() throws RemoteException;

    String name() throws RemoteException;
}
