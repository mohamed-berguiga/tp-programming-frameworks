package tn.rnu.eniso.fwk.iot;

import org.junit.Test;
import tn.rnu.eniso.fwk.iot.model.Location;
import tn.rnu.eniso.fwk.iot.model.Packet;
import tn.rnu.eniso.fwk.iot.net.PacketCodec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;

public class PacketTest {
    private static Packet sample() {
        return new Packet.Builder()
                .cardId(0xCAFE0001L)
                .timeFrame(42)
                .temperature(-17.5)
                .gps(new Location(35.82, 10.63))
                .build();
    }

    @Test
    public void builtPacketIsValid() {
        assertTrue(sample().isValid());
    }

    @Test
    public void tamperedChecksumIsDetected() {
        Packet p = sample();
        Packet forged = new Packet.Builder()
                .cardId(p.getCardId()).timeFrame(p.getTimeFrame())
                .temperature(-5.0)
                .gps(p.getGpsSensor())
                .receivedChecksum(p.getChecksum())
                .build();
        assertFalse(forged.isValid());
    }

    @Test
    public void codecRoundTrip() throws IOException {
        Packet p = sample();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        PacketCodec.write(out, p);
        out.flush();
        assertEquals(PacketCodec.PACKET_SIZE, bytes.size());

        Packet back = PacketCodec.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        assertEquals(p.getCardId(), back.getCardId());
        assertEquals(p.getTimeFrame(), back.getTimeFrame());
        assertEquals(p.getTemperatureSensor(), back.getTemperatureSensor(), 0.0);
        assertEquals(p.getGpsSensor(), back.getGpsSensor());
        assertEquals(p.getChecksum(), back.getChecksum());
        assertTrue(back.isValid());
    }

    @Test(expected = IllegalStateException.class)
    public void gpsIsMandatory() {
        new Packet.Builder().cardId(1).build();
    }
}
