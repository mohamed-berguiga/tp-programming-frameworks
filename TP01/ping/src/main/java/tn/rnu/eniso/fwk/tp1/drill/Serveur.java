package tn.rnu.eniso.fwk.tp1.drill;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class Serveur {
    public static final int DEFAULT_PORT = 1987;
    private static final AtomicLong CARD_IDS = new AtomicLong(1000);

    public static void main(String[] a) throws IOException {
        int port = a.length > 0 ? Integer.parseInt(a[0]) : DEFAULT_PORT;
        long period = a.length > 1 ? Long.parseLong(a[1]) : 200;
        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("Serveur listening on " + port);
            serve(ss, period);
        }
    }

    public static void serve(ServerSocket ss, long periodMs) {
        while (!ss.isClosed()) {
            try {
                Socket s = ss.accept();
                long cardId = CARD_IDS.incrementAndGet();
                Thread t = new Thread(() -> stream(s, cardId, periodMs), "card-" + cardId);
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                return;
            }
        }
    }

    static void stream(Socket s, long cardId, long periodMs) {
        PacketGenerator generator = new PacketGenerator(cardId, new Random());
        System.out.println("card " + cardId + " connected from " + s.getRemoteSocketAddress());
        try (Socket socket = s;
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {
            while (true) {
                generator.next().write(out);
                out.flush();
                Thread.sleep(periodMs);
            }
        } catch (IOException e) {
            System.out.println("card " + cardId + " disconnected");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
