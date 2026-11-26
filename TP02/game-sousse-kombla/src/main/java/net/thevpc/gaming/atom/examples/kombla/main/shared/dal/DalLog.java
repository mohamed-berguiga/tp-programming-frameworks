package net.thevpc.gaming.atom.examples.kombla.main.shared.dal;

public final class DalLog {
    public static final boolean DEBUG = Boolean.getBoolean("kombla.debug");

    private DalLog() {
    }

    public static void info(String msg) {
        System.out.println("[kombla-dal] " + msg);
    }

    public static void debug(String msg) {
        if (DEBUG) {
            System.out.println("[kombla-dal:" + Thread.currentThread().getName() + "] " + msg);
        }
    }
}
