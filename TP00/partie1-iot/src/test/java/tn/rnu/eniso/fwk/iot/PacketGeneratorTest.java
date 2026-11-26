package tn.rnu.eniso.fwk.iot;

import org.junit.Test;
import tn.rnu.eniso.fwk.iot.model.Packet;
import tn.rnu.eniso.fwk.iot.simulation.CardReader;
import tn.rnu.eniso.fwk.iot.simulation.PacketGenerator;

import java.util.Random;

import static org.junit.Assert.*;

public class PacketGeneratorTest {
    @Test
    public void normalTemperaturesStayInBounds() {
        PacketGenerator g = new PacketGenerator(new CardReader(7), 0.0, new Random(1));
        for (int i = 0; i < 10_000; i++) {
            Packet p = g.generate();
            assertTrue(p.getTemperatureSensor() >= PacketGenerator.MIN_TEMP);
            assertTrue(p.getTemperatureSensor() <= PacketGenerator.MAX_TEMP);
            assertFalse(p.isColdChainBroken());
        }
    }

    @Test
    public void alertsExceedThreshold() {
        PacketGenerator g = new PacketGenerator(new CardReader(7), 1.0, new Random(2));
        for (int i = 0; i < 1000; i++) {
            Packet p = g.generate();
            assertTrue(p.getTemperatureSensor() > Packet.ALERT_THRESHOLD);
            assertTrue(p.getTemperatureSensor() <= PacketGenerator.MAX_ALERT_TEMP);
        }
    }

    @Test
    public void cardIdFixedAndTimeFrameIncremental() {
        PacketGenerator g = new PacketGenerator(new CardReader(99), 0.05, new Random(3));
        for (long i = 0; i < 100; i++) {
            Packet p = g.generate();
            assertEquals(99, p.getCardId());
            assertEquals(i, p.getTimeFrame());
            assertTrue(p.isValid());
        }
    }

    @Test
    public void gpsDriftsSlightly() {
        PacketGenerator g = new PacketGenerator(new CardReader(1), 0.0, new Random(4));
        Packet a = g.generate();
        Packet b = g.generate();
        assertEquals(a.getGpsSensor().getLatitude(), b.getGpsSensor().getLatitude(), 0.001);
        assertEquals(a.getGpsSensor().getLongitude(), b.getGpsSensor().getLongitude(), 0.001);
    }
}
