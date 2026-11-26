package net.thevpc.gaming.atom.examples.kombla.main.client.dal;

import net.thevpc.gaming.atom.examples.kombla.main.shared.model.DynamicGameModel;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import static org.junit.Assert.assertTrue;

public class SnapshotCostTest {
    @Test
    public void measure() throws Exception {
        DynamicGameModel model = Fixtures.model(1234L);
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        for (int i = 0; i < 200; i++) {
            bs.reset();
            new ObjectOutputStream(bs).writeObject(model);
        }
        long t0 = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            bs.reset();
            new ObjectOutputStream(bs).writeObject(model);
        }
        long us = (System.nanoTime() - t0) / 1000;
        System.out.println("[snapshot-cost] bytes=" + bs.size() + " us/call=" + (us / 1000.0)
                + " -> " + (bs.size() * 25) + " B/s per player at 25 Hz");
        assertTrue(bs.size() > 0);
    }
}
