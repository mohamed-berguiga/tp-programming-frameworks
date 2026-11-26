package tn.rnu.eniso.fwk.iot.client;

import tn.rnu.eniso.fwk.iot.model.Packet;

import java.io.PrintStream;

public class ConsoleAlertSystem implements PacketListener {
    private static final String RED = "\u001B[31m";
    private static final String BOLD_RED = "\u001B[1;31m";
    private static final String RESET = "\u001B[0m";

    private final PrintStream out;
    private final boolean colors;

    public ConsoleAlertSystem() {
        this(System.out, true);
    }

    public ConsoleAlertSystem(PrintStream out, boolean colors) {
        this.out = out;
        this.colors = colors;
    }

    @Override
    public void onPacketReceived(Packet p) {
        out.println("[INFO]    " + p);
    }

    @Override
    public void onTemperatureAlert(Packet p) {
        String msg = String.format(java.util.Locale.ROOT, "[WARNING] COLD CHAIN BROKEN card=%d timeFrame=%d temperature=%.2f°C",
                p.getCardId(), p.getTimeFrame(), p.getTemperatureSensor());
        out.println(colors ? BOLD_RED + msg + RESET : msg);
    }

    @Override
    public void onConnectionError(String error) {
        String msg = "[ERROR]   " + error;
        out.println(colors ? RED + msg + RESET : msg);
    }
}
