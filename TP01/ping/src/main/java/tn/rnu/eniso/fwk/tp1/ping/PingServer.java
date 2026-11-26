package tn.rnu.eniso.fwk.tp1.ping;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class PingServer {
    public static final int DEFAULT_PORT = 7070;

    public static void main(String[] a) throws IOException {
        int port = a.length > 0 ? Integer.parseInt(a[0]) : DEFAULT_PORT;
        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("listening on " + port);
            serve(ss);
        }
    }

    public static void serve(ServerSocket ss) {
        while (!ss.isClosed()) {
            try {
                Socket s = ss.accept();
                new Thread(() -> handle(s), "ping-client").start();
            } catch (IOException e) {
                return;
            }
        }
    }

    static void handle(Socket s) {
        try (Socket socket = s;
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            String name = in.readUTF();
            System.out.println("hello from " + name);
            out.writeUTF("PONG " + name);
            out.flush();
        } catch (IOException e) {
            System.err.println("client error: " + e.getMessage());
        }
    }
}
