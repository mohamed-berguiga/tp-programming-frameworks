package tn.rnu.eniso.fwk.tp1.drill;

import java.util.Random;

public class PacketGenerator {
    private final long cardId;
    private final Random random;
    private long timeframe;
    private double latitude = 35.8256;
    private double longitude = 10.6084;

    public PacketGenerator(long cardId, Random random) {
        this.cardId = cardId;
        this.random = random;
    }

    public Packet next() {
        latitude += (random.nextDouble() - 0.5) * 0.001;
        longitude += (random.nextDouble() - 0.5) * 0.001;
        double temperature = 15 + random.nextDouble() * 25;
        return new Packet(temperature, latitude, longitude, cardId, timeframe++);
    }
}
