package tn.rnu.eniso.fwk.rmi.experiment;

import org.junit.Test;

import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import static org.junit.Assert.assertArrayEquals;

public class ExperimentTest {
    @Test(timeout = 10000)
    public void valueSemanticsOverRmi() throws Exception {
        int port;
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        Registry registry = LocateRegistry.createRegistry(port);
        ProbeImpl impl = new ProbeImpl();
        registry.rebind("probe", impl);
        try {
            Probe api = (Probe) LocateRegistry.getRegistry("localhost", port).lookup("probe");
            assertArrayEquals(new String[]{"99 1", "false", "false"}, ExperimentClient.run(api));
        } finally {
            UnicastRemoteObject.unexportObject(impl, true);
            UnicastRemoteObject.unexportObject(registry, true);
        }
    }
}
