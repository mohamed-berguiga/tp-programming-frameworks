package net.thevpc.gaming.atom.examples.kombla.main.server.dal;

public final class MainServerDAOFactory {
    private MainServerDAOFactory() {
    }

    public static MainServerDAO create() {
        String transport = System.getProperty("kombla.transport", "socket");
        if ("obj".equalsIgnoreCase(transport)) {
            return new ObjSocketMainServerDAO();
        }
        return new SocketMainServerDAO();
    }
}
