package NG.GUIMenu.Menu;

import NG.Camera.Camera;
import NG.Core.Game;
import NG.Core.ModLoader;
import NG.DataStructures.Generic.Color4f;
import NG.Entities.Cube;
import NG.Entities.Entity;
import NG.GUIMenu.Components.*;
import NG.GameState.MapGeneratorMod;
import NG.Mods.Mod;
import NG.Tracks.TrackType;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.List;

/**
 * @author Geert van Ieperen. Created on 28-9-2018.
 */
public class MainMenu extends SFrame {
    // these are upper bounds
    private static final int NUM_TOP_BUTTONS = 10;
    private static final int NUM_BOT_BUTTONS = 10;
    public static final int BUTTON_MIN_WIDTH = 300;
    public static final int BUTTON_MIN_HEIGHT = 50;

    private final Vector2i topButtonPos;
    private final Vector2i bottomButtonPos;
    public static final int NUM_BUTTONS = NUM_TOP_BUTTONS + NUM_BOT_BUTTONS + 1;
    private final Game game;
    private final ModLoader modLoader;
    private final SFrame newGameFrame;

    public MainMenu(Game game, ModLoader modManager, Runnable terminateProgram) {
        super("Main Menu", 400, 500, false);
        this.game = game;
        this.modLoader = modManager;
        topButtonPos = new Vector2i(1, -1);
        bottomButtonPos = new Vector2i(1, NUM_BUTTONS);
        SContainer buttons = new SPanel(3, NUM_BUTTONS);

        newGameFrame = new NewGameFrame(game, modLoader);

        STextComponent newGame = new SButton("Start new game", this::showNewGame, BUTTON_MIN_WIDTH, BUTTON_MIN_HEIGHT);
        buttons.add(newGame, onTop());
        STextComponent justStart = new SButton("Start Testworld", this::testWorld, BUTTON_MIN_WIDTH, BUTTON_MIN_HEIGHT);
        buttons.add(justStart, onTop());
        STextComponent exitGame = new SButton("Exit game", terminateProgram, BUTTON_MIN_WIDTH, BUTTON_MIN_HEIGHT);
        buttons.add(exitGame, onBot());

        Vector2i mid = onTop();
        buttons.add(new SFiller(), new Vector2i(0, mid.y));
        buttons.add(new SFiller(), new Vector2i(1, mid.y));
        buttons.add(new SFiller(), new Vector2i(2, mid.y));

        setMainPanel(buttons);
    }

    private void testWorld() {
        int xSize = 100;
        int ySize = 100;

        // random map
        List<Mod> mods = modLoader.allMods();
        MapGeneratorMod mapGenerator = mods.stream()
                .filter(m -> m instanceof MapGeneratorMod)
                .findAny() // any generator
                .map(m -> (MapGeneratorMod) m)
                .orElseThrow();
        mapGenerator.setXSize(xSize);
        mapGenerator.setYSize(ySize);

        modLoader.initMods(mods);
        game.map().generateNew(mapGenerator);

        // set camera to middle of map
        Vector3f cameraFocus = new Vector3f(xSize / 2f, ySize / 2f, 0);
        Camera cam = game.camera();
        Vector3f cameraEye = new Vector3f(cameraFocus).add(-50, -50, 50);
        cam.set(cameraFocus, cameraEye);

        Vector3f pos = new Vector3f(cameraFocus).add(0, 0, 20);
        Entity cube = new Cube(game, pos);
        game.state().addEntity(cube);

        game.lights().addDirectionalLight(new Vector3f(1, 1.5f, 0.5f), Color4f.WHITE, 0.5f);

        SToolBar toolBar = new SToolBar(game, true);
        for (TrackType trackType : game.objectTypes().getTrackTypes()) {
            toolBar.addButton("Build " + trackType, () -> showBuildTool(trackType));
        }

        toolBar.addButton("Exit", () -> {
            game.gui().clear();
            modLoader.stopGame();
        });
        game.gui().setToolBar(toolBar);

        // start
        modLoader.startGame();
        newGameFrame.setVisible(false);
    }

    private void showBuildTool(TrackType track) {
        game.gui().addFrame(new BuildMenu(game, track));
    }

    private void showNewGame() {
        newGameFrame.setVisible(true);
        game.gui().addFrame(newGameFrame);
    }

    private Vector2i onTop() {
        return topButtonPos.add(0, 1);
    }

    private Vector2i onBot() {
        return bottomButtonPos.sub(0, 1);
    }
}
