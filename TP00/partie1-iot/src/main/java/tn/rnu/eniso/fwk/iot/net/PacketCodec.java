package tn.rnu.eniso.fwk.iot.net;

import tn.rnu.eniso.fwk.iot.model.Location;
import tn.rnu.eniso.fwk.iot.model.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class PacketCodec {
    public static final int PACKET_SIZE = 8 + 8 + 8 + 8 + 8 + 4;

    private PacketCodec() {
    }

    public static void write(DataOutputStream out, Packet p) throws IOException {
        out.writeLong(p.getCardId());
        out.writeLong(p.getTimeFrame());
        out.writeDouble(p.getTemperatureSensor());
        out.writeDouble(p.getGpsSensor().getLatitude());
        out.writeDouble(p.getGpsSensor().getLongitude());
        out.writeInt(p.getChecksum());
    }

    public static Packet read(DataInputStream in) throws IOException {
        long cardId = in.readLong();
        long timeFrame = in.readLong();
        double temperature = in.readDouble();
        double lat = in.readDouble();
        double lon = in.readDouble();
        int checksum = in.readInt();
        return new Packet.Builder()
                .cardId(cardId)
                .timeFrame(timeFrame)
                .temperature(temperature)
                .gps(new Location(lat, lon))
                .receivedChecksum(checksum)
                .build();
    }
}
