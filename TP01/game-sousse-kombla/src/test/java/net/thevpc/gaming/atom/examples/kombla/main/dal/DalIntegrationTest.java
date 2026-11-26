package net.thevpc.gaming.atom.examples.kombla.main.dal;

import net.thevpc.gaming.atom.examples.kombla.main.client.dal.MainClientDAO;
import net.thevpc.gaming.atom.examples.kombla.main.client.dal.MainClientDAOListener;
import net.thevpc.gaming.atom.examples.kombla.main.client.dal.ObjSocketMainClientDAO;
import net.thevpc.gaming.atom.examples.kombla.main.client.dal.SocketMainClientDAO;
import net.thevpc.gaming.atom.examples.kombla.main.server.dal.MainServerDAO;
import net.thevpc.gaming.atom.examples.kombla.main.server.dal.MainServerDAOListener;
import net.thevpc.gaming.atom.examples.kombla.main.server.dal.ObjSocketMainServerDAO;
import net.thevpc.gaming.atom.examples.kombla.main.server.dal.SocketMainServerDAO;
import net.thevpc.gaming.atom.examples.kombla.main.shared.engine.AppConfig;
import net.thevpc.gaming.atom.examples.kombla.main.shared.model.DynamicGameModel;
import net.thevpc.gaming.atom.examples.kombla.main.shared.model.StartGameInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class DalIntegrationTest {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> transports() {
        return Arrays.asList(new Object[][]{{"socket"}, {"obj"}});
    }

    private final String transport;
    private final List<MainClientDAO> clients = new ArrayList<>();
    private FakeEngine engine;
    private MainServerDAO server;
    private int port;

    public DalIntegrationTest(String transport) {
        this.transport = transport;
    }

    static class FakeEngine implements MainServerDAOListener {
        final AtomicInteger nextId = new AtomicInteger(1);
        final BlockingQueue<String> events = new LinkedBlockingQueue<>();

        public StartGameInfo onReceivePlayerJoined(String name) {
            int id = nextId.incrementAndGet();
            events.add("JOIN:" + name + ":" + id);
            return new StartGameInfo(id, Fixtures.maze());
        }

        public void onReceiveMoveLeft(int playerId) {
            events.add("LEFT:" + playerId);
        }

        public void onReceiveMoveRight(int playerId) {
            events.add("RIGHT:" + playerId);
        }

        public void onReceiveMoveUp(int playerId) {
            events.add("UP:" + playerId);
        }

        public void onReceiveMoveDown(int playerId) {
            events.add("DOWN:" + playerId);
        }

        public void onReceiveReleaseBomb(int playerId) {
            events.add("FIRE:" + playerId);
        }

        String next() throws InterruptedException {
            return events.poll(5, TimeUnit.SECONDS);
        }
    }

    static class Inbox implements MainClientDAOListener {
        final BlockingQueue<DynamicGameModel> models = new LinkedBlockingQueue<>();
        volatile String thread;

        public void onModelChanged(DynamicGameModel model) {
            thread = Thread.currentThread().getName();
            models.add(model);
        }

        DynamicGameModel next() throws InterruptedException {
            return models.poll(5, TimeUnit.SECONDS);
        }
    }

    @Before
    public void startServer() throws IOException {
        port = freePort();
        server = newServer(2);
        engine = new FakeEngine();
        server.start(engine, config("host", port));
    }

    @After
    public void stopAll() {
        for (MainClientDAO c : clients) {
            closeClient(c);
        }
        if (server instanceof SocketMainServerDAO) {
            ((SocketMainServerDAO) server).close();
        } else {
            ((ObjSocketMainServerDAO) server).close();
        }
    }

    @Test(timeout = 20000)
    public void twoPlayersCommandsAndBroadcast() throws Exception {
        Inbox inbox1 = new Inbox();
        Inbox inbox2 = new Inbox();
        MainClientDAO c1 = newClient(inbox1, "ada");
        MainClientDAO c2 = newClient(inbox2, "bob");
        StartGameInfo i1 = c1.connect();
        StartGameInfo i2 = c2.connect();

        assertNotEquals(i1.getPlayerId(), i2.getPlayerId());
        assertArrayEquals(Fixtures.maze(), i1.getMaze());
        assertEquals("JOIN:ada:" + i1.getPlayerId(), engine.next());
        assertEquals("JOIN:bob:" + i2.getPlayerId(), engine.next());
        waitSessions(2);

        c1.sendMoveLeft();
        assertEquals("LEFT:" + i1.getPlayerId(), engine.next());
        c2.sendFire();
        assertEquals("FIRE:" + i2.getPlayerId(), engine.next());
        c1.sendMoveRight();
        c1.sendMoveUp();
        c1.sendMoveDown();
        assertEquals("RIGHT:" + i1.getPlayerId(), engine.next());
        assertEquals("UP:" + i1.getPlayerId(), engine.next());
        assertEquals("DOWN:" + i1.getPlayerId(), engine.next());

        DynamicGameModel m = Fixtures.model(100L);
        server.sendModelChanged(m);
        Fixtures.assertSameModel(m, inbox1.next());
        Fixtures.assertSameModel(m, inbox2.next());
        assertEquals("kombla-reader", inbox1.thread);

        server.sendModelChanged(Fixtures.model(101L));
        assertEquals(101L, inbox1.next().getFrame());
        assertEquals(101L, inbox2.next().getFrame());
    }

    @Test(timeout = 20000)
    public void disconnectingOneClientDoesNotBreakTheOther() throws Exception {
        Inbox inbox1 = new Inbox();
        Inbox inbox2 = new Inbox();
        MainClientDAO c1 = newClient(inbox1, "ada");
        MainClientDAO c2 = newClient(inbox2, "bob");
        c1.connect();
        StartGameInfo i2 = c2.connect();
        waitSessions(2);

        closeClient(c1);
        for (int i = 0; i < 50 && sessionCount() > 1; i++) {
            server.sendModelChanged(Fixtures.model(i));
            Thread.sleep(20);
        }
        assertEquals(1, sessionCount());
        inbox2.models.clear();
        server.sendModelChanged(Fixtures.model(500L));
        DynamicGameModel received = inbox2.next();
        while (received != null && received.getFrame() != 500L) {
            received = inbox2.next();
        }
        assertNotNull(received);
        c2.sendFire();
        String ev;
        do {
            ev = engine.next();
        } while (ev != null && !ev.startsWith("FIRE"));
        assertEquals("FIRE:" + i2.getPlayerId(), ev);
    }

    @Test(timeout = 20000)
    public void rejoinWithSameNameGetsNewIdAndFullGameIsRefused() throws Exception {
        MainClientDAO c1 = newClient(new Inbox(), "ada");
        int first = c1.connect().getPlayerId();
        waitSessions(1);
        closeClient(c1);
        waitSessions(0);

        MainClientDAO again = newClient(new Inbox(), "ada");
        int second = again.connect().getPlayerId();
        assertNotEquals(first, second);

        newClient(new Inbox(), "bob").connect();
        waitSessions(2);
        MainClientDAO third = newClient(new Inbox(), "eve");
        try {
            third.connect();
            fail("a full game must answer KO");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("full"));
        }
    }

    @Test(timeout = 10000)
    public void connectWithoutServerFailsCleanly() throws Exception {
        MainClientDAO c = transport.equals("obj") ? new ObjSocketMainClientDAO() : new SocketMainClientDAO();
        c.start(new Inbox(), config("x", freePort()));
        try {
            c.connect();
            fail("expected a RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("cannot join"));
        }
    }

    private MainServerDAO newServer(int maxPlayers) {
        return transport.equals("obj") ? new ObjSocketMainServerDAO(maxPlayers) : new SocketMainServerDAO(maxPlayers);
    }

    private MainClientDAO newClient(MainClientDAOListener l, String name) {
        MainClientDAO c = transport.equals("obj") ? new ObjSocketMainClientDAO() : new SocketMainClientDAO();
        c.start(l, config(name, port));
        clients.add(c);
        return c;
    }

    private static void closeClient(MainClientDAO c) {
        if (c instanceof SocketMainClientDAO) {
            ((SocketMainClientDAO) c).close();
        } else {
            ((ObjSocketMainClientDAO) c).close();
        }
    }

    private int sessionCount() {
        return server instanceof SocketMainServerDAO
                ? ((SocketMainServerDAO) server).getSessionCount()
                : ((ObjSocketMainServerDAO) server).getSessionCount();
    }

    private void waitSessions(int n) throws InterruptedException {
        for (int i = 0; i < 250 && sessionCount() != n; i++) {
            Thread.sleep(20);
        }
        assertEquals(n, sessionCount());
    }

    private static AppConfig config(String name, int port) {
        AppConfig c = new AppConfig().setPlayerName(name);
        c.setServerAddress("localhost");
        c.setServerPort(port);
        return c;
    }

    private static int freePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }
}
