package NG.GUIMenu.Menu;

import NG.Content.LinearConnectionSc;
import NG.Content.Scenario;
import NG.Content.TriangleStationsSc;
import NG.Core.Game;
import NG.Core.ModLoader;
import NG.GUIMenu.Components.SButton;
import NG.GUIMenu.Components.SContainer;
import NG.GUIMenu.Components.SFiller;
import NG.GUIMenu.Components.SFrame;
import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.SComponentProperties;
import org.joml.Vector2i;

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
        Scenario emptyScenario = new Scenario.Empty(modManager);
        Scenario triangleScenario = new TriangleStationsSc(modManager);
        Scenario linearScenario = new LinearConnectionSc(modManager);

        setMainPanel(SContainer.row(
                new SFiller(),
                SContainer.column(
                        new SButton("Start new game", this::showNewGame, MAIN_BUTTON_PROPERTIES),
                        new SButton("Start Empty", () -> emptyScenario.apply(game), MAIN_BUTTON_PROPERTIES),
                        new SButton("Start Triangle", () -> triangleScenario.apply(game), MAIN_BUTTON_PROPERTIES),
                        new SButton("Start Linear", () -> linearScenario.apply(game), MAIN_BUTTON_PROPERTIES),
                        new SFiller().setGrowthPolicy(false, true),
                        new SButton("Exit game", terminateProgram, MAIN_BUTTON_PROPERTIES)
                ),
                new SFiller()
        ));
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
