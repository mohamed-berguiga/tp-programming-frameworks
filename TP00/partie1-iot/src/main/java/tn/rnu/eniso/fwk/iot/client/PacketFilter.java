package tn.rnu.eniso.fwk.iot.client;

import tn.rnu.eniso.fwk.iot.model.Packet;

public interface PacketFilter {
    boolean accept(Packet p);

    default PacketFilter and(PacketFilter other) {
        return p -> accept(p) && other.accept(p);
    }

    default PacketFilter or(PacketFilter other) {
        return p -> accept(p) || other.accept(p);
    }

    static PacketFilter all() {
        return p -> true;
    }

    static PacketFilter byCardId(long cardId) {
        return p -> p.getCardId() == cardId;
    }

    static PacketFilter temperatureAbove(double threshold) {
        return p -> p.getTemperatureSensor() > threshold;
    }
}
