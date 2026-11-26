package tn.rnu.eniso.fwk.chat.push;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

public class PushChatServerMain {
    private static Registry registry;
    private static ChatImpl servant;

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Registry.REGISTRY_PORT;
        registry = LocateRegistry.createRegistry(port);
        servant = new ChatImpl();
        registry.rebind("chat", servant);
        System.out.println("bound: " + Arrays.toString(registry.list()) + " on port " + port);
    }
}
