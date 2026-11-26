package net.thevpc.gaming.atom.examples.kombla.main.client.dal;

import net.thevpc.gaming.atom.examples.kombla.main.shared.dal.CountingOutputStream;
import net.thevpc.gaming.atom.examples.kombla.main.shared.dal.DalLog;
import net.thevpc.gaming.atom.examples.kombla.main.shared.dal.ProtocolCodec;
import net.thevpc.gaming.atom.examples.kombla.main.shared.dal.ProtocolConstants;
import net.thevpc.gaming.atom.examples.kombla.main.shared.engine.AppConfig;
import net.thevpc.gaming.atom.examples.kombla.main.shared.model.DynamicGameModel;
import net.thevpc.gaming.atom.examples.kombla.main.shared.model.StartGameInfo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicLong;

public class SocketMainClientDAO implements MainClientDAO {
    private volatile MainClientDAOListener listener;
    private AppConfig config;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final Object writeLock = new Object();
    private volatile boolean closed;
    private Thread reader;

    private final AtomicLong framesSent = new AtomicLong();
    private final AtomicLong bytesSent = new AtomicLong();
    private final AtomicLong framesReceived = new AtomicLong();

    @Override
    public void start(MainClientDAOListener listener, AppConfig properties) {
        this.listener = listener;
        this.config = properties;
    }

    @Override
    public StartGameInfo connect() {
        String host = config.getServerAddress() == null || config.getServerAddress().trim().isEmpty()
                ? "localhost" : config.getServerAddress().trim();
        int port = config.getServerPort();
        try {
            socket = new Socket(host, port);
            socket.setTcpNoDelay(true);
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(new CountingOutputStream(socket.getOutputStream(), bytesSent)));
            StartGameInfo info = handshake(ClientNames.resolve(config));
            reader = new Thread(this::onLoopReceiveModelChanged, "kombla-reader");
            reader.setDaemon(true);
            reader.start();
            DalLog.info("joined " + host + ":" + port + " as player " + info.getPlayerId());
            return info;
        } catch (IOException e) {
            closeQuietly();
            throw new RuntimeException("cannot join game at " + host + ":" + port + ": " + e.getMessage(), e);
        }
    }

    private StartGameInfo handshake(String playerName) throws IOException {
        synchronized (writeLock) {
            out.writeInt(ProtocolConstants.CONNECT);
            out.writeInt(ProtocolConstants.PROTOCOL_VERSION);
            out.writeUTF(playerName);
            out.flush();
            framesSent.incrementAndGet();
        }
        int code = in.readInt();
        if (code == ProtocolConstants.OK) {
            return ProtocolCodec.readStartGameInfo(in);
        }
        if (code == ProtocolConstants.KO) {
            String reason = in.readUTF();
            closeQuietly();
            throw new RuntimeException("join refused by server: " + reason);
        }
        throw new IOException("unexpected handshake frame " + ProtocolConstants.codeName(code));
    }

    private void onLoopReceiveModelChanged() {
        try {
            while (!closed) {
                int code = in.readInt();
                if (code != ProtocolConstants.OK) {
                    throw new IOException("unexpected frame " + ProtocolConstants.codeName(code));
                }
                DynamicGameModel model = ProtocolCodec.readModel(in);
                long n = framesReceived.incrementAndGet();
                DalLog.debug("<- OK snapshot #" + n + " frame=" + model.getFrame() + " sprites=" + model.getSprites().size());
                MainClientDAOListener l = listener;
                if (l != null) {
                    l.onModelChanged(model);
                }
            }
        } catch (EOFException | SocketException e) {
            if (!closed) {
                DalLog.info("server gone (" + e.getClass().getSimpleName() + "), reader stops");
            }
        } catch (IOException e) {
            DalLog.info("protocol error, reader stops: " + e.getMessage());
        } finally {
            closeQuietly();
            DalLog.info("client stats: framesSent=" + framesSent.get() + " bytesSent=" + bytesSent.get()
                    + " snapshotsReceived=" + framesReceived.get());
        }
    }

    @Override
    public void sendMoveLeft() {
        send(ProtocolConstants.LEFT);
    }

    @Override
    public void sendMoveRight() {
        send(ProtocolConstants.RIGHT);
    }

    @Override
    public void sendMoveUp() {
        send(ProtocolConstants.UP);
    }

    @Override
    public void sendMoveDown() {
        send(ProtocolConstants.DOWN);
    }

    @Override
    public void sendFire() {
        send(ProtocolConstants.FIRE);
    }

    private void send(int code) {
        if (closed || out == null) {
            return;
        }
        try {
            synchronized (writeLock) {
                out.writeInt(code);
                out.flush();
            }
            framesSent.incrementAndGet();
            DalLog.debug("-> " + ProtocolConstants.codeName(code));
        } catch (IOException e) {
            DalLog.info("cannot send " + ProtocolConstants.codeName(code) + ": " + e.getMessage());
            closeQuietly();
        }
    }

    public void close() {
        closeQuietly();
    }

    public boolean isClosed() {
        return closed;
    }

    public long getFramesSent() {
        return framesSent.get();
    }

    public long getBytesSent() {
        return bytesSent.get();
    }

    public long getFramesReceived() {
        return framesReceived.get();
    }

    private void closeQuietly() {
        closed = true;
        Socket s = socket;
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }
    }
}
