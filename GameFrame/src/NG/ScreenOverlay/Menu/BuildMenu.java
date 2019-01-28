package NG.ScreenOverlay.Menu;

import NG.Engine.Game;
import NG.Entities.BasicStation;
import NG.ScreenOverlay.Frames.Components.SFrame;
import NG.ScreenOverlay.Frames.Components.SPanel;
import NG.ScreenOverlay.Frames.Components.SToggleButton;
import NG.Tracks.TrackBuilder;
import NG.Tracks.TrackMod;
import org.joml.Vector2i;

/**
 * @author Geert van Ieperen. Created on 5-11-2018.
 */
public class BuildMenu extends SFrame {
    public BuildMenu(Game game, TrackMod.TrackType trackType) {
        super("Build Menu " + trackType.name());

        SPanel mainPanel = new SPanel(1, 2);

        SToggleButton buildTrack = new SToggleButton("Build Tracks", 250, 50);
        mainPanel.add(buildTrack, new Vector2i(0, 0));

        buildTrack.addStateChangeListener(
                () -> game.inputHandling()
                        .setMouseTool(buildTrack.getState() ?
                                new TrackBuilder(game, trackType, buildTrack) :
                                null
                        )
        );

        SToggleButton buildStation = new SToggleButton("Build Station", 250, 50);
        buildStation.addStateChangeListener(
                () -> game.inputHandling()
                        .setMouseTool(buildStation.getState() ?
                                new BasicStation.Builder(game, buildStation, trackType) :
                                null
                        )
        );
        mainPanel.add(buildStation, new Vector2i(0, 1));

        setMainPanel(mainPanel);
        setSize(200, 0);
    }
}
