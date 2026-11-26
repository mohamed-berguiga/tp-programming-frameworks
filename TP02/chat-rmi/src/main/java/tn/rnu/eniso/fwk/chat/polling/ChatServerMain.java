package tn.rnu.eniso.fwk.chat.polling;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

public class ChatServerMain {
    private static Registry registry;
    private static ChatServerImpl servant;

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Registry.REGISTRY_PORT;
        registry = LocateRegistry.createRegistry(port);
        servant = new ChatServerImpl("polling-chat");
        registry.rebind("chat", servant);
        System.out.println("bound: " + Arrays.toString(registry.list()) + " on port " + port);
    }
}
