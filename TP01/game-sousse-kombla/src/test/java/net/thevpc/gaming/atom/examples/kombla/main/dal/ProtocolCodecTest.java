package net.thevpc.gaming.atom.examples.kombla.main.dal;

import net.thevpc.gaming.atom.examples.kombla.main.shared.dal.ProtocolCodec;
import net.thevpc.gaming.atom.examples.kombla.main.shared.model.DynamicGameModel;
import net.thevpc.gaming.atom.examples.kombla.main.shared.model.StartGameInfo;
import net.thevpc.gaming.atom.model.MovementStyles;
import net.thevpc.gaming.atom.model.Player;
import net.thevpc.gaming.atom.model.Sprite;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ProtocolCodecTest {
    private static DataInputStream reader(ByteArrayOutputStream bytes) {
        return new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
    }

    @Test
    public void modelRoundTrip() throws IOException {
        DynamicGameModel m = Fixtures.model(42L);
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bs);
        ProtocolCodec.writeModel(out, m);
        out.flush();

        DynamicGameModel back = ProtocolCodec.readModel(reader(bs));
        assertEquals(42L, back.getFrame());
        assertEquals(m.getSprites().size(), back.getSprites().size());
        assertEquals(m.getPlayers().size(), back.getPlayers().size());
        Fixtures.assertSameModel(m, back);
    }

    @Test
    public void movementStyleAndPropertiesSurvive() throws IOException {
        DynamicGameModel m = Fixtures.model(1L);
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        ProtocolCodec.writeModel(new DataOutputStream(bs), m);
        DynamicGameModel back = ProtocolCodec.readModel(reader(bs));

        Sprite person = back.getSprites().get(0);
        assertEquals(MovementStyles.MOVING_SLOW, person.getMovementStyle());
        Sprite explosion = back.getSprites().get(2);
        assertEquals("Explosion", explosion.getKind());
        assertEquals("3", explosion.getProperty("explosionTime"));
    }

    @Test
    public void startGameInfoRoundTrip() throws IOException {
        StartGameInfo info = new StartGameInfo(7, Fixtures.maze());
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        ProtocolCodec.writeStartGameInfo(new DataOutputStream(bs), info);
        assertEquals(4 * (3 + 12 * 12), bs.size());
        StartGameInfo back = ProtocolCodec.readStartGameInfo(reader(bs));
        assertEquals(7, back.getPlayerId());
        assertArrayEquals(info.getMaze(), back.getMaze());
    }

    @Test
    public void playerRoundTrip() throws IOException {
        Player p = Fixtures.model(0).getPlayers().get(0);
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        ProtocolCodec.writePlayer(new DataOutputStream(bs), p);
        Player back = ProtocolCodec.readPlayer(reader(bs));
        assertEquals(p.getId(), back.getId());
        assertEquals(p.getName(), back.getName());
        assertEquals(p.getColor(), back.getColor());
    }

    @Test
    public void stringMapRoundTripKeepsOrder() throws IOException {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("b", "2");
        m.put("a", "1");
        m.put("é", "ü");
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        ProtocolCodec.writeStringMap(new DataOutputStream(bs), m);
        assertEquals(m, ProtocolCodec.readStringMap(reader(bs)));
    }

    @Test
    public void streamStaysAlignedAfterSeveralFrames() throws IOException {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bs);
        for (int i = 0; i < 5; i++) {
            ProtocolCodec.writeModel(out, Fixtures.model(i));
        }
        DataInputStream in = reader(bs);
        for (int i = 0; i < 5; i++) {
            assertEquals(i, ProtocolCodec.readModel(in).getFrame());
        }
        assertEquals(0, in.available());
    }

    @Test
    public void dtosAreJavaSerializable() throws Exception {
        DynamicGameModel m = Fixtures.model(9L);
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bs);
        oos.writeObject(m);
        oos.writeObject(new StartGameInfo(3, Fixtures.maze()));
        oos.flush();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bs.toByteArray()));
        DynamicGameModel back = (DynamicGameModel) ois.readObject();
        StartGameInfo info = (StartGameInfo) ois.readObject();
        Fixtures.assertSameModel(m, back);
        assertEquals(3, info.getPlayerId());
        assertArrayEquals(Fixtures.maze(), info.getMaze());
    }
}
