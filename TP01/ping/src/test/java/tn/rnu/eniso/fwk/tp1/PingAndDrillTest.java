package tn.rnu.eniso.fwk.tp1;

import org.junit.Test;
import tn.rnu.eniso.fwk.tp1.drill.Client;
import tn.rnu.eniso.fwk.tp1.drill.Packet;
import tn.rnu.eniso.fwk.tp1.drill.Serveur;
import tn.rnu.eniso.fwk.tp1.ping.PingClient;
import tn.rnu.eniso.fwk.tp1.ping.PingServer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.Assert.*;

public class PingAndDrillTest {
    @Test(timeout = 10000)
    public void pingPong() throws Exception {
        try (ServerSocket ss = new ServerSocket(0)) {
            Thread server = new Thread(() -> PingServer.serve(ss));
            server.setDaemon(true);
            server.start();
            assertEquals("PONG ada", PingClient.ping("localhost", ss.getLocalPort(), "ada"));
            assertEquals("PONG bob", PingClient.ping("localhost", ss.getLocalPort(), "bob"));
        }
    }

    @Test
    public void packetRoundTrip() throws Exception {
        Packet p = new Packet(31.5, 35.8, 10.6, 42L, 7L);
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        p.write(new DataOutputStream(bs));
        assertEquals(Packet.SIZE, bs.size());
        Packet back = Packet.read(new DataInputStream(new ByteArrayInputStream(bs.toByteArray())));
        assertEquals(p.getTemperature(), back.getTemperature(), 0);
        assertEquals(p.getCardId(), back.getCardId());
        assertEquals(p.getTimeframe(), back.getTimeframe());
        assertEquals(p.getChecksum(), back.getChecksum());
        assertTrue(back.isValid());
    }

    @Test(timeout = 15000)
    public void clientOnlyShowsHotPackets() throws Exception {
        List<Packet> shown = new CopyOnWriteArrayList<>();
        try (ServerSocket ss = new ServerSocket(0)) {
            Thread server = new Thread(() -> Serveur.serve(ss, 2));
            server.setDaemon(true);
            server.start();
            Thread client = new Thread(() -> {
                try {
                    Client.receive("localhost", ss.getLocalPort(), shown::add);
                } catch (Exception ignored) {
                }
            });
            client.setDaemon(true);
            client.start();
            while (shown.size() < 20) {
                Thread.sleep(20);
            }
        }
        for (Packet p : shown) {
            assertTrue(p.getTemperature() > 30);
        }
    }
}
