package net.thevpc.gaming.atom.examples.kombla.main.shared.dal;

public final class RmiConstants {
    public static final String BINDING_NAME = "kombla-main";
    public static final int DEFAULT_MAX_REMOTE_PLAYERS = 3;
    public static final int MAX_NAME_LENGTH = 16;
    public static final String RESPONSE_TIMEOUT_MS = "5000";

    private RmiConstants() {
    }

    public static String url(String host, int port) {
        return "rmi://" + host + ":" + port + "/" + BINDING_NAME;
    }
}
