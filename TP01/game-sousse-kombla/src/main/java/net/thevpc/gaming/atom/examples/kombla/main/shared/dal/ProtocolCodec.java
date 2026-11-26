package net.thevpc.gaming.atom.examples.kombla.main.shared.dal;

import net.thevpc.gaming.atom.examples.kombla.main.shared.model.DynamicGameModel;
import net.thevpc.gaming.atom.examples.kombla.main.shared.model.StartGameInfo;
import net.thevpc.gaming.atom.model.DefaultPlayer;
import net.thevpc.gaming.atom.model.DefaultSprite;
import net.thevpc.gaming.atom.model.ModelDimension;
import net.thevpc.gaming.atom.model.Player;
import net.thevpc.gaming.atom.model.Sprite;

import java.awt.Color;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProtocolCodec {
    private ProtocolCodec() {
    }

    public static void writeStartGameInfo(DataOutputStream out, StartGameInfo info) throws IOException {
        out.writeInt(info.getPlayerId());
        int[][] maze = info.getMaze();
        int rows = maze.length;
        int columns = rows == 0 ? 0 : maze[0].length;
        out.writeInt(rows);
        out.writeInt(columns);
        for (int[] row : maze) {
            for (int cell : row) {
                out.writeInt(cell);
            }
        }
    }

    public static StartGameInfo readStartGameInfo(DataInputStream in) throws IOException {
        int playerId = in.readInt();
        int rows = in.readInt();
        int columns = in.readInt();
        int[][] maze = new int[rows][columns];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                maze[r][c] = in.readInt();
            }
        }
        return new StartGameInfo(playerId, maze);
    }

    public static void writePlayer(DataOutputStream out, Player p) throws IOException {
        out.writeInt(p.getId());
        writeNullableString(out, p.getName());
        Color color = p.getColor();
        out.writeBoolean(color != null);
        if (color != null) {
            out.writeInt(color.getRGB());
        }
        writeStringMap(out, toStringMap(p.getProperties()));
    }

    public static Player readPlayer(DataInputStream in) throws IOException {
        DefaultPlayer p = new DefaultPlayer();
        p.setId(in.readInt());
        p.setName(readNullableString(in));
        if (in.readBoolean()) {
            p.setColor(new Color(in.readInt(), true));
        }
        for (Map.Entry<String, String> e : readStringMap(in).entrySet()) {
            p.setProperty(e.getKey(), e.getValue());
        }
        return p;
    }

    public static void writeSprite(DataOutputStream out, Sprite s) throws IOException {
        out.writeInt(s.getId());
        out.writeUTF(s.getKind());
        writeNullableString(out, s.getName());
        out.writeDouble(s.getX());
        out.writeDouble(s.getY());
        out.writeDouble(s.getWidth());
        out.writeDouble(s.getHeight());
        out.writeDouble(s.getDirection());
        out.writeDouble(s.getSpeed());
        out.writeInt(s.getLife());
        out.writeInt(s.getMaxLife());
        out.writeInt(s.getPlayerId());
        out.writeInt(s.getLayer());
        out.writeBoolean(s.isCrossable());
        out.writeInt(s.getMovementStyle());
        out.writeInt(s.getStyle());
        writeStringMap(out, toStringMap(s.getProperties()));
    }

    public static Sprite readSprite(DataInputStream in) throws IOException {
        int id = in.readInt();
        String kind = in.readUTF();
        String name = readNullableString(in);
        double x = in.readDouble();
        double y = in.readDouble();
        double width = in.readDouble();
        double height = in.readDouble();
        double direction = in.readDouble();
        double speed = in.readDouble();
        int life = in.readInt();
        int maxLife = in.readInt();
        int playerId = in.readInt();
        int layer = in.readInt();
        boolean crossable = in.readBoolean();
        int movementStyle = in.readInt();
        int style = in.readInt();
        Map<String, String> props = readStringMap(in);

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
        for (Map.Entry<String, String> e : props.entrySet()) {
            s.setProperty(e.getKey(), e.getValue());
        }
        return s;
    }

    public static void writeModel(DataOutputStream out, DynamicGameModel m) throws IOException {
        out.writeLong(m.getFrame());
        List<Player> players = m.getPlayers() == null ? new ArrayList<Player>() : m.getPlayers();
        out.writeInt(players.size());
        for (Player p : players) {
            writePlayer(out, p);
        }
        List<Sprite> sprites = m.getSprites() == null ? new ArrayList<Sprite>() : m.getSprites();
        out.writeInt(sprites.size());
        for (Sprite s : sprites) {
            writeSprite(out, s);
        }
    }

    public static DynamicGameModel readModel(DataInputStream in) throws IOException {
        long frame = in.readLong();
        int playerCount = in.readInt();
        List<Player> players = new ArrayList<>(playerCount);
        for (int i = 0; i < playerCount; i++) {
            players.add(readPlayer(in));
        }
        int spriteCount = in.readInt();
        List<Sprite> sprites = new ArrayList<>(spriteCount);
        for (int i = 0; i < spriteCount; i++) {
            sprites.add(readSprite(in));
        }
        return new DynamicGameModel(frame, sprites, players);
    }

    public static void writeStringMap(DataOutputStream out, Map<String, String> map) throws IOException {
        out.writeInt(map.size());
        for (Map.Entry<String, String> e : map.entrySet()) {
            out.writeUTF(e.getKey());
            out.writeUTF(e.getValue());
        }
    }

    public static Map<String, String> readStringMap(DataInputStream in) throws IOException {
        int n = in.readInt();
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            m.put(in.readUTF(), in.readUTF());
        }
        return m;
    }

    public static void writeNullableString(DataOutputStream out, String s) throws IOException {
        out.writeBoolean(s != null);
        if (s != null) {
            out.writeUTF(s);
        }
    }

    public static String readNullableString(DataInputStream in) throws IOException {
        return in.readBoolean() ? in.readUTF() : null;
    }

    static Map<String, String> toStringMap(Map<String, Object> props) {
        Map<String, String> m = new LinkedHashMap<>();
        if (props != null) {
            for (Map.Entry<String, Object> e : props.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    m.put(e.getKey(), String.valueOf(e.getValue()));
                }
            }
        }
        return m;
    }
}
