package net.thevpc.gaming.atom.examples.kombla.main.client.dal;

import net.thevpc.gaming.atom.examples.kombla.main.shared.model.DynamicGameModel;
import net.thevpc.gaming.atom.model.DefaultPlayer;
import net.thevpc.gaming.atom.model.DefaultSprite;
import net.thevpc.gaming.atom.model.ModelDimension;
import net.thevpc.gaming.atom.model.MovementStyles;
import net.thevpc.gaming.atom.model.Orientation;
import net.thevpc.gaming.atom.model.Player;
import net.thevpc.gaming.atom.model.Sprite;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

final class Fixtures {
    private Fixtures() {
    }

    static int[][] maze() {
        int[][] m = new int[12][12];
        for (int r = 0; r < 12; r++) {
            for (int c = 0; c < 12; c++) {
                m[r][c] = (r == 0 || c == 0 || r == 11 || c == 11) ? 1 + (r + c) % 4 : 0;
            }
        }
        return m;
    }

    static DynamicGameModel model(long frame) {
        List<Player> players = new ArrayList<>();
        DefaultPlayer p1 = new DefaultPlayer("Boss");
        p1.setId(1);
        p1.setColor(Color.RED);
        players.add(p1);
        DefaultPlayer p2 = new DefaultPlayer("ada");
        p2.setId(2);
        p2.setProperty("score", 12);
        players.add(p2);

        List<Sprite> sprites = new ArrayList<>();
        DefaultSprite person = new DefaultSprite("Person");
        person.setId(10);
        person.setSize(new ModelDimension(0.5, 0.5));
        person.setLocation(3.25, 4.5);
        person.setDirection(Orientation.EAST);
        person.setSpeed(0.05);
        person.setLife(30);
        person.setPlayerId(2);
        person.setMovementStyle(MovementStyles.MOVING_SLOW);
        sprites.add(person);

        DefaultSprite bomb = new DefaultSprite("Bomb");
        bomb.setId(11);
        bomb.setSize(new ModelDimension(0.5, 0.5));
        bomb.setLocation(3, 4);
        bomb.setSpeed(0);
        bomb.setCrossable(true);
        bomb.setPlayerId(2);
        sprites.add(bomb);

        DefaultSprite explosion = new DefaultSprite("Explosion");
        explosion.setId(12);
        explosion.setSize(new ModelDimension(1, 1));
        explosion.setLocation(5, 6);
        explosion.setPlayerId(1);
        explosion.setProperty("explosionTime", 3);
        sprites.add(explosion);
        return new DynamicGameModel(frame, sprites, players);
    }

    static void assertSameModel(DynamicGameModel expected, DynamicGameModel actual) {
        assertEquals(expected.getFrame(), actual.getFrame());
        assertEquals(expected.getPlayers().size(), actual.getPlayers().size());
        for (int i = 0; i < expected.getPlayers().size(); i++) {
            assertEquals(expected.getPlayers().get(i).getId(), actual.getPlayers().get(i).getId());
            assertEquals(expected.getPlayers().get(i).getName(), actual.getPlayers().get(i).getName());
        }
        assertEquals(expected.getSprites().size(), actual.getSprites().size());
        for (int i = 0; i < expected.getSprites().size(); i++) {
            Sprite e = expected.getSprites().get(i);
            Sprite a = actual.getSprites().get(i);
            assertEquals(e.getId(), a.getId());
            assertEquals(e.getKind(), a.getKind());
            assertEquals(e.getLocation(), a.getLocation());
            assertEquals(e.getWidth(), a.getWidth(), 0);
            assertEquals(e.getHeight(), a.getHeight(), 0);
            assertEquals(e.getDirection(), a.getDirection(), 0);
            assertEquals(e.getSpeed(), a.getSpeed(), 0);
            assertEquals(e.getLife(), a.getLife());
            assertEquals(e.getPlayerId(), a.getPlayerId());
            assertEquals(e.getLayer(), a.getLayer());
            assertEquals(e.isCrossable(), a.isCrossable());
            assertEquals(e.getMovementStyle(), a.getMovementStyle());
            assertEquals(String.valueOf(e.getProperties().keySet()), String.valueOf(a.getProperties().keySet()));
        }
    }
}
