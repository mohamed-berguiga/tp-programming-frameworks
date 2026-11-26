package net.thevpc.gaming.helloworld;

import net.thevpc.gaming.atom.annotations.AtomSprite;
import net.thevpc.gaming.atom.annotations.Inject;
import net.thevpc.gaming.atom.annotations.OnInit;
import net.thevpc.gaming.atom.engine.SceneEngine;
import net.thevpc.gaming.atom.engine.maintasks.MoveSpriteMainTask;
import net.thevpc.gaming.atom.model.Sprite;

@AtomSprite(
        name = Ball.NAME,
        kind = "Ball",
        sceneEngine = "hello",
        x = 2,
        y = 2,
        direction = Math.PI / 4,
        speed = 0.2,
        life = 3,
        maxLife = 3,
        mainTask = MoveSpriteMainTask.class,
        collisionTask = Ball2CollisionManager.class
)
public class Ball {
    public static final String NAME = "Ball1";
    public static final double SPEED = 0.2;

    @Inject
    Sprite sprite;
    @Inject
    SceneEngine sceneEngine;

    @OnInit
    private void init() {
        sprite.setLocation(2, 2);
    }
}
