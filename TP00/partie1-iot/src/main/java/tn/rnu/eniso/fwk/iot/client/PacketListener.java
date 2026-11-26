package tn.rnu.eniso.fwk.iot.client;

import tn.rnu.eniso.fwk.iot.model.Packet;

public interface PacketListener {
    void onPacketReceived(Packet p);

    void onTemperatureAlert(Packet p);

    void onConnectionError(String error);
}
