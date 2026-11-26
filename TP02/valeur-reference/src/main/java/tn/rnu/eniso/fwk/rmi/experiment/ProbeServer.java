package tn.rnu.eniso.fwk.rmi.experiment;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

public class ProbeServer {
    private static Registry registry;
    private static ProbeImpl probe;

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Registry.REGISTRY_PORT;
        registry = LocateRegistry.createRegistry(port);
        probe = new ProbeImpl();
        registry.rebind("probe", probe);
        System.out.println("bound: " + Arrays.toString(registry.list()) + " on port " + port);
    }
}
