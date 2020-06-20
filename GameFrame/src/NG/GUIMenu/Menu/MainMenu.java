package NG.GUIMenu.Menu;

import NG.Content.Scenario;
import NG.Content.TestScenario;
import NG.Core.Game;
import NG.Core.ModLoader;
import NG.Entities.Locomotive;
import NG.Entities.Train;
import NG.Entities.Wagon;
import NG.GUIMenu.Components.*;
import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.SComponentProperties;
import NG.Network.NetworkNode;
import NG.Network.RailNode;
import NG.Tools.Logger;
import NG.Tools.Toolbox;
import NG.Tracks.TrackPiece;
import org.joml.Vector2i;

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
    public static final SComponentProperties BUTTON_PROPERTIES_STATIC = new SComponentProperties(
            200, 60, false, false, NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER
    );
    public static final SComponentProperties BUTTON_PROPERTIES_STRETCH = new SComponentProperties(
            200, 60, true, false, NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER
    );
    public static final SComponentProperties MAIN_BUTTON_PROPERTIES = new SComponentProperties(
            300, 100, false, false, NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER
    );
    public static final SComponentProperties TEXT_PROPERTIES = new SComponentProperties(
            0, 50, false, false, NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.LEFT
    );

    private final Vector2i topButtonPos;
    private final Vector2i bottomButtonPos;
    private final Game game;
    private final SFrame newGameFrame;

    public MainMenu(Game game, ModLoader modManager, Runnable terminateProgram) {
        super("Main Menu", 400, 500, false);
        this.game = game;
        topButtonPos = new Vector2i(1, -1);
        bottomButtonPos = new Vector2i(1, NUM_BUTTONS);

        newGameFrame = new NewGameFrame(game, modManager);
        Scenario testScenario = new TestScenario(modManager);

        STextComponent newGame = new SButton("Start new game", this::showNewGame, MAIN_BUTTON_PROPERTIES);
        STextComponent justStart = new SButton("Start Testworld", () -> testScenario.apply(game), MAIN_BUTTON_PROPERTIES);
        STextComponent exitGame = new SButton("Exit game", terminateProgram, MAIN_BUTTON_PROPERTIES);

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

    public static SToolBar getToolBar(Game game, ModLoader modLoader) {
        SToolBar toolBar = new SToolBar(game, true);

        toolBar.addButton(
                "Build Track",
                () -> game.gui().addFrame(new BuildMenu(game))
        );

        toolBar.addButton( // TODO remove this debug option
                "New Train",
                () -> game.inputHandling().setMouseTool(new EntityActionTool(
                        game, e -> e instanceof TrackPiece,
                        entity -> buildMaxLengthTrain((TrackPiece) entity, game)
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

        return toolBar;
    }

    private static void buildMaxLengthTrain(TrackPiece trackPiece, Game game) {
        trackPiece.setOccupied(true);

        float trackLength = trackPiece.getLength();
        double gameTime = game.timer().getGameTime();
        Train construction = new Train(game, Toolbox.random.nextInt(100), gameTime, trackPiece);
        game.state().addEntity(construction);

        List<Locomotive.Properties> locomotiveTypes = game.objectTypes().locomotiveTypes;
        List<Wagon.Properties> wagonTypes = game.objectTypes().wagonTypes;

        construction.addElement(new Locomotive(locomotiveTypes.get(0)));
        Wagon wagon = new Wagon(wagonTypes.get(0));

        while (construction.getLength() + wagon.properties.length < trackLength) {
            construction.addElement(wagon);
            wagon = new Wagon(wagonTypes.get(0));
        }
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
