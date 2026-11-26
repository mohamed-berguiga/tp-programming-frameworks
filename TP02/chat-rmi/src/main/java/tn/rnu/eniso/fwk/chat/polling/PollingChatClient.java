package tn.rnu.eniso.fwk.chat.polling;

import tn.rnu.eniso.fwk.chat.common.Message;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Scanner;

public class PollingChatClient {
    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "rmi://localhost:1099/chat";
        String me = args.length > 1 ? args[1] : System.getProperty("user.name", "ada");
        long pollMs = args.length > 2 ? Long.parseLong(args[2]) : 200;
        ChatServer api = (ChatServer) Naming.lookup(url);
        System.out.println("connected to " + api.name() + " as " + me + " (polling every " + pollMs + " ms)");

        Thread poller = new Thread(() -> {
            while (true) {
                try {
                    Message m = api.read();
                    if (m != null) {
                        System.out.println(m);
                    }
                    Thread.sleep(pollMs);
                } catch (RemoteException e) {
                    System.out.println("server lost: " + e.getMessage());
                    System.exit(1);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "chat-poller");
        poller.setDaemon(true);
        poller.start();

        Scanner kb = new Scanner(System.in);
        while (kb.hasNextLine()) {
            try {
                api.send(new Message(me, kb.nextLine()));
            } catch (RemoteException e) {
                System.out.println("server lost: " + e.getMessage());
                System.exit(1);
            }
        }
    }
}
