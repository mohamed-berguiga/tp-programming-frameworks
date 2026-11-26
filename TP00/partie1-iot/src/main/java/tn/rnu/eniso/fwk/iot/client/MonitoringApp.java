package tn.rnu.eniso.fwk.iot.client;

public class MonitoringApp {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 1987;
        PacketFilter filter = PacketFilter.all();
        int positional = 0;
        for (String a : args) {
            if (a.startsWith("--card=")) {
                filter = filter.and(PacketFilter.byCardId(Long.decode(a.substring("--card=".length()))));
            } else if (a.equals("--alerts")) {
                filter = filter.and(PacketFilter.temperatureAbove(-12.0));
            } else if (positional == 0) {
                host = a;
                positional++;
            } else {
                port = Integer.parseInt(a);
                positional++;
            }
        }
        PacketListener display = new FilteringPacketListener(filter, new ConsoleAlertSystem());
        new MonitoringClient(host, port, display).run();
    }
}
