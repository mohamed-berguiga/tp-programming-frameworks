package tn.rnu.eniso.fwk.chat.polling;

import tn.rnu.eniso.fwk.chat.common.Message;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayDeque;
import java.util.Queue;

public class ChatServerImpl extends UnicastRemoteObject implements ChatServer {
    private static final long serialVersionUID = 1L;

    private final Queue<Message> box = new ArrayDeque<>();
    private final String name;

    public ChatServerImpl(String name) throws RemoteException {
        super();
        this.name = name;
    }

    @Override
    public void send(Message m) {
        synchronized (box) {
            box.add(m);
        }
    }

    @Override
    public Message read() {
        synchronized (box) {
            return box.poll();
        }
    }

    @Override
    public String name() {
        return name;
    }
}
