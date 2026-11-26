package net.thevpc.gaming.atom.examples.kombla.main.server.dal;

import net.thevpc.gaming.atom.examples.kombla.main.shared.dal.CountingOutputStream;
import net.thevpc.gaming.atom.examples.kombla.main.shared.dal.DalLog;
import net.thevpc.gaming.atom.examples.kombla.main.shared.dal.ProtocolCodec;
import net.thevpc.gaming.atom.examples.kombla.main.shared.dal.ProtocolConstants;
import net.thevpc.gaming.atom.examples.kombla.main.shared.engine.AppConfig;
import net.thevpc.gaming.atom.examples.kombla.main.shared.model.DynamicGameModel;
import net.thevpc.gaming.atom.examples.kombla.main.shared.model.StartGameInfo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SocketMainServerDAO implements MainServerDAO {
    private final int maxRemotePlayers;
    private final Map<Integer, ClientSession> sessions = new ConcurrentHashMap<>();
    private final Object engineLock = new Object();
    private final AtomicInteger sessionCounter = new AtomicInteger();
    private final AtomicLong framesSent = new AtomicLong();
    private final AtomicLong bytesSent = new AtomicLong();

    private volatile MainServerDAOListener listener;
    private ServerSocket serverSocket;
    private volatile boolean closed;

    public SocketMainServerDAO() {
        this(Integer.getInteger("kombla.maxPlayers", ProtocolConstants.DEFAULT_MAX_REMOTE_PLAYERS));
    }

    public SocketMainServerDAO(int maxRemotePlayers) {
        this.maxRemotePlayers = maxRemotePlayers;
    }

    @Override
    public void start(MainServerDAOListener listener, AppConfig properties) {
        this.listener = listener;
        int port = properties.getServerPort();
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException("cannot listen on port " + port + ": " + e.getMessage(), e);
        }
        Thread acceptor = new Thread(this::acceptLoop, "kombla-acceptor");
        acceptor.setDaemon(true);
        acceptor.start();
        DalLog.info("hosting on port " + serverSocket.getLocalPort() + " (max " + maxRemotePlayers + " remote players)");
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public int getSessionCount() {
        return sessions.size();
    }

    public long getFramesSent() {
        return framesSent.get();
    }

    public long getBytesSent() {
        return bytesSent.get();
    }

    private void acceptLoop() {
        while (!closed) {
            try {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                ClientSession session = new ClientSession(socket, sessionCounter.incrementAndGet());
                Thread t = new Thread(() -> processClient(session), "kombla-session-" + session.number);
                t.setDaemon(true);
                t.start();
            } catch (SocketException e) {
                return;
            } catch (IOException e) {
                DalLog.info("accept failed: " + e.getMessage());
            }
        }
    }

    private void processClient(ClientSession s) {
        try {
            s.open(bytesSent);
            if (!handshake(s)) {
                return;
            }
            while (!closed) {
                int code = s.in.readInt();
                DalLog.debug("<- " + ProtocolConstants.codeName(code) + " from player " + s.playerId);
                dispatch(s.playerId, code);
            }
        } catch (EOFException | SocketException e) {
        } catch (IOException e) {
            DalLog.info("session " + s.number + " protocol error: " + e.getMessage());
        } finally {
            removeSession(s, "disconnected");
        }
    }

    private boolean handshake(ClientSession s) throws IOException {
        int code = s.in.readInt();
        if (code != ProtocolConstants.CONNECT) {
            throw new IOException("expected CONNECT, got " + ProtocolConstants.codeName(code));
        }
        int version = s.in.readInt();
        String name = s.in.readUTF();
        String refusal = null;
        if (version != ProtocolConstants.PROTOCOL_VERSION) {
            refusal = "protocol version " + version + " not supported (server speaks " + ProtocolConstants.PROTOCOL_VERSION + ")";
        } else if (name.trim().isEmpty() || name.length() > ProtocolConstants.MAX_NAME_LENGTH) {
            refusal = "bad player name";
        } else if (sessions.size() >= maxRemotePlayers) {
            refusal = "game is full (" + maxRemotePlayers + " players)";
        }
        if (refusal != null) {
            synchronized (s.writeLock) {
                s.out.writeInt(ProtocolConstants.KO);
                s.out.writeUTF(refusal);
                s.out.flush();
            }
            DalLog.info("refused '" + name + "': " + refusal);
            return false;
        }
        StartGameInfo info;
        synchronized (engineLock) {
            info = listener.onReceivePlayerJoined(name);
        }
        s.playerId = info.getPlayerId();
        s.name = name;
        synchronized (s.writeLock) {
            s.out.writeInt(ProtocolConstants.OK);
            ProtocolCodec.writeStartGameInfo(s.out, info);
            s.out.flush();
        }
        sessions.put(s.playerId, s);
        DalLog.info("player " + s.playerId + " '" + name + "' joined from " + s.socket.getRemoteSocketAddress());
        return true;
    }

    private void dispatch(int playerId, int code) throws IOException {
        MainServerDAOListener l = listener;
        synchronized (engineLock) {
            switch (code) {
                case ProtocolConstants.LEFT:
                    l.onReceiveMoveLeft(playerId);
                    break;
                case ProtocolConstants.RIGHT:
                    l.onReceiveMoveRight(playerId);
                    break;
                case ProtocolConstants.UP:
                    l.onReceiveMoveUp(playerId);
                    break;
                case ProtocolConstants.DOWN:
                    l.onReceiveMoveDown(playerId);
                    break;
                case ProtocolConstants.FIRE:
                    l.onReceiveReleaseBomb(playerId);
                    break;
                default:
                    throw new IOException("unknown command " + code);
            }
        }
    }

    @Override
    public void sendModelChanged(DynamicGameModel dynamicGameModel) {
        if (sessions.isEmpty()) {
            return;
        }
        byte[] frame;
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024);
            DataOutputStream data = new DataOutputStream(buffer);
            data.writeInt(ProtocolConstants.OK);
            ProtocolCodec.writeModel(data, dynamicGameModel);
            data.flush();
            frame = buffer.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("cannot encode snapshot", e);
        }
        for (ClientSession s : sessions.values()) {
            try {
                synchronized (s.writeLock) {
                    s.out.write(frame);
                    s.out.flush();
                }
                framesSent.incrementAndGet();
            } catch (IOException e) {
                removeSession(s, "dropped during broadcast (" + e.getMessage() + ")");
            }
        }
        DalLog.debug("-> OK snapshot frame=" + dynamicGameModel.getFrame() + " " + frame.length + " bytes x " + sessions.size());
    }

    private void removeSession(ClientSession s, String why) {
        s.close();
        if (s.playerId != 0 && sessions.remove(s.playerId, s)) {
            DalLog.info("player " + s.playerId + " '" + s.name + "' " + why
                    + " (framesSent=" + framesSent.get() + ", bytesSent=" + bytesSent.get() + ")");
        }
    }

    public void close() {
        closed = true;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        for (ClientSession s : sessions.values()) {
            removeSession(s, "closed by host");
        }
    }

    private static final class ClientSession {
        final Socket socket;
        final int number;
        final Object writeLock = new Object();
        DataInputStream in;
        DataOutputStream out;
        volatile int playerId;
        volatile String name;

        ClientSession(Socket socket, int number) {
            this.socket = socket;
            this.number = number;
        }

        void open(AtomicLong bytesCounter) throws IOException {
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(new CountingOutputStream(socket.getOutputStream(), bytesCounter)));
        }

        void close() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
