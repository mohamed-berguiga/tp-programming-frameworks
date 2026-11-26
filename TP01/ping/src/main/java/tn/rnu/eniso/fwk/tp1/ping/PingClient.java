package tn.rnu.eniso.fwk.tp1.ping;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class PingClient {
    public static void main(String[] a) throws IOException {
        String host = a.length > 0 ? a[0] : "localhost";
        int port = a.length > 1 ? Integer.parseInt(a[1]) : PingServer.DEFAULT_PORT;
        String name = a.length > 2 ? a[2] : "ada";
        System.out.println(ping(host, port, name));
    }

    public static String ping(String host, int port, String name) throws IOException {
        try (Socket s = new Socket(host, port);
             DataInputStream in = new DataInputStream(s.getInputStream());
             DataOutputStream out = new DataOutputStream(s.getOutputStream())) {
            out.writeUTF(name);
            out.flush();
            return in.readUTF();
        }
    }
}
