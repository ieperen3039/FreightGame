package NG.GUIMenu.Menu;

import NG.Camera.Camera;
import NG.Core.Game;
import NG.Core.ModLoader;
import NG.Entities.Cube;
import NG.Entities.Entity;
import NG.GUIMenu.Components.*;
import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.SComponentProperties;
import NG.GameMap.MapGeneratorMod;
import NG.GameMap.SimpleMapGenerator;
import NG.Mods.Mod;
import NG.Network.NetworkNode;
import NG.Network.RailNode;
import NG.Settings.Settings;
import NG.Tools.Logger;
import NG.Tracks.TrackPiece;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author Geert van Ieperen. Created on 28-9-2018.
 */
public class MainMenu extends SFrame {
    // these are upper bounds
    private static final int NUM_TOP_BUTTONS = 10;
    private static final int NUM_BOT_BUTTONS = 10;
    public static final int NUM_BUTTONS = NUM_TOP_BUTTONS + NUM_BOT_BUTTONS + 1;
    public static final SComponentProperties BUTTON_PROPERTIES = new SComponentProperties(
            300, 100, false, false, NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER
    );
    public static final SComponentProperties TEXT_PROPERTIES = new SComponentProperties(
            0, 100, false, false, NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.LEFT
    );

    private final Vector2i topButtonPos;
    private final Vector2i bottomButtonPos;
    private final Game game;
    private final ModLoader modLoader;
    private final SFrame newGameFrame;

    public MainMenu(Game game, ModLoader modManager, Runnable terminateProgram) {
        super("Main Menu", 400, 500, false);
        this.game = game;
        this.modLoader = modManager;
        topButtonPos = new Vector2i(1, -1);
        bottomButtonPos = new Vector2i(1, NUM_BUTTONS);

        newGameFrame = new NewGameFrame(game, modLoader);

        STextComponent newGame = new SButton("Start new game", this::showNewGame, BUTTON_PROPERTIES);
        STextComponent justStart = new SButton("Start Testworld", this::testWorld, BUTTON_PROPERTIES);
        STextComponent exitGame = new SButton("Exit game", terminateProgram, BUTTON_PROPERTIES);

        setMainPanel(SContainer.row(
                new SFiller(),
                SContainer.column(
                        newGame,
                        justStart,
                        new SFiller().setGrowthPolicy(false, true),
                        exitGame
                ),
                new SFiller()
        ));
    }

    private void testWorld() {
        Settings settings = game.settings();
        int xSize = 100;
        int ySize = 100;

        // random map
        List<Mod> mods = modLoader.allMods();
        MapGeneratorMod mapGenerator = new SimpleMapGenerator(0);
        mapGenerator.setSize(xSize, ySize);

        modLoader.initMods(mods);
        game.map().generateNew(game, mapGenerator);

        // set camera to middle of map
        Vector2f size = game.map().getSize();
        Vector3f cameraFocus = new Vector3f(size.x / 2, size.y / 2, 0);
        Camera cam = game.camera();
        Vector3f cameraEye = new Vector3f(cameraFocus).add(-20, -20, 20);
        cam.set(cameraFocus, cameraEye);

        Vector3f pos = new Vector3f(cameraFocus).add(0, 0, 20);
        Entity cube = new Cube(game, pos);
        game.state().addEntity(cube);

        game.lights().addDirectionalLight(
                new Vector3f(1, 1.5f, 0.5f), settings.SUNLIGHT_COLOR, settings.SUNLIGHT_INTENSITY
        );

        SToolBar toolBar = new SToolBar(game, true);
        toolBar.addButton(
                "Build Track",
                () -> game.gui().addFrame(new BuildMenu(game))
        );

        toolBar.addButton( // TODO remove this debug option
                "New Train",
                () -> game.inputHandling().setMouseTool(new EntityActionTool(
                        game, e -> e instanceof TrackPiece,
                        entity -> game.gui().addFrame(new TrainConstructionMenu(game, (TrackPiece) entity))
                ))
        );

        toolBar.addButton("Dump Network", // find any networknode, and print getNetworkAsString
                () -> game.state().entities().stream()
                        .filter(e -> e instanceof TrackPiece)
                        .map(e -> (TrackPiece) e)
                        .filter(e -> !e.isDespawnedAt(game.timer().getGameTime()))
                        .map(TrackPiece::getStartNode)
                        .map(RailNode::getNetworkNode)
                        .filter(NetworkNode::isNetworkCritical)
                        .findAny()
                        .ifPresentOrElse(
                                n -> Logger.WARN.print(NetworkNode.getNetworkAsString(n)),
                                () -> Logger.WARN.print("No network present")
                        )
        );

        toolBar.addButton("Check All", // checks the NetworkNodes of all track pieces
                () -> game.state().entities().stream()
                        .filter(e -> e instanceof TrackPiece)
                        .map(e -> (TrackPiece) e)
                        .filter(e -> !e.isDespawnedAt(game.timer().getGameTime()))
                        .flatMap(t -> Stream.of(t.getStartNode(), t.getEndNode()))
                        .distinct()
                        .map(RailNode::getNetworkNode)
                        .forEach(NetworkNode::check)
        );

        toolBar.addButton("Exit", () -> {
            game.gui().clear();
            modLoader.stopGame();
        });
        game.gui().setToolBar(toolBar);

        // start
        modLoader.startGame();
        newGameFrame.setVisible(false);
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
