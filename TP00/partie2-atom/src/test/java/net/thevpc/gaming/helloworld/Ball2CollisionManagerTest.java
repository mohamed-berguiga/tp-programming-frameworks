package net.thevpc.gaming.helloworld;

import net.thevpc.gaming.atom.model.CollisionSides;
import net.thevpc.gaming.atom.model.DefaultSprite;
import org.junit.Test;

import static org.junit.Assert.*;

public class Ball2CollisionManagerTest {
    private static final double EPS = 1e-9;

    @Test
    public void verticalWallFlipsHorizontalComponent() {
        assertEquals(3 * Math.PI / 4, Ball2CollisionManager.calculateBounceDirection(Math.PI / 4, CollisionSides.EAST), EPS);
    }

    @Test
    public void horizontalWallFlipsVerticalComponent() {
        assertEquals(7 * Math.PI / 4, Ball2CollisionManager.calculateBounceDirection(Math.PI / 4, CollisionSides.SOUTH), EPS);
    }

    @Test
    public void cornerReversesDirection() {
        CollisionSides corner = CollisionSides.of(CollisionSides.SIDE_EAST | CollisionSides.SIDE_SOUTH);
        assertEquals(5 * Math.PI / 4, Ball2CollisionManager.calculateBounceDirection(Math.PI / 4, corner), EPS);
    }

    @Test
    public void headOnDetection() {
        DefaultSprite a = new DefaultSprite("Ball");
        DefaultSprite b = new DefaultSprite("Ball2");
        a.setDirection(Math.PI / 4);
        b.setDirection(Math.PI / 4 + Math.PI);
        assertTrue(Ball2CollisionManager.isHeadOn(a, b));
        b.setDirection(Math.PI / 4 + 0.1);
        assertFalse(Ball2CollisionManager.isHeadOn(a, b));
    }
}
