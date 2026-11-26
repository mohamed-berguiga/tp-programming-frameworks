package tn.rnu.eniso.fwk.chat.push;

import tn.rnu.eniso.fwk.chat.common.Message;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ChatServer extends Remote {
    void join(String name, ChatClientCallback cb) throws RemoteException;

    void send(Message m) throws RemoteException;

    void leave(String name) throws RemoteException;

    String name() throws RemoteException;
}
