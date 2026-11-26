package net.thevpc.gaming.helloworld;

import net.thevpc.gaming.atom.engine.SceneEngine;
import net.thevpc.gaming.atom.engine.SpriteFilter;
import net.thevpc.gaming.atom.engine.maintasks.MoveSpriteMainTask;
import net.thevpc.gaming.atom.engine.maintasks.MoveToPointSpriteMainTask;
import net.thevpc.gaming.atom.model.ModelPoint;
import net.thevpc.gaming.atom.model.Orientation;
import net.thevpc.gaming.atom.model.Sprite;
import net.thevpc.gaming.atom.presentation.DefaultSceneController;
import net.thevpc.gaming.atom.presentation.KeyCode;
import net.thevpc.gaming.atom.presentation.KeyCodeSet;
import net.thevpc.gaming.atom.presentation.SceneKeyEvent;
import net.thevpc.gaming.atom.presentation.SceneMouseEvent;

public class SpriteController2 extends DefaultSceneController {
    private final SpriteFilter sprites;
    private final KeyCodeSet up = KeyCodeSet.of(KeyCode.E);
    private final KeyCodeSet down = KeyCodeSet.of(KeyCode.D);
    private final KeyCodeSet left = KeyCodeSet.of(KeyCode.S);
    private final KeyCodeSet right = KeyCodeSet.of(KeyCode.F);

    public SpriteController2(SpriteFilter sprites) {
        if (sprites == null) {
            throw new NullPointerException("null sprite condition");
        }
        this.sprites = sprites;
    }

    @Override
    public void keyChanged(SceneKeyEvent e) {
        for (Sprite sp : e.getScene().getSceneEngine().findSprites(sprites)) {
            keyChanged(sp, e);
        }
    }

    private void keyChanged(Sprite sprite, SceneKeyEvent e) {
        KeyCodeSet keys = e.getKeyCodes();
        Orientation or = null;
        if (keys.equals(up)) {
            or = Orientation.NORTH;
        } else if (keys.equals(down)) {
            or = Orientation.SOUTH;
        } else if (keys.equals(left)) {
            or = Orientation.WEST;
        } else if (keys.equals(right)) {
            or = Orientation.EAST;
        } else if (keys.equals(up.plus(left))) {
            or = Orientation.NORTH_WEST;
        } else if (keys.equals(up.plus(right))) {
            or = Orientation.NORTH_EAST;
        } else if (keys.equals(down.plus(left))) {
            or = Orientation.SOUTH_WEST;
        } else if (keys.equals(down.plus(right))) {
            or = Orientation.SOUTH_EAST;
        }
        if (or != null) {
            SceneEngine engine = e.getScene().getSceneEngine();
            if (!(engine.getSpriteMainTask(sprite) instanceof MoveSpriteMainTask)) {
                engine.setSpriteMainTask(sprite, new MoveSpriteMainTask());
            }
            sprite.setDirection(or);
            e.setConsumed(true);
        }
    }

    @Override
    public void mouseClicked(SceneMouseEvent e) {
        ModelPoint target = e.getPoint();
        if (target == null) {
            return;
        }
        SceneEngine engine = e.getScene().getSceneEngine();
        for (Sprite sp : engine.findSprites(sprites)) {
            engine.setSpriteMainTask(sp, new MoveToPointSpriteMainTask(target, sp.getSpeed(), true));
        }
    }
}
