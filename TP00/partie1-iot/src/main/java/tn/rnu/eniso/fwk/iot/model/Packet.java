package tn.rnu.eniso.fwk.iot.model;

public final class Packet {
    public static final double ALERT_THRESHOLD = -12.0;

    private final double temperatureSensor;
    private final Location gpsSensor;
    private final long cardId;
    private final long timeFrame;
    private final int checksum;

    private Packet(Builder b, int checksum) {
        this.temperatureSensor = b.temperature;
        this.gpsSensor = b.gps;
        this.cardId = b.cardId;
        this.timeFrame = b.timeFrame;
        this.checksum = checksum;
    }

    public double getTemperatureSensor() {
        return temperatureSensor;
    }

    public Location getGpsSensor() {
        return gpsSensor;
    }

    public long getCardId() {
        return cardId;
    }

    public long getTimeFrame() {
        return timeFrame;
    }

    public int getChecksum() {
        return checksum;
    }

    public boolean isValid() {
        return checksum == computeChecksum(temperatureSensor, gpsSensor, cardId, timeFrame);
    }

    public boolean isColdChainBroken() {
        return temperatureSensor > ALERT_THRESHOLD;
    }

    static int computeChecksum(double temperature, Location gps, long cardId, long timeFrame) {
        int h = 17;
        h = 31 * h + Long.hashCode(Double.doubleToLongBits(temperature));
        h = 31 * h + Long.hashCode(Double.doubleToLongBits(gps.getLatitude()));
        h = 31 * h + Long.hashCode(Double.doubleToLongBits(gps.getLongitude()));
        h = 31 * h + Long.hashCode(cardId);
        h = 31 * h + Long.hashCode(timeFrame);
        return h;
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "Packet{card=%d, t=%d, temp=%.2f°C, gps=%s, checksum=%08x}",
                cardId, timeFrame, temperatureSensor, gpsSensor, checksum);
    }

    public static final class Builder {
        private double temperature;
        private Location gps;
        private long cardId;
        private long timeFrame;
        private Integer receivedChecksum;

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder gps(Location gps) {
            this.gps = gps;
            return this;
        }

        public Builder cardId(long cardId) {
            this.cardId = cardId;
            return this;
        }

        public Builder timeFrame(long timeFrame) {
            this.timeFrame = timeFrame;
            return this;
        }

        public Builder receivedChecksum(int checksum) {
            this.receivedChecksum = checksum;
            return this;
        }

        public Packet build() {
            if (gps == null) {
                throw new IllegalStateException("gps is required");
            }
            if (Double.isNaN(temperature)) {
                throw new IllegalStateException("temperature is NaN");
            }
            int sum = receivedChecksum != null
                    ? receivedChecksum
                    : computeChecksum(temperature, gps, cardId, timeFrame);
            return new Packet(this, sum);
        }
    }
}
