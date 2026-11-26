package tn.rnu.eniso.fwk.chat.push;

import tn.rnu.eniso.fwk.chat.common.Message;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

public class PushChatClient {
    static class CallbackImpl extends UnicastRemoteObject implements ChatClientCallback {
        private static final long serialVersionUID = 1L;

        CallbackImpl() throws RemoteException {
            super();
        }

        @Override
        public void onMessage(Message m) {
            System.out.println(m);
        }
    }

    private static CallbackImpl callback;

    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "rmi://localhost:1099/chat";
        String me = args.length > 1 ? args[1] : System.getProperty("user.name", "ada");
        ChatServer api = (ChatServer) Naming.lookup(url);
        callback = new CallbackImpl();
        api.join(me, callback);
        System.out.println("joined " + api.name() + " as " + me + ", type and press Enter");

        Scanner kb = new Scanner(System.in);
        try {
            while (kb.hasNextLine()) {
                api.send(new Message(me, kb.nextLine()));
            }
            api.leave(me);
        } catch (RemoteException e) {
            System.out.println("server lost: " + e.getMessage());
        } finally {
            UnicastRemoteObject.unexportObject(callback, true);
        }
    }
}
