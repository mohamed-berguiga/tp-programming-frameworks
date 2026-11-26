package tn.rnu.eniso.fwk.iot;

import org.junit.Test;
import tn.rnu.eniso.fwk.iot.client.FilteringPacketListener;
import tn.rnu.eniso.fwk.iot.client.MonitoringClient;
import tn.rnu.eniso.fwk.iot.client.PacketFilter;
import tn.rnu.eniso.fwk.iot.client.PacketListener;
import tn.rnu.eniso.fwk.iot.model.Packet;
import tn.rnu.eniso.fwk.iot.net.SimulationServer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ServerClientIntegrationTest {
    static class Recorder implements PacketListener {
        final List<Packet> received = new CopyOnWriteArrayList<>();
        final List<Packet> alerts = new CopyOnWriteArrayList<>();
        final List<String> errors = new CopyOnWriteArrayList<>();
        final CountDownLatch alertLatch = new CountDownLatch(1);
        final CountDownLatch packetsLatch;
        final CountDownLatch errorLatch = new CountDownLatch(1);

        Recorder(int expectedPackets) {
            packetsLatch = new CountDownLatch(expectedPackets);
        }

        public void onPacketReceived(Packet p) {
            received.add(p);
            packetsLatch.countDown();
        }

        public void onTemperatureAlert(Packet p) {
            alerts.add(p);
            alertLatch.countDown();
        }

        public void onConnectionError(String error) {
            errors.add(error);
            errorLatch.countDown();
        }
    }

    @Test(timeout = 15000)
    public void alertIsReceivedWhenTemperatureExceedsThreshold() throws Exception {
        SimulationServer server = new SimulationServer(0, 2, 20, 0.5);
        server.start();
        try {
            Recorder rec = new Recorder(20);
            MonitoringClient client = new MonitoringClient("localhost", server.getPort(), rec);
            client.start();
            assertTrue("no alert received", rec.alertLatch.await(10, TimeUnit.SECONDS));
            assertTrue("not enough packets", rec.packetsLatch.await(10, TimeUnit.SECONDS));
            for (Packet a : rec.alerts) {
                assertTrue(a.getTemperatureSensor() > -12.0);
                assertTrue(rec.received.contains(a));
            }
            for (Packet p : rec.received) {
                assertTrue(p.isValid());
            }
            assertEquals(2, rec.received.stream().mapToLong(Packet::getCardId).distinct().count());
            client.stop();
        } finally {
            server.stop();
        }
    }

    @Test(timeout = 15000)
    public void filtersAreCombinable() throws Exception {
        SimulationServer server = new SimulationServer(0, 3, 10, 0.5);
        server.start();
        try {
            long wanted = 0xCAFE0002L;
            Recorder rec = new Recorder(10);
            PacketFilter f = PacketFilter.byCardId(wanted).and(PacketFilter.temperatureAbove(-12.0));
            MonitoringClient client = new MonitoringClient("localhost", server.getPort(), new FilteringPacketListener(f, rec));
            client.start();
            assertTrue(rec.packetsLatch.await(10, TimeUnit.SECONDS));
            for (Packet p : rec.received) {
                assertEquals(wanted, p.getCardId());
                assertTrue(p.getTemperatureSensor() > -12.0);
            }
            client.stop();
        } finally {
            server.stop();
        }
    }

    @Test(timeout = 10000)
    public void unreachableServerReportsConnectionError() throws Exception {
        java.net.ServerSocket tmp = new java.net.ServerSocket(0);
        int freePort = tmp.getLocalPort();
        tmp.close();
        Recorder rec = new Recorder(1);
        new MonitoringClient("localhost", freePort, rec).run();
        assertEquals(1, rec.errors.size());
        assertTrue(rec.errors.get(0).contains("cannot reach"));
    }

    @Test(timeout = 15000)
    public void serverShutdownReportsConnectionError() throws Exception {
        SimulationServer server = new SimulationServer(0, 1, 20, 0.0);
        server.start();
        Recorder rec = new Recorder(3);
        MonitoringClient client = new MonitoringClient("localhost", server.getPort(), rec);
        client.start();
        assertTrue(rec.packetsLatch.await(5, TimeUnit.SECONDS));
        server.stop();
        assertTrue("connection drop not reported", rec.errorLatch.await(5, TimeUnit.SECONDS));
    }
}
