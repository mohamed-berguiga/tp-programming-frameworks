package tn.rnu.eniso.fwk.iot.client;

import tn.rnu.eniso.fwk.iot.model.Packet;
import tn.rnu.eniso.fwk.iot.net.PacketCodec;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public class MonitoringClient {
    private final String host;
    private final int port;
    private final PacketListener listener;
    private volatile Socket socket;
    private volatile boolean stopped;

    public MonitoringClient(String host, int port, PacketListener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    public Thread start() {
        Thread t = new Thread(this::run, "iot-monitor");
        t.setDaemon(true);
        t.start();
        return t;
    }

    public void run() {
        try (Socket s = new Socket(host, port);
             DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()))) {
            this.socket = s;
            while (!stopped) {
                processIncomingPacket(PacketCodec.read(in));
            }
        } catch (EOFException e) {
            if (!stopped) {
                listener.onConnectionError("connection closed by server " + host + ":" + port);
            }
        } catch (IOException e) {
            if (!stopped) {
                listener.onConnectionError("cannot reach server " + host + ":" + port + " (" + e.getMessage() + ")");
            }
        }
    }

    public void stop() {
        stopped = true;
        Socket s = socket;
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void processIncomingPacket(Packet p) {
        if (!p.isValid()) {
            listener.onConnectionError("corrupted packet dropped (bad checksum): " + p);
            return;
        }
        listener.onPacketReceived(p);
        if (p.getTemperatureSensor() > Packet.ALERT_THRESHOLD) {
            listener.onTemperatureAlert(p);
        }
    }
}
