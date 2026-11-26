package net.thevpc.gaming.helloworld;

import net.thevpc.gaming.atom.annotations.AtomSprite;
import net.thevpc.gaming.atom.annotations.Inject;
import net.thevpc.gaming.atom.annotations.OnInit;
import net.thevpc.gaming.atom.engine.maintasks.MoveSpriteMainTask;
import net.thevpc.gaming.atom.model.Sprite;

@AtomSprite(
        name = Ball2.NAME,
        kind = Ball2.KIND,
        sceneEngine = "hello",
        x = 7,
        y = 7,
        width = 2,
        height = 2,
        direction = Math.PI / 4 + Math.PI,
        speed = Ball.SPEED * 2,
        life = 3,
        maxLife = 3,
        mainTask = MoveSpriteMainTask.class,
        collisionTask = Ball2CollisionManager.class
)
public class Ball2 {
    public static final String NAME = "Ball2";
    public static final String KIND = "Ball2";

    @Inject
    Sprite sprite;

    @OnInit
    private void init() {
        sprite.setLocation(7, 7);
    }
}
