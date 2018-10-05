package NG.ScreenOverlay;

import NG.Engine.Game;
import NG.Mods.Mod;
import NG.ScreenOverlay.Frames.Components.*;
import org.joml.Vector2i;

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

    public MainMenu(Game game, Runnable exitGameAction) {
        super("Main Menu", 400, 500, true);
        this.game = game;
        topButtonPos = new Vector2i(1, -1);
        bottomButtonPos = new Vector2i(1, NUM_BUTTONS);

        SContainer buttons = new SPanel(3, NUM_BUTTONS);

        SButton newGame = new SButton("Start new game", this::showNewGame, BUTTON_MIN_WIDTH, BUTTON_MIN_HEIGHT);
        buttons.add(newGame, onTop());

        SButton dummy = new SButton("Nothing", () -> {
        }, BUTTON_MIN_WIDTH, BUTTON_MIN_HEIGHT);
        buttons.add(dummy, onTop());

        SButton exitGame = new SButton("Exit game", exitGameAction, BUTTON_MIN_WIDTH, BUTTON_MIN_HEIGHT);
        buttons.add(exitGame, onBot());

        Vector2i mid = onTop();
        buttons.add(new SFiller(), new Vector2i(0, mid.y));
        buttons.add(new SFiller(), new Vector2i(1, mid.y));
        buttons.add(new SFiller(), new Vector2i(2, mid.y));

        setMainPanel(buttons);
    }

    private void showNewGame() {
        List<Mod> modList = game.modList();
        int nOfMods = modList.size();

        SFrame newGameFrame = new SFrame("New Game Parameters");
        final int ROWS = 3;
        final int COLS = 3;

        SPanel mainPanel = new SPanel(COLS, ROWS);
        mainPanel.add(new SFiller(), new Vector2i(0, 0));
        mainPanel.add(new SFiller(), new Vector2i(COLS - 1, ROWS - 1));

        // add mod buttons
        SContainer modPanel = new SPanel(1, nOfMods);
        Vector2i pos = new Vector2i(0, -1);
        for (Mod mod : modList) {
            SToggleButton button = new SToggleButton(mod.getModName(), BUTTON_MIN_WIDTH, BUTTON_MIN_HEIGHT);
            modPanel.add(button, pos.add(0, 1));
        }

        mainPanel.add(modPanel, new Vector2i(1, 1));
        newGameFrame.setMainPanel(mainPanel);
        newGameFrame.setVisible(true);
        newGameFrame.pack();
        newGameFrame.addToSize(50, 50);

        game.gui().addFrame(newGameFrame);
    }

    private Vector2i onTop() {
        return topButtonPos.add(0, 1);
    }

    private Vector2i onBot() {
        return bottomButtonPos.sub(0, 1);
    }
}
