package NG.Menu.Main;

import NG.Content.Scenario.FileScenario;
import NG.Content.Scenario.LinearConnectionSc;
import NG.Content.Scenario.Scenario;
import NG.Content.Scenario.TriangleStationsSc;
import NG.Core.Game;
import NG.Core.ModLoader;
import NG.GUIMenu.Components.SButton;
import NG.GUIMenu.Components.SContainer;
import NG.GUIMenu.Components.SFiller;
import NG.GUIMenu.Components.SFrame;
import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.SComponentProperties;

import java.io.IOException;

/**
 * @author Geert van Ieperen. Created on 28-9-2018.
 */
public class MainMenu extends SFrame {
    public static final int STANDARD_BUTTON_WIDTH = 150;
    public static final int STANDARD_BUTTON_HEIGHT = 30;

    public static final SComponentProperties BUTTON_PROPERTIES_STATIC = new SComponentProperties(
            STANDARD_BUTTON_WIDTH, STANDARD_BUTTON_HEIGHT,
            false, false,
            NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER_MIDDLE
    );
    public static final SComponentProperties BUTTON_PROPERTIES_STRETCH = new SComponentProperties(
            STANDARD_BUTTON_WIDTH, STANDARD_BUTTON_HEIGHT,
            true, false,
            NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER_MIDDLE
    );
    public static final SComponentProperties BUTTON_PROPERTIES_SMALL_STATIC = new SComponentProperties(
            STANDARD_BUTTON_WIDTH / 2, STANDARD_BUTTON_HEIGHT / 2,
            false, false,
            NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER_MIDDLE
    );
    public static final SComponentProperties MAIN_BUTTON_PROPERTIES = new SComponentProperties(
            (int) (STANDARD_BUTTON_WIDTH * 1.5f), (int) (STANDARD_BUTTON_HEIGHT * 1.5f),
            false, false,
            NGFonts.TextType.TITLE, SFrameLookAndFeel.Alignment.CENTER_MIDDLE
    );
    public static final SComponentProperties TEXT_PROPERTIES = new SComponentProperties(
            0, NGFonts.SIZE_LARGE * 2,
            false, false,
            NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.LEFT_MIDDLE
    );
    @SuppressWarnings("SuspiciousNameCombination")
    public static final SComponentProperties SQUARE_BUTTON_PROPS = new SComponentProperties(
            STANDARD_BUTTON_HEIGHT, STANDARD_BUTTON_HEIGHT,
            false, false,
            NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER_MIDDLE
    );

    private final Game game;
    private final SFrame newGameFrame;

    public MainMenu(Game game, ModLoader modManager, Runnable terminateProgram) {
        super("Main Menu", 400, 500, false);
        this.game = game;

        newGameFrame = new NewGameFrame(game, modManager);
        Scenario emptyScenario = new Scenario.Empty(modManager);
        Scenario triangleScenario = new TriangleStationsSc(modManager);
        Scenario linearScenario = new LinearConnectionSc(modManager);
        Scenario firstScenario = getFileScenario(modManager, "first");

        setMainPanel(SContainer.row(
                new SFiller(),
                SContainer.column(
                        new SButton("Start new game", this::showNewGame, MAIN_BUTTON_PROPERTIES),
                        new SButton("Start Empty", () -> emptyScenario.apply(game), MAIN_BUTTON_PROPERTIES),
                        new SButton("Start Triangle", () -> triangleScenario.apply(game), MAIN_BUTTON_PROPERTIES),
                        new SButton("Start Linear", () -> linearScenario.apply(game), MAIN_BUTTON_PROPERTIES),
                        new SButton("Start First Scenario", () -> firstScenario.apply(game), MAIN_BUTTON_PROPERTIES),
                        SFiller.vertical(),
                        new SButton("Exit game", terminateProgram, MAIN_BUTTON_PROPERTIES)
                ),
                new SFiller()
        ));
    }

    public Scenario getFileScenario(ModLoader modManager, String first) {
        try {
            return new FileScenario(modManager, first);
        } catch (IOException e) {
            return new Scenario.Empty(modManager);
        }
    }

    private void showNewGame() {
        newGameFrame.setVisible(true);
        game.gui().addFrame(newGameFrame);
    }

}
