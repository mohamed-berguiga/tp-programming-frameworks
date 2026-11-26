package net.thevpc.gaming.atom.examples.kombla.main.server.dal;

import net.thevpc.gaming.atom.examples.kombla.main.shared.engine.AppConfig;
import net.thevpc.gaming.atom.examples.kombla.main.shared.model.DynamicGameModel;

public interface MainServerDAO {
    void start(MainServerDAOListener listener, AppConfig properties);

    void sendModelChanged(DynamicGameModel dynamicGameModel);
}
