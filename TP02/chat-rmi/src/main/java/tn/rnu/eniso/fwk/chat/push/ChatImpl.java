package tn.rnu.eniso.fwk.chat.push;

import tn.rnu.eniso.fwk.chat.common.Message;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatImpl extends UnicastRemoteObject implements ChatServer {
    private static final long serialVersionUID = 1L;

    private final Map<String, ChatClientCallback> room = new ConcurrentHashMap<>();

    public ChatImpl() throws RemoteException {
        super();
    }

    @Override
    public void join(String n, ChatClientCallback cb) throws RemoteException {
        if (n == null || n.trim().isEmpty() || n.length() > 16) {
            throw new RemoteException("bad name");
        }
        room.put(n, cb);
        System.out.println(n + " joined, room=" + room.keySet());
    }

    @Override
    public void send(Message m) {
        room.forEach((n, cb) -> {
            try {
                cb.onMessage(m);
            } catch (RemoteException e) {
                room.remove(n);
                System.out.println(n + " gone (" + e.getClass().getSimpleName() + ")");
            }
        });
    }

    @Override
    public void leave(String name) {
        if (room.remove(name) != null) {
            System.out.println(name + " left");
        }
    }

    @Override
    public String name() {
        return "push-chat";
    }

    public int size() {
        return room.size();
    }
}
