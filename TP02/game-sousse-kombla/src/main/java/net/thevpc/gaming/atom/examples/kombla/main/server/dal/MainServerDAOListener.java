package net.thevpc.gaming.atom.examples.kombla.main.server.dal;

import net.thevpc.gaming.atom.examples.kombla.main.shared.model.StartGameInfo;

public interface MainServerDAOListener {
    StartGameInfo onReceivePlayerJoined(String name);

    void onReceiveMoveLeft(int playerId);

    void onReceiveMoveRight(int playerId);

    void onReceiveMoveUp(int playerId);

    void onReceiveMoveDown(int playerId);

    void onReceiveReleaseBomb(int playerId);
}
