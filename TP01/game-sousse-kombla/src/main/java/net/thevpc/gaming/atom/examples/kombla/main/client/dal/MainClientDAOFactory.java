package net.thevpc.gaming.atom.examples.kombla.main.client.dal;

public final class MainClientDAOFactory {
    private MainClientDAOFactory() {
    }

    public static MainClientDAO create() {
        String transport = System.getProperty("kombla.transport", "socket");
        if ("obj".equalsIgnoreCase(transport)) {
            return new ObjSocketMainClientDAO();
        }
        return new SocketMainClientDAO();
    }
}
