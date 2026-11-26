package tn.rnu.eniso.fwk.rmi.experiment;

import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Experiment {
    public static void main(String[] args) throws Exception {
        int port;
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        Registry registry = LocateRegistry.createRegistry(port);
        ProbeImpl impl = new ProbeImpl();
        registry.rebind("probe", impl);
        Probe api = (Probe) LocateRegistry.getRegistry("localhost", port).lookup("probe");
        System.out.println("stub class: " + api.getClass().getName());
        ExperimentClient.run(api);
        UnicastRemoteObject.unexportObject(impl, true);
        UnicastRemoteObject.unexportObject(registry, true);
    }
}
