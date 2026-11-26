package net.thevpc.gaming.helloworld;

import net.thevpc.gaming.atom.annotations.AtomScene;
import net.thevpc.gaming.atom.annotations.AtomSceneEngine;
import net.thevpc.gaming.atom.annotations.Inject;
import net.thevpc.gaming.atom.annotations.OnNextFrame;
import net.thevpc.gaming.atom.annotations.OnSceneStarted;
import net.thevpc.gaming.atom.debug.AdjustViewController;
import net.thevpc.gaming.atom.engine.SceneEngine;
import net.thevpc.gaming.atom.engine.SpriteFilter;
import net.thevpc.gaming.atom.model.Point;
import net.thevpc.gaming.atom.model.Sprite;
import net.thevpc.gaming.atom.presentation.DefaultSceneController;
import net.thevpc.gaming.atom.presentation.DefaultSpriteView;
import net.thevpc.gaming.atom.presentation.ImageSpriteView;
import net.thevpc.gaming.atom.presentation.KeyCode;
import net.thevpc.gaming.atom.presentation.Scene;
import net.thevpc.gaming.atom.presentation.SceneKeyEvent;
import net.thevpc.gaming.atom.presentation.SpriteController;
import net.thevpc.gaming.atom.presentation.SpriteDrawingContext;
import net.thevpc.gaming.atom.presentation.components.SLabel;
import net.thevpc.gaming.atom.presentation.layers.FillBoardColorLayer;
import net.thevpc.gaming.atom.presentation.layers.FillScreenColorLayer;
import net.thevpc.gaming.atom.presentation.layers.Layers;

import java.awt.Color;
import java.awt.Graphics2D;

@AtomScene(
        id = HelloWorldScene.ID,
        title = "Hello World",
        tileWidth = 80,
        tileHeight = 80
)
@AtomSceneEngine(
        id = HelloWorldScene.ID,
        columns = 10,
        rows = 10,
        fps = 25
)
public class HelloWorldScene {
    public static final String ID = "hello";
    static final String BACKGROUND_MODE = System.getProperty("hello.background", "image");
    static final String BALL2_VIEW = System.getProperty("hello.ball2", "animated");

    @Inject
    private Scene scene;
    @Inject
    private SceneEngine sceneEngine;

    private final SLabel livesLabel = new SLabel("lives", "Lives: 3");

    @OnSceneStarted
    private void init() {
        installBackground();
        scene.addLayer(Layers.debug());

        scene.addController(new SpriteController(SpriteFilter.byName(Ball.NAME)).setArrowKeysLayout());
        scene.addController(new SpriteController2(SpriteFilter.byName(Ball2.NAME)));
        scene.addController(new AdjustViewController());
        scene.addController(new DefaultSceneController() {
            @Override
            public void keyPressed(SceneKeyEvent e) {
                if (e.getKeyCode() == KeyCode.SPACE) {
                    fireMissile();
                    e.setConsumed(true);
                }
            }
        });

        scene.addComponent(Styles.style(livesLabel, 20, Color.YELLOW).setLocation(Point.ratio(0.15f, 0.04f)));
        scene.addComponent(
                Styles.style(new SLabel("help", "CTRL-D: debug, CTRL-I: isometric, arrows: Ball1, ESDF/mouse: Ball2, SPACE: missile"), 12, Color.WHITE)
                        .setLocation(Point.ratio(0.5f, 0.96f))
        );

        scene.setSpriteView(SpriteFilter.byKind("Ball"), new ImageSpriteView("/ball.png", 8, 4));
        if ("magenta".equals(BALL2_VIEW)) {
            useMagentaSquareView();
        } else {
            useAnimatedImageView();
        }
        scene.setSpriteView(SpriteFilter.byKind(Missile.KIND), new DefaultSpriteView() {
            @Override
            public void draw(SpriteDrawingContext context) {
                Graphics2D g = context.getGraphics();
                g.setColor(Color.ORANGE);
                g.fill(context.getSpriteShape());
            }
        });
    }

    @OnNextFrame
    public void onEveryTick() {
        Sprite ball = sceneEngine.findSpriteByName(Ball.NAME);
        if (ball != null) {
            livesLabel.setText("Lives: " + ball.getLife());
        } else {
            livesLabel.setText("Lives: 0 (ball destroyed)");
        }
    }

    private void installBackground() {
        switch (BACKGROUND_MODE) {
            case "board":
                scene.addLayer(new FillBoardColorLayer(new Color(20, 90, 40)));
                break;
            case "screen":
                scene.addLayer(new FillScreenColorLayer(new Color(30, 30, 110)));
                break;
            default:
                scene.addLayer(Layers.fillScreenImage("/background.png"));
                break;
        }
    }

    void useMagentaSquareView() {
        scene.setSpriteView(SpriteFilter.byName(Ball2.NAME), new DefaultSpriteView() {
            @Override
            public void draw(SpriteDrawingContext context) {
                Graphics2D g = context.getGraphics();
                g.setColor(Color.MAGENTA);
                g.fill(context.getSpriteShape());
            }
        });
    }

    void useAnimatedImageView() {
        ImageSpriteView view = new ImageSpriteView(
                "/ball2_0.png", "/ball2_1.png", "/ball2_2.png", "/ball2_3.png",
                "/ball2_4.png", "/ball2_5.png", "/ball2_6.png", "/ball2_7.png");
        scene.setSpriteView(SpriteFilter.byName(Ball2.NAME), view);
    }

    private void fireMissile() {
        Sprite ball = sceneEngine.findSpriteByName(Ball.NAME);
        if (ball != null) {
            sceneEngine.invokeLater(() -> Missile.fire(sceneEngine, ball));
        }
    }
}
