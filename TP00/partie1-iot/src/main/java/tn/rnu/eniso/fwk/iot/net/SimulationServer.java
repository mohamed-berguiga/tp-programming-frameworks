package tn.rnu.eniso.fwk.iot.net;

import tn.rnu.eniso.fwk.iot.model.Packet;
import tn.rnu.eniso.fwk.iot.simulation.CardReader;
import tn.rnu.eniso.fwk.iot.simulation.PacketGenerator;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulationServer {
    public static final int DEFAULT_PORT = 1987;

    private final int requestedPort;
    private final int cardCount;
    private final long periodMs;
    private final double alertProbability;
    private final Set<ClientSession> clients = new CopyOnWriteArraySet<>();
    private final AtomicInteger clientCounter = new AtomicInteger();
    private ServerSocket serverSocket;
    private ScheduledExecutorService cards;
    private volatile boolean running;

    public SimulationServer(int port, int cardCount, long periodMs, double alertProbability) {
        this.requestedPort = port;
        this.cardCount = cardCount;
        this.periodMs = periodMs;
        this.alertProbability = alertProbability;
    }

    public synchronized void start() throws IOException {
        serverSocket = new ServerSocket(requestedPort);
        running = true;
        final AtomicInteger cardThreads = new AtomicInteger();
        cards = Executors.newScheduledThreadPool(cardCount, r -> {
            Thread t = new Thread(r, "iot-card-" + cardThreads.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        Random seed = new Random();
        for (int i = 0; i < cardCount; i++) {
            long cardId = 0xCAFE0000L + i + 1;
            PacketGenerator generator = new PacketGenerator(new CardReader(cardId), alertProbability, new Random(seed.nextLong()));
            cards.scheduleAtFixedRate(() -> broadcast(generator.generate()), 0, periodMs, TimeUnit.MILLISECONDS);
        }
        Thread acceptor = new Thread(this::acceptLoop, "iot-acceptor");
        acceptor.setDaemon(true);
        acceptor.start();
        System.out.println("[server] listening on port " + getPort() + " with " + cardCount + " cards");
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public int getClientCount() {
        return clients.size();
    }

    public synchronized void stop() {
        running = false;
        if (cards != null) {
            cards.shutdownNow();
        }
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        for (ClientSession c : clients) {
            c.close();
        }
        clients.clear();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                ClientSession session = new ClientSession(socket, clientCounter.incrementAndGet());
                clients.add(session);
                session.start();
                System.out.println("[server] client #" + session.number + " connected from " + socket.getRemoteSocketAddress());
            } catch (SocketException e) {
                return;
            } catch (IOException e) {
                System.err.println("[server] accept failed: " + e.getMessage());
            }
        }
    }

    private void broadcast(Packet p) {
        for (ClientSession c : clients) {
            c.enqueue(p);
        }
    }

    private void remove(ClientSession session) {
        if (clients.remove(session)) {
            System.out.println("[server] client #" + session.number + " disconnected");
        }
    }

    private final class ClientSession {
        private final Socket socket;
        private final int number;
        private final BlockingQueue<Packet> queue = new ArrayBlockingQueue<>(1024);

        ClientSession(Socket socket, int number) {
            this.socket = socket;
            this.number = number;
        }

        void start() {
            Thread t = new Thread(this::writeLoop, "iot-client-" + number);
            t.setDaemon(true);
            t.start();
        }

        void enqueue(Packet p) {
            queue.offer(p);
        }

        private void writeLoop() {
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {
                while (running && !socket.isClosed()) {
                    Packet p = queue.take();
                    PacketCodec.write(out, p);
                    out.flush();
                }
            } catch (IOException e) {
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                close();
                remove(this);
            }
        }

        void close() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        int cards = args.length > 1 ? Integer.parseInt(args[1]) : 3;
        long period = args.length > 2 ? Long.parseLong(args[2]) : 500;
        double alert = args.length > 3 ? Double.parseDouble(args[3]) : 0.05;
        SimulationServer server = new SimulationServer(port, cards, period, alert);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
