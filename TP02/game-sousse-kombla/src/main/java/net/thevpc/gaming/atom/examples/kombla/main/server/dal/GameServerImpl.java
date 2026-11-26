package net.thevpc.gaming.atom.examples.kombla.main.server.dal;

import net.thevpc.gaming.atom.examples.kombla.main.shared.dal.DalLog;
import net.thevpc.gaming.atom.examples.kombla.main.shared.dal.GameClientCallback;
import net.thevpc.gaming.atom.examples.kombla.main.shared.dal.GameServer;
import net.thevpc.gaming.atom.examples.kombla.main.shared.dal.RmiConstants;
import net.thevpc.gaming.atom.examples.kombla.main.shared.model.DynamicGameModel;
import net.thevpc.gaming.atom.examples.kombla.main.shared.model.StartGameInfo;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class GameServerImpl extends UnicastRemoteObject implements GameServer {
    private static final long serialVersionUID = 1L;

    private final transient MainServerDAOListener engine;
    private final int maxRemotePlayers;
    private final transient Map<Integer, GameClientCallback> sessions = new ConcurrentHashMap<>();
    private final transient Map<Integer, String> names = new ConcurrentHashMap<>();
    private final transient Object engineLock = new Object();

    private final transient AtomicReference<DynamicGameModel> pending = new AtomicReference<>();
    private final transient ExecutorService push = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "kombla-push");
        t.setDaemon(true);
        return t;
    });
    private final transient AtomicLong calls = new AtomicLong();
    private final transient AtomicLong pushes = new AtomicLong();

    public GameServerImpl(MainServerDAOListener engine, int maxRemotePlayers) throws RemoteException {
        super();
        this.engine = engine;
        this.maxRemotePlayers = maxRemotePlayers;
    }

    @Override
    public StartGameInfo join(String name, GameClientCallback cb) throws RemoteException {
        calls.incrementAndGet();
        if (name == null || name.trim().isEmpty() || name.length() > RmiConstants.MAX_NAME_LENGTH) {
            throw new RemoteException("bad name");
        }
        if (cb == null) {
            throw new RemoteException("missing callback");
        }
        StartGameInfo info;
        synchronized (engineLock) {
            if (sessions.size() >= maxRemotePlayers) {
                throw new RemoteException("game is full (" + maxRemotePlayers + " players)");
            }
            info = engine.onReceivePlayerJoined(name);
            sessions.put(info.getPlayerId(), cb);
            names.put(info.getPlayerId(), name);
        }
        DalLog.info("player " + info.getPlayerId() + " '" + name + "' joined");
        return info;
    }

    @Override
    public void moveLeft(int playerId) throws RemoteException {
        checkJoined(playerId);
        synchronized (engineLock) {
            engine.onReceiveMoveLeft(playerId);
        }
    }

    @Override
    public void moveRight(int playerId) throws RemoteException {
        checkJoined(playerId);
        synchronized (engineLock) {
            engine.onReceiveMoveRight(playerId);
        }
    }

    @Override
    public void moveUp(int playerId) throws RemoteException {
        checkJoined(playerId);
        synchronized (engineLock) {
            engine.onReceiveMoveUp(playerId);
        }
    }

    @Override
    public void moveDown(int playerId) throws RemoteException {
        checkJoined(playerId);
        synchronized (engineLock) {
            engine.onReceiveMoveDown(playerId);
        }
    }

    @Override
    public void fire(int playerId) throws RemoteException {
        checkJoined(playerId);
        synchronized (engineLock) {
            engine.onReceiveReleaseBomb(playerId);
        }
    }

    @Override
    public void leave(int playerId) {
        calls.incrementAndGet();
        drop(playerId, "left the game");
    }

    private void checkJoined(int playerId) throws RemoteException {
        calls.incrementAndGet();
        if (!sessions.containsKey(playerId)) {
            throw new RemoteException("unknown player " + playerId);
        }
    }

    void broadcast(DynamicGameModel m) {
        if (sessions.isEmpty()) {
            return;
        }
        pending.set(m);
        push.execute(this::pushPending);
    }

    private void pushPending() {
        DynamicGameModel s = pending.getAndSet(null);
        if (s == null) {
            return;
        }
        for (Map.Entry<Integer, GameClientCallback> e : sessions.entrySet()) {
            try {
                e.getValue().onModelChanged(s);
                pushes.incrementAndGet();
            } catch (RemoteException x) {
                drop(e.getKey(), "gone (" + x.getClass().getSimpleName() + ")");
            }
        }
        DalLog.debug("pushed frame " + s.getFrame() + " to " + sessions.size() + " players");
    }

    private void drop(int playerId, String why) {
        if (sessions.remove(playerId) != null) {
            String name = names.remove(playerId);
            DalLog.info("player " + playerId + " '" + name + "' " + why
                    + " (calls=" + calls.get() + ", pushes=" + pushes.get() + ")");
        }
    }

    public int getSessionCount() {
        return sessions.size();
    }

    public long getCallCount() {
        return calls.get();
    }

    void shutdown() {
        push.shutdownNow();
        try {
            push.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        sessions.clear();
    }
}
