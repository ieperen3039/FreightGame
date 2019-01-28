package NG.ScreenOverlay.Menu;

import NG.Engine.Game;
import NG.ScreenOverlay.Frames.GUIManager;
import NG.ScreenOverlay.ToolBar;
import NG.Tools.Logger;
import NG.Tracks.TrackMod;

import java.util.List;

/**
 * @author Geert van Ieperen. Created on 21-11-2018.
 */
public class FreightToolBar extends ToolBar {
    /**
     * Creates the toolbar on the top of the GUI. One instance of this should be passed to the {@link GUIManager}.
     * @param game     a reference to the game itself.
     * @param stopGame the action to run when exit is pressed
     */
    public FreightToolBar(Game game, Runnable stopGame) {
        super(game);

        addButton("Exit", stopGame);
        addButton("$$$", () -> Logger.INFO.print("You are given one (1) arbitrary value(s)"));
        addSeparator();

        List<TrackMod.TrackType> tracks = game.objectTypes().getTrackTypes();
        for (TrackMod.TrackType trackType : tracks) {
            addButton("B", () -> game.gui().addFrame(new BuildMenu(game, trackType)));
        }
    }
}
