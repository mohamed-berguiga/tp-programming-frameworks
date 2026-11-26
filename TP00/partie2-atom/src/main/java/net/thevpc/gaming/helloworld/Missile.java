package net.thevpc.gaming.helloworld;

import net.thevpc.gaming.atom.engine.SceneEngine;
import net.thevpc.gaming.atom.engine.collisiontasks.BorderCollision;
import net.thevpc.gaming.atom.engine.collisiontasks.SpriteCollision;
import net.thevpc.gaming.atom.engine.collisiontasks.SpriteCollisionTask;
import net.thevpc.gaming.atom.engine.collisiontasks.TileCollision;
import net.thevpc.gaming.atom.engine.maintasks.MoveSpriteMainTask;
import net.thevpc.gaming.atom.model.ModelDimension;
import net.thevpc.gaming.atom.model.Sprite;

public final class Missile {
    public static final String KIND = "Missile";
    public static final double SIZE = 0.4;
    static final String SHOOTER = "shooter";

    private Missile() {
    }

    public static Sprite fire(SceneEngine engine, Sprite shooter) {
        double dir = shooter.getDirection();
        double cx = shooter.getX() + shooter.getWidth() / 2;
        double cy = shooter.getY() + shooter.getHeight() / 2;
        double distance = Math.max(shooter.getWidth(), shooter.getHeight()) / 2 + SIZE;
        double x = cx + Math.cos(dir) * distance - SIZE / 2;
        double y = cy + Math.sin(dir) * distance - SIZE / 2;

        Sprite missile = engine.createSprite(KIND);
        missile.setName(KIND);
        missile.setSize(new ModelDimension(SIZE, SIZE));
        missile.setLocation(x, y);
        missile.setPreviousLocation(missile.getLocation());
        missile.setDirection(dir);
        missile.setSpeed(shooter.getSpeed() * 2);
        missile.setProperty(SHOOTER, shooter.getId());
        engine.addSprite(missile);
        engine.setSpriteMainTask(missile, new MoveSpriteMainTask());
        engine.setSpriteCollisionTask(missile, new SelfDestructCollisionTask());
        return missile;
    }

    static class SelfDestructCollisionTask implements SpriteCollisionTask {
        @Override
        public void collideWithBorder(BorderCollision collision) {
            destroy(collision.getSprite());
        }

        @Override
        public void collideWithTile(TileCollision collision) {
            destroy(collision.getSprite());
        }

        @Override
        public void collideWithSprite(SpriteCollision collision) {
            Sprite missile = collision.getSprite();
            Sprite other = collision.getOther();
            if (Integer.valueOf(other.getId()).equals(missile.getProperty(SHOOTER))) {
                return;
            }
            if (!KIND.equals(other.getKind()) && other.getLife() > 0) {
                other.addLife(-1);
            }
            destroy(missile);
        }

        private void destroy(Sprite missile) {
            missile.die();
        }
    }
}
