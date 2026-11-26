package net.thevpc.gaming.atom.examples.kombla.main.server.dal;

import net.thevpc.gaming.atom.examples.kombla.main.shared.dal.DalLog;
import net.thevpc.gaming.atom.examples.kombla.main.shared.dal.RmiConstants;
import net.thevpc.gaming.atom.examples.kombla.main.shared.engine.AppConfig;
import net.thevpc.gaming.atom.examples.kombla.main.shared.model.DynamicGameModel;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;

public class RmiMainServerDAO implements MainServerDAO {
    private final int maxRemotePlayers;
    private Registry registry;
    private GameServerImpl servant;

    public RmiMainServerDAO() {
        this(Integer.getInteger("kombla.maxPlayers", RmiConstants.DEFAULT_MAX_REMOTE_PLAYERS));
    }

    public RmiMainServerDAO(int maxRemotePlayers) {
        this.maxRemotePlayers = maxRemotePlayers;
    }

    @Override
    public void start(MainServerDAOListener listener, AppConfig properties) {
        if (System.getProperty("sun.rmi.transport.tcp.responseTimeout") == null) {
            System.setProperty("sun.rmi.transport.tcp.responseTimeout", RmiConstants.RESPONSE_TIMEOUT_MS);
        }
        int port = properties.getServerPort();
        try {
            registry = LocateRegistry.createRegistry(port);
            servant = new GameServerImpl(listener, maxRemotePlayers);
            registry.rebind(RmiConstants.BINDING_NAME, servant);
            DalLog.info("bound: " + Arrays.toString(registry.list()) + " on registry port " + port);
        } catch (RemoteException e) {
            throw new RuntimeException("cannot bind " + RmiConstants.BINDING_NAME + " on port " + port + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void sendModelChanged(DynamicGameModel dynamicGameModel) {
        servant.broadcast(dynamicGameModel);
    }

    public int getSessionCount() {
        return servant.getSessionCount();
    }

    public void close() {
        try {
            registry.unbind(RmiConstants.BINDING_NAME);
        } catch (Exception ignored) {
        }
        servant.shutdown();
        unexport(servant);
        unexport(registry);
    }

    private static void unexport(java.rmi.Remote r) {
        try {
            UnicastRemoteObject.unexportObject(r, true);
        } catch (NoSuchObjectException ignored) {
        }
    }
}
