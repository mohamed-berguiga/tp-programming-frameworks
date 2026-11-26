package net.thevpc.gaming.atom.examples.kombla.main.client.dal;

import net.thevpc.gaming.atom.examples.kombla.main.shared.model.StartGameInfo;

import net.thevpc.gaming.atom.examples.kombla.main.shared.engine.AppConfig;

public interface MainClientDAO {
    public void start(MainClientDAOListener listener, AppConfig properties);

    public StartGameInfo connect();

    public void sendMoveLeft();

    public void sendMoveRight();

    public void sendMoveUp();

    public void sendMoveDown();

    public void sendFire();
}
