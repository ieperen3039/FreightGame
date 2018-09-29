package NG.ScreenOverlay;

import NG.Engine.Game;
import NG.GameState.MapGeneratorMod;
import NG.Mods.Mod;
import NG.ScreenOverlay.Frames.Components.*;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Geert van Ieperen. Created on 28-9-2018.
 */
public class MainMenu extends SFrame {
    private static final int NUM_TOP_BUTTONS = 10;
    private static final int NUM_BOT_BUTTONS = 2;
    private final Vector2i topButtonPos;
    private final Vector2i bottomButtonPos;
    public static final int NUM_BUTTONS = NUM_TOP_BUTTONS + NUM_BOT_BUTTONS + 1;
    private final Game game;

    public MainMenu(Game game, Runnable exitGameAction) {
        super("Main Menu");
        this.game = game;
        topButtonPos = new Vector2i(1, -1);
        bottomButtonPos = new Vector2i(1, NUM_BUTTONS + 2);

        SContainer buttons = new SPanel(3, NUM_BUTTONS);
        buttons.add(new SFiller(), new Vector2i(2, NUM_TOP_BUTTONS));

        SButton newGame = new SButton("Start new game", this::showNewGame);
        buttons.add(newGame, onTop());

        SButton exitGame = new SButton("Exit game", exitGameAction);

    }

    private void showNewGame() {
        Collection<MapGeneratorMod> generators = new ArrayList<>();

        for (Mod mod : game.modList()) {
            if (mod instanceof MapGeneratorMod) {
                generators.add((MapGeneratorMod) mod);
            }
        }

        SFrame newGameFrame = new SFrame("New Game Parameters");


    }

    private Vector2i onTop() {
        return topButtonPos.add(0, 1);
    }

    private Vector2i onBot() {
        return bottomButtonPos.sub(0, 1);
    }

    @Override
    public void setPosition(int x, int y) {
        // Non.
    }
}
