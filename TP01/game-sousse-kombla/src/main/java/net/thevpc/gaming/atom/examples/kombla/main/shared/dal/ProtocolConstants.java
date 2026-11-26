package net.thevpc.gaming.atom.examples.kombla.main.shared.dal;

public final class ProtocolConstants {
    public static final int PROTOCOL_VERSION = 1;

    public static final int CONNECT = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;
    public static final int UP = 4;
    public static final int DOWN = 5;
    public static final int FIRE = 6;
    public static final int OK = 7;
    public static final int KO = 8;

    public static final int DEFAULT_MAX_REMOTE_PLAYERS = 3;
    public static final int MAX_NAME_LENGTH = 16;

    private ProtocolConstants() {
    }

    public static String codeName(int code) {
        switch (code) {
            case CONNECT: return "CONNECT";
            case LEFT: return "LEFT";
            case RIGHT: return "RIGHT";
            case UP: return "UP";
            case DOWN: return "DOWN";
            case FIRE: return "FIRE";
            case OK: return "OK";
            case KO: return "KO";
            default: return "UNKNOWN(" + code + ")";
        }
    }
}
