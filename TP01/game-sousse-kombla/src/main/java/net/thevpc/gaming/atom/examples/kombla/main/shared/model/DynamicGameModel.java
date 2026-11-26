package net.thevpc.gaming.atom.examples.kombla.main.shared.model;

import net.thevpc.gaming.atom.model.DefaultSprite;
import net.thevpc.gaming.atom.model.ModelDimension;
import net.thevpc.gaming.atom.model.Player;
import net.thevpc.gaming.atom.model.Sprite;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vpc on 10/7/16.
 */
public class DynamicGameModel implements Serializable {
    private static final long serialVersionUID = 1L;
    long frame; transient List<Sprite> sprites; java.util.List<Player> players;

    public DynamicGameModel() {

    }
    public DynamicGameModel(long frame, List<Sprite> sprites, List<Player> players) {
        this.frame = frame;
        this.sprites = sprites;
        this.players = players;
    }

    public DynamicGameModel setFrame(long frame) {
        this.frame = frame;
        return this;
    }

    public DynamicGameModel setSprites(List<Sprite> sprites) {
        this.sprites = sprites;
        return this;
    }

    public DynamicGameModel setPlayers(List<Player> players) {
        this.players = players;
        return this;
    }

    public long getFrame() {
        return frame;
    }

    public List<Sprite> getSprites() {
        return sprites;
    }

    public List<Player> getPlayers() {
        return players;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        List<SpriteState> states = new ArrayList<>();
        if (sprites != null) {
            for (Sprite s : sprites) {
                states.add(new SpriteState(s));
            }
        }
        out.writeObject(states);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        List<SpriteState> states = (List<SpriteState>) in.readObject();
        sprites = new ArrayList<>(states.size());
        for (SpriteState st : states) {
            sprites.add(st.toSprite());
        }
    }

    /** Serializable image of the Sprite fields the client needs. */
    private static final class SpriteState implements Serializable {
        private static final long serialVersionUID = 1L;
        int id;
        String kind;
        String name;
        double x;
        double y;
        double width;
        double height;
        double direction;
        double speed;
        int life;
        int maxLife;
        int playerId;
        int layer;
        boolean crossable;
        int movementStyle;
        int style;
        HashMap<String, Object> properties = new HashMap<>();

        SpriteState(Sprite s) {
            id = s.getId();
            kind = s.getKind();
            name = s.getName();
            x = s.getX();
            y = s.getY();
            width = s.getWidth();
            height = s.getHeight();
            direction = s.getDirection();
            speed = s.getSpeed();
            life = s.getLife();
            maxLife = s.getMaxLife();
            playerId = s.getPlayerId();
            layer = s.getLayer();
            crossable = s.isCrossable();
            movementStyle = s.getMovementStyle();
            style = s.getStyle();
            for (Map.Entry<String, Object> e : s.getProperties().entrySet()) {
                Object v = e.getValue();
                properties.put(e.getKey(), v instanceof Serializable ? v : String.valueOf(v));
            }
        }

        Sprite toSprite() {
            DefaultSprite s = new DefaultSprite(kind);
            s.setId(id);
            if (name != null) {
                s.setName(name);
            }
            s.setSize(new ModelDimension(width, height));
            s.setLocation(x, y);
            s.setPreviousLocation(s.getLocation());
            s.setDirection(direction);
            s.setSpeed(speed);
            s.setMaxLife(maxLife);
            s.setLife(life);
            s.setPlayerId(playerId);
            s.setLayer(layer);
            s.setCrossable(crossable);
            s.setMovementStyle(movementStyle);
            s.setStyle(style);
            for (Map.Entry<String, Object> e : properties.entrySet()) {
                s.setProperty(e.getKey(), e.getValue());
            }
            return s;
        }
    }
}
