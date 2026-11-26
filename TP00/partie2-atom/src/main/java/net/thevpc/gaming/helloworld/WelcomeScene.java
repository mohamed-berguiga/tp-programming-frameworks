package net.thevpc.gaming.helloworld;

import net.thevpc.gaming.atom.annotations.AtomScene;
import net.thevpc.gaming.atom.annotations.AtomSceneEngine;
import net.thevpc.gaming.atom.annotations.Inject;
import net.thevpc.gaming.atom.annotations.OnSceneStarted;
import net.thevpc.gaming.atom.model.Orientation;
import net.thevpc.gaming.atom.model.Point;
import net.thevpc.gaming.atom.presentation.DefaultSceneController;
import net.thevpc.gaming.atom.presentation.KeyCode;
import net.thevpc.gaming.atom.presentation.Scene;
import net.thevpc.gaming.atom.presentation.SceneKeyEvent;
import net.thevpc.gaming.atom.presentation.components.SLabel;
import net.thevpc.gaming.atom.presentation.layers.Layers;

import java.awt.Color;

@AtomScene(
        id = WelcomeScene.ID,
        title = "Hello World - Welcome",
        tileWidth = 80,
        tileHeight = 80
)
@AtomSceneEngine(
        id = WelcomeScene.ID,
        columns = 10,
        rows = 10,
        fps = 25,
        welcome = true
)
public class WelcomeScene {
    public static final String ID = "welcome";

    @Inject
    private Scene scene;

    @OnSceneStarted
    private void init() {
        scene.addLayer(Layers.fillScreenGradient(new Color(10, 20, 60), new Color(60, 10, 80), Orientation.SOUTH));
        scene.addComponent(Styles.style(new SLabel("title", "ATOM - HELLO BALLS"), 40, Color.YELLOW)
                .setLocation(Point.ratio(0.5f, 0.3f)));
        scene.addComponent(Styles.style(new SLabel("help1", "Arrows: move Ball1   |   E/S/D/F or mouse click: move Ball2"), 16, Color.WHITE)
                .setLocation(Point.ratio(0.5f, 0.5f)));
        scene.addComponent(Styles.style(new SLabel("help2", "SPACE in game: fire a missile   |   CTRL-D: debug   CTRL-I: isometric"), 16, Color.WHITE)
                .setLocation(Point.ratio(0.5f, 0.58f)));
        scene.addComponent(Styles.style(new SLabel("start", "Press SPACE to start"), 24, Color.CYAN)
                .setLocation(Point.ratio(0.5f, 0.75f)));
        scene.addController(new DefaultSceneController() {
            @Override
            public void keyPressed(SceneKeyEvent e) {
                if (e.getKeyCode() == KeyCode.SPACE) {
                    e.setConsumed(true);
                    e.getGameEngine().setActiveSceneEngine(HelloWorldScene.ID);
                }
            }
        });
    }
}
