package net.thevpc.gaming.atom.examples.kombla.main.client.dal;

import net.thevpc.gaming.atom.examples.kombla.main.shared.model.DynamicGameModel;

public interface MainClientDAOListener {
    public void onModelChanged(DynamicGameModel model);
}
