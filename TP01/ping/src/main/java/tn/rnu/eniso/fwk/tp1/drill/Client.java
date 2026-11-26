package tn.rnu.eniso.fwk.tp1.drill;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

public class Client {
    public static final double THRESHOLD = 30.0;

    public static void main(String[] a) throws IOException {
        String host = a.length > 0 ? a[0] : "localhost";
        int port = a.length > 1 ? Integer.parseInt(a[1]) : Serveur.DEFAULT_PORT;
        receive(host, port, p -> System.out.println(p));
    }

    public static void receive(String host, int port, Consumer<Packet> display) throws IOException {
        try (Socket s = new Socket(host, port);
             DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()))) {
            while (true) {
                Packet p = Packet.read(in);
                if (p.isValid() && p.getTemperature() > THRESHOLD) {
                    display.accept(p);
                }
            }
        } catch (EOFException e) {
            System.out.println("server closed the connection");
        }
    }
}
