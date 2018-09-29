package NG.ScreenOverlay;

import NG.Engine.Game;
import NG.ScreenOverlay.Frames.Components.SButton;
import NG.ScreenOverlay.Frames.Components.SContainer;
import NG.ScreenOverlay.Frames.Components.SFrame;
import NG.ScreenOverlay.Frames.Components.SPanel;

/**
 * @author Geert van Ieperen. Created on 28-9-2018.
 */
public class MainMenu extends SFrame {
    private static final int NUM_BUTTONS = 10;

    public MainMenu(Game game) {
        super("Main Menu");
        SContainer buttons = new SPanel(3, NUM_BUTTONS);

        SButton start_game = new SButton("Start new game", () -> game.state().ge);


        add(start_game, MIDDLE);
    }

    @Override
    public void setPosition(int x, int y) {
        // Non.
    }
}
