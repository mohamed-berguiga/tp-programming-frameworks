package net.thevpc.gaming.atom.examples.kombla.main.client.dal;

import net.thevpc.gaming.atom.examples.kombla.main.shared.engine.AppConfig;

final class ClientNames {
    private ClientNames() {
    }

    static String resolve(AppConfig config) {
        String name = config == null ? null : config.getPlayerName();
        if (name == null || name.trim().isEmpty()) {
            name = System.getProperty("kombla.playerName", System.getProperty("user.name", "player"));
        }
        name = name.trim();
        return name.length() > 16 ? name.substring(0, 16) : name;
    }
}
