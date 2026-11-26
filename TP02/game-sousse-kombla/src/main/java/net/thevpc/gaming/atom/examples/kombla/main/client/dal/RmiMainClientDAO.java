package net.thevpc.gaming.atom.examples.kombla.main.client.dal;

import net.thevpc.gaming.atom.examples.kombla.main.shared.dal.DalLog;
import net.thevpc.gaming.atom.examples.kombla.main.shared.dal.GameClientCallback;
import net.thevpc.gaming.atom.examples.kombla.main.shared.dal.GameServer;
import net.thevpc.gaming.atom.examples.kombla.main.shared.dal.RmiConstants;
import net.thevpc.gaming.atom.examples.kombla.main.shared.engine.AppConfig;
import net.thevpc.gaming.atom.examples.kombla.main.shared.model.DynamicGameModel;
import net.thevpc.gaming.atom.examples.kombla.main.shared.model.StartGameInfo;

import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class RmiMainClientDAO implements MainClientDAO {
    static class CallbackImpl extends UnicastRemoteObject implements GameClientCallback {
        private static final long serialVersionUID = 1L;
        private final transient MainClientDAOListener owner;

        CallbackImpl(MainClientDAOListener l) throws RemoteException {
            super();
            owner = l;
        }

        @Override
        public void onModelChanged(DynamicGameModel m) {
            owner.onModelChanged(m);
        }
    }

    private MainClientDAOListener listener;
    private AppConfig cfg;
    private CallbackImpl callback;
    private GameServer server;
    private volatile int playerId;
    private volatile boolean dead;

    @Override
    public void start(MainClientDAOListener l, AppConfig c) {
        this.listener = l;
        this.cfg = c;
        try {
            this.callback = new CallbackImpl(l);
        } catch (RemoteException e) {
            throw new RuntimeException("cannot export callback: " + e.getMessage(), e);
        }
    }

    @Override
    public StartGameInfo connect() {
        String host = cfg.getServerAddress() == null || cfg.getServerAddress().trim().isEmpty()
                ? "localhost" : cfg.getServerAddress().trim();
        int port = cfg.getServerPort();
        try {
            server = (GameServer) LocateRegistry.getRegistry(host, port).lookup(RmiConstants.BINDING_NAME);
            StartGameInfo info = server.join(ClientNames.resolve(cfg), callback);
            playerId = info.getPlayerId();
            DalLog.info("joined " + RmiConstants.url(host, port) + " as player " + playerId);
            return info;
        } catch (NotBoundException e) {
            throw new RuntimeException("no game bound as '" + RmiConstants.BINDING_NAME + "' at " + host + ":" + port, e);
        } catch (RemoteException e) {
            throw new RuntimeException("cannot join game at " + host + ":" + port + ": " + rootMessage(e), e);
        }
    }

    @Override
    public void sendMoveLeft() {
        if (alive()) {
            try {
                server.moveLeft(playerId);
            } catch (RemoteException e) {
                lost("moveLeft", e);
            }
        }
    }

    @Override
    public void sendMoveRight() {
        if (alive()) {
            try {
                server.moveRight(playerId);
            } catch (RemoteException e) {
                lost("moveRight", e);
            }
        }
    }

    @Override
    public void sendMoveUp() {
        if (alive()) {
            try {
                server.moveUp(playerId);
            } catch (RemoteException e) {
                lost("moveUp", e);
            }
        }
    }

    @Override
    public void sendMoveDown() {
        if (alive()) {
            try {
                server.moveDown(playerId);
            } catch (RemoteException e) {
                lost("moveDown", e);
            }
        }
    }

    @Override
    public void sendFire() {
        if (alive()) {
            try {
                server.fire(playerId);
            } catch (RemoteException e) {
                lost("fire", e);
            }
        }
    }

    private boolean alive() {
        return !dead && server != null;
    }

    private void lost(String call, RemoteException e) {
        dead = true;
        DalLog.info("server lost during " + call + "(" + playerId + "): " + rootMessage(e) + " - no retry");
    }

    public void close() {
        if (!dead && server != null) {
            try {
                server.leave(playerId);
            } catch (RemoteException ignored) {
            }
        }
        dead = true;
        try {
            UnicastRemoteObject.unexportObject(callback, true);
        } catch (NoSuchObjectException ignored) {
        }
    }

    void crash() throws NoSuchObjectException {
        dead = true;
        UnicastRemoteObject.unexportObject(callback, true);
    }

    public int getPlayerId() {
        return playerId;
    }

    public boolean isDead() {
        return dead;
    }

    private static String rootMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        return c.getMessage() == null ? c.getClass().getSimpleName() : c.getMessage();
    }
}
