package tn.rnu.eniso.fwk.chat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tn.rnu.eniso.fwk.chat.common.Message;
import tn.rnu.eniso.fwk.chat.polling.ChatServerImpl;
import tn.rnu.eniso.fwk.chat.push.ChatClientCallback;
import tn.rnu.eniso.fwk.chat.push.ChatImpl;

import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ChatRmiTest {
    private Registry registry;
    private int port;

    static class Inbox extends UnicastRemoteObject implements ChatClientCallback {
        private static final long serialVersionUID = 1L;
        final BlockingQueue<Message> got = new LinkedBlockingQueue<>();

        Inbox() throws RemoteException {
            super();
        }

        public void onMessage(Message m) {
            got.add(m);
        }
    }

    @Before
    public void createRegistry() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        registry = LocateRegistry.createRegistry(port);
    }

    @After
    public void destroyRegistry() throws Exception {
        UnicastRemoteObject.unexportObject(registry, true);
    }

    @Test(timeout = 10000)
    public void pollingChatThroughStub() throws Exception {
        ChatServerImpl impl = new ChatServerImpl("t");
        registry.rebind("chat", impl);
        assertArrayEquals(new String[]{"chat"}, registry.list());
        tn.rnu.eniso.fwk.chat.polling.ChatServer api =
                (tn.rnu.eniso.fwk.chat.polling.ChatServer) Naming.lookup("rmi://localhost:" + port + "/chat");
        assertNull(api.read());
        Message sent = new Message("ada", "hi");
        api.send(sent);
        Message back = api.read();
        assertEquals("hi", back.text());
        assertNotSame(sent, back);
        UnicastRemoteObject.unexportObject(impl, true);
    }

    @Test(timeout = 10000)
    public void pushChatBroadcastsAndDropsDeadMembers() throws Exception {
        ChatImpl impl = new ChatImpl();
        registry.rebind("chat", impl);
        tn.rnu.eniso.fwk.chat.push.ChatServer api =
                (tn.rnu.eniso.fwk.chat.push.ChatServer) LocateRegistry.getRegistry("localhost", port).lookup("chat");
        Inbox ada = new Inbox();
        Inbox bob = new Inbox();
        api.join("ada", ada);
        api.join("bob", bob);

        api.send(new Message("ada", "hello"));
        assertEquals("hello", ada.got.poll(5, TimeUnit.SECONDS).text());
        assertEquals("hello", bob.got.poll(5, TimeUnit.SECONDS).text());

        UnicastRemoteObject.unexportObject(bob, true);
        api.send(new Message("ada", "still there?"));
        assertEquals("still there?", ada.got.poll(5, TimeUnit.SECONDS).text());
        assertEquals(1, impl.size());

        api.send(new Message("ada", "again"));
        assertEquals("again", ada.got.poll(5, TimeUnit.SECONDS).text());
        UnicastRemoteObject.unexportObject(ada, true);
        UnicastRemoteObject.unexportObject(impl, true);
    }
}
