package net.thevpc.gaming.helloworld;

import net.thevpc.gaming.atom.engine.collisiontasks.BorderCollision;
import net.thevpc.gaming.atom.engine.collisiontasks.DefaultSpriteCollisionTask;
import net.thevpc.gaming.atom.engine.collisiontasks.SpriteCollision;
import net.thevpc.gaming.atom.engine.collisiontasks.TileCollision;
import net.thevpc.gaming.atom.model.CollisionSides;
import net.thevpc.gaming.atom.model.Sprite;

public class Ball2CollisionManager extends DefaultSpriteCollisionTask {
    @Override
    public void collideWithBorder(BorderCollision collision) {
        collision.adjustSpritePosition();
        Sprite sprite = collision.getSprite();
        sprite.setDirection(calculateBounceDirection(sprite.getDirection(), collision.getBorderCollisionSides()));
    }

    @Override
    public void collideWithSprite(SpriteCollision collision) {
        collision.adjustSpritePosition();
        Sprite sprite = collision.getSprite();
        Sprite other = collision.getOther();
        if (Missile.KIND.equals(other.getKind())) {
            return;
        }
        if (isHeadOn(sprite, other)) {
            sprite.setDirection(normalize(sprite.getDirection() + Math.PI));
        } else {
            sprite.setDirection(calculateBounceDirection(sprite.getDirection(), collision.getSpriteCollisionSides()));
        }
    }

    @Override
    public void collideWithTile(TileCollision collision) {
        collision.adjustSpritePosition();
        Sprite sprite = collision.getSprite();
        sprite.setDirection(calculateBounceDirection(sprite.getDirection(), collision.getSpriteCollisionSides()));
    }

    static double calculateBounceDirection(double direction, CollisionSides sides) {
        if (sides == null) {
            return normalize(direction + Math.PI);
        }
        double d = direction;
        boolean flipX = sides.isEast() || sides.isWest();
        boolean flipY = sides.isNorth() || sides.isSouth();
        if (!flipX && !flipY) {
            return normalize(d + Math.PI);
        }
        if (flipX) {
            d = Math.PI - d;
        }
        if (flipY) {
            d = -d;
        }
        return normalize(d);
    }

    static boolean isHeadOn(Sprite a, Sprite b) {
        double diff = Math.abs(normalize(a.getDirection() - b.getDirection()) - Math.PI);
        return diff < Math.PI / 4;
    }

    static double normalize(double angle) {
        double twoPi = 2 * Math.PI;
        double a = angle % twoPi;
        return a < 0 ? a + twoPi : a;
    }
}
