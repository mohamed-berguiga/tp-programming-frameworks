package net.thevpc.gaming.atom.examples.kombla.main.client.dal;

import net.thevpc.gaming.atom.examples.kombla.main.server.dal.MainServerDAOListener;
import net.thevpc.gaming.atom.examples.kombla.main.server.dal.RmiMainServerDAO;
import net.thevpc.gaming.atom.examples.kombla.main.shared.engine.AppConfig;
import net.thevpc.gaming.atom.examples.kombla.main.shared.model.DynamicGameModel;
import net.thevpc.gaming.atom.examples.kombla.main.shared.model.StartGameInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class RmiDalIntegrationTest {
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

        DynamicGameModel await(long frame) throws InterruptedException {
            long deadline = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < deadline) {
                DynamicGameModel m = models.poll(100, TimeUnit.MILLISECONDS);
                if (m != null && m.getFrame() == frame) {
                    return m;
                }
            }
            return null;
        }
    }

    private FakeEngine engine;
    private RmiMainServerDAO server;
    private int port;
    private final List<RmiMainClientDAO> clients = new ArrayList<>();

    @Before
    public void startServer() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        engine = new FakeEngine();
        server = new RmiMainServerDAO(2);
        server.start(engine, config("host"));
    }

    @After
    public void stop() {
        for (RmiMainClientDAO c : clients) {
            c.close();
        }
        server.close();
    }

    @Test(timeout = 30000)
    public void commandsReachTheEngineAndBothClientsReceiveTheModel() throws Exception {
        Inbox in1 = new Inbox();
        Inbox in2 = new Inbox();
        RmiMainClientDAO c1 = client(in1, "ada");
        RmiMainClientDAO c2 = client(in2, "bob");
        StartGameInfo i1 = c1.connect();
        StartGameInfo i2 = c2.connect();
        assertNotEquals(i1.getPlayerId(), i2.getPlayerId());
        assertArrayEquals(Fixtures.maze(), i1.getMaze());
        assertEquals("JOIN:ada:" + i1.getPlayerId(), engine.next());
        assertEquals("JOIN:bob:" + i2.getPlayerId(), engine.next());

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
        DynamicGameModel r1 = in1.await(100L);
        DynamicGameModel r2 = in2.await(100L);
        assertNotNull(r1);
        assertNotNull(r2);
        Fixtures.assertSameModel(m, r1);
        Fixtures.assertSameModel(m, r2);
        assertNotSame(m, r1);
        assertTrue("push must arrive on an RMI thread, was " + in1.thread, in1.thread.startsWith("RMI TCP Connection"));
    }

    @Test(timeout = 30000)
    public void crashedClientIsDroppedAndTheOtherKeepsPlaying() throws Exception {
        Inbox in1 = new Inbox();
        Inbox in2 = new Inbox();
        RmiMainClientDAO c1 = client(in1, "ada");
        RmiMainClientDAO c2 = client(in2, "bob");
        c1.connect();
        StartGameInfo i2 = c2.connect();
        assertEquals(2, server.getSessionCount());

        c1.crash();
        for (int f = 1; f <= 50 && server.getSessionCount() > 1; f++) {
            server.sendModelChanged(Fixtures.model(f));
            Thread.sleep(40);
        }
        assertEquals(1, server.getSessionCount());

        server.sendModelChanged(Fixtures.model(777L));
        assertNotNull(in2.await(777L));
        c2.sendMoveUp();
        String ev;
        do {
            ev = engine.next();
        } while (ev != null && !ev.startsWith("UP"));
        assertEquals("UP:" + i2.getPlayerId(), ev);
    }

    @Test(timeout = 30000)
    public void leaveFullGameAndRejoin() throws Exception {
        RmiMainClientDAO c1 = client(new Inbox(), "ada");
        int first = c1.connect().getPlayerId();
        client(new Inbox(), "bob").connect();
        try {
            client(new Inbox(), "eve").connect();
            fail("a full game must refuse");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("full"));
        }
        c1.close();
        assertEquals(1, server.getSessionCount());
        int again = client(new Inbox(), "ada").connect().getPlayerId();
        assertNotEquals(first, again);
    }

    @Test(timeout = 30000)
    public void commandAfterServerDeathIsNotRetried() throws Exception {
        RmiMainClientDAO c1 = client(new Inbox(), "ada");
        c1.connect();
        server.close();
        c1.sendFire();
        assertTrue(c1.isDead());
        c1.sendFire();
        startServer();
    }

    @Test(timeout = 10000)
    public void noServerGivesCleanError() throws Exception {
        RmiMainClientDAO c = new RmiMainClientDAO();
        AppConfig cfg = config("x");
        try (ServerSocket s = new ServerSocket(0)) {
            cfg.setServerPort(s.getLocalPort());
        }
        c.start(new Inbox(), cfg);
        clients.add(c);
        try {
            c.connect();
            fail();
        } catch (RuntimeException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("cannot join"));
        }
    }

    private RmiMainClientDAO client(MainClientDAOListener l, String name) {
        RmiMainClientDAO c = new RmiMainClientDAO();
        c.start(l, config(name));
        clients.add(c);
        return c;
    }

    private AppConfig config(String name) {
        AppConfig c = new AppConfig().setPlayerName(name);
        c.setServerAddress("localhost");
        c.setServerPort(port);
        return c;
    }
}
