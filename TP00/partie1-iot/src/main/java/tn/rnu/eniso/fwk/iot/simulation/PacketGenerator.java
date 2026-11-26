package tn.rnu.eniso.fwk.iot.simulation;

import tn.rnu.eniso.fwk.iot.model.Location;
import tn.rnu.eniso.fwk.iot.model.Packet;

import java.util.Random;

public class PacketGenerator {
    public static final double IDEAL_TEMP = -18.0;
    public static final double MIN_TEMP = -30.0;
    public static final double MAX_TEMP = -12.0;
    public static final double MAX_ALERT_TEMP = -5.0;

    private final CardReader reader;
    private final Random random;
    private final double alertProbability;
    private long currentTimeFrame = 0;
    private Location position;

    public PacketGenerator(CardReader reader) {
        this(reader, 0.05, new Random());
    }

    public PacketGenerator(CardReader reader, double alertProbability, Random random) {
        if (alertProbability < 0 || alertProbability > 1) {
            throw new IllegalArgumentException("alertProbability must be in [0,1]");
        }
        this.reader = reader;
        this.alertProbability = alertProbability;
        this.random = random;
        this.position = new Location(35.8256 + random.nextDouble() * 0.05, 10.6084 + random.nextDouble() * 0.05);
    }

    public Packet generate() {
        return new Packet.Builder()
                .cardId(reader.getId())
                .timeFrame(currentTimeFrame++)
                .temperature(readSimulatedTemp())
                .gps(readSimulatedGPS())
                .build();
    }

    private double readSimulatedTemp() {
        if (random.nextDouble() < alertProbability) {
            return MAX_TEMP + 0.1 + random.nextDouble() * (MAX_ALERT_TEMP - MAX_TEMP - 0.1);
        }
        double t = IDEAL_TEMP + random.nextGaussian() * 2.0;
        return Math.max(MIN_TEMP, Math.min(MAX_TEMP, t));
    }

    private Location readSimulatedGPS() {
        position = position.moveBy((random.nextDouble() - 0.5) * 0.0005, (random.nextDouble() - 0.5) * 0.0005);
        return position;
    }
}
