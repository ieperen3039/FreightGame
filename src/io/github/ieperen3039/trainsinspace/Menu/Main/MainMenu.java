package io.github.ieperen3039.trainsinspace.Menu.Main;

import io.github.ieperen3039.ngn.UserInterface.Components.SButton;
import io.github.ieperen3039.ngn.UserInterface.Components.SContainer;
import io.github.ieperen3039.ngn.UserInterface.Components.SFiller;
import io.github.ieperen3039.ngn.UserInterface.Components.SFrame;
import io.github.ieperen3039.ngn.UserInterface.Rendering.NGFont;
import io.github.ieperen3039.ngn.UserInterface.Rendering.SFrameLookAndFeel;
import io.github.ieperen3039.ngn.UserInterface.SComponentProperties;
import io.github.ieperen3039.trainsinspace.Content.Scenario.FileScenario;
import io.github.ieperen3039.trainsinspace.Content.Scenario.LinearConnectionSc;
import io.github.ieperen3039.trainsinspace.Content.Scenario.Scenario;
import io.github.ieperen3039.trainsinspace.Content.Scenario.TriangleStationsSc;
import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.Core.ModLoader;
import io.github.ieperen3039.trainsinspace.Core.TrainGame;

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
            NGFont.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER_MIDDLE
    );
    public static final SComponentProperties BUTTON_PROPERTIES_STRETCH = new SComponentProperties(
            STANDARD_BUTTON_WIDTH, STANDARD_BUTTON_HEIGHT,
            true, false,
            NGFont.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER_MIDDLE
    );
    public static final SComponentProperties BUTTON_PROPERTIES_SMALL_STATIC = new SComponentProperties(
            STANDARD_BUTTON_WIDTH / 2, STANDARD_BUTTON_HEIGHT / 2,
            false, false,
            NGFont.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER_MIDDLE
    );
    public static final SComponentProperties MAIN_BUTTON_PROPERTIES = new SComponentProperties(
            (int) (STANDARD_BUTTON_WIDTH * 1.5f), (int) (STANDARD_BUTTON_HEIGHT * 1.5f),
            false, false,
            NGFont.TextType.TITLE, SFrameLookAndFeel.Alignment.CENTER_MIDDLE
    );
    public static final SComponentProperties TEXT_PROPERTIES = new SComponentProperties(
            0, 24,
            false, false,
            NGFont.TextType.REGULAR, SFrameLookAndFeel.Alignment.LEFT_MIDDLE
    );
    @SuppressWarnings("SuspiciousNameCombination")
    public static final SComponentProperties SQUARE_BUTTON_PROPS = new SComponentProperties(
            STANDARD_BUTTON_HEIGHT, STANDARD_BUTTON_HEIGHT,
            false, false,
            NGFont.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER_MIDDLE
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
                        new SButton("Start new game", MAIN_BUTTON_PROPERTIES, this::showNewGame),
                        new SButton("Load Game", MAIN_BUTTON_PROPERTIES, () -> modManager.loadGame(TrainGame.SAVE_FILE)),
                        new SButton("Start Empty", MAIN_BUTTON_PROPERTIES, () -> emptyScenario.apply(game)),
                        new SButton("Start Triangle", MAIN_BUTTON_PROPERTIES, () -> triangleScenario.apply(game)),
                        new SButton("Start Linear", MAIN_BUTTON_PROPERTIES, () -> linearScenario.apply(game)),
                        new SButton("Start First Scenario", MAIN_BUTTON_PROPERTIES, () -> firstScenario.apply(game)),
                        SFiller.vertical(),
                        new SButton("Exit game", MAIN_BUTTON_PROPERTIES, terminateProgram)
                ),
                new SFiller()
        ));
    }

    public Scenario getFileScenario(ModLoader modManager, String file) {
        try {
            return new FileScenario(modManager, file);
        } catch (IOException e) {
            return new Scenario.Empty(modManager);
        }
    }

    private void showNewGame() {
        newGameFrame.setVisible(true);
        game.gui().addFrame(newGameFrame);
    }
}
