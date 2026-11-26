package tn.rnu.eniso.fwk.tp1.drill;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;

public final class Packet {
    public static final int SIZE = 8 + 8 + 8 + 8 + 8 + 4;

    private final double temperature;
    private final double latitude;
    private final double longitude;
    private final long cardId;
    private final long timeframe;
    private final int checksum;

    public Packet(double temperature, double latitude, double longitude, long cardId, long timeframe) {
        this(temperature, latitude, longitude, cardId, timeframe,
                computeChecksum(temperature, latitude, longitude, cardId, timeframe));
    }

    private Packet(double temperature, double latitude, double longitude, long cardId, long timeframe, int checksum) {
        this.temperature = temperature;
        this.latitude = latitude;
        this.longitude = longitude;
        this.cardId = cardId;
        this.timeframe = timeframe;
        this.checksum = checksum;
    }

    static int computeChecksum(double temperature, double latitude, double longitude, long cardId, long timeframe) {
        int h = 17;
        h = 31 * h + Double.hashCode(temperature);
        h = 31 * h + Double.hashCode(latitude);
        h = 31 * h + Double.hashCode(longitude);
        h = 31 * h + Long.hashCode(cardId);
        h = 31 * h + Long.hashCode(timeframe);
        return h;
    }

    public boolean isValid() {
        return checksum == computeChecksum(temperature, latitude, longitude, cardId, timeframe);
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeDouble(temperature);
        out.writeDouble(latitude);
        out.writeDouble(longitude);
        out.writeLong(cardId);
        out.writeLong(timeframe);
        out.writeInt(checksum);
    }

    public static Packet read(DataInputStream in) throws IOException {
        double temperature = in.readDouble();
        double latitude = in.readDouble();
        double longitude = in.readDouble();
        long cardId = in.readLong();
        long timeframe = in.readLong();
        int checksum = in.readInt();
        return new Packet(temperature, latitude, longitude, cardId, timeframe, checksum);
    }

    public double getTemperature() {
        return temperature;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public long getCardId() {
        return cardId;
    }

    public long getTimeframe() {
        return timeframe;
    }

    public int getChecksum() {
        return checksum;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "Packet{card=%d, t=%d, temp=%.1f, gps=(%.5f,%.5f), checksum=%08x}",
                cardId, timeframe, temperature, latitude, longitude, checksum);
    }
}
