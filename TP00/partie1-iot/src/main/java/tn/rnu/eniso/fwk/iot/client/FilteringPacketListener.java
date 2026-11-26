package tn.rnu.eniso.fwk.iot.client;

import tn.rnu.eniso.fwk.iot.model.Packet;

public class FilteringPacketListener implements PacketListener {
    private final PacketFilter filter;
    private final PacketListener delegate;

    public FilteringPacketListener(PacketFilter filter, PacketListener delegate) {
        this.filter = filter;
        this.delegate = delegate;
    }

    @Override
    public void onPacketReceived(Packet p) {
        if (filter.accept(p)) {
            delegate.onPacketReceived(p);
        }
    }

    @Override
    public void onTemperatureAlert(Packet p) {
        if (filter.accept(p)) {
            delegate.onTemperatureAlert(p);
        }
    }

    @Override
    public void onConnectionError(String error) {
        delegate.onConnectionError(error);
    }
}
