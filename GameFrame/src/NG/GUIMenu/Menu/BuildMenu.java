package NG.GUIMenu.Menu;

import NG.Core.Game;
import NG.Entities.BasicStation;
import NG.GUIMenu.Components.SFrame;
import NG.GUIMenu.Components.SPanel;
import NG.GUIMenu.Components.SToggleButton;
import NG.Tracks.TrackBuilder;
import NG.Tracks.TrackType;
import org.joml.Vector2i;

/**
 * @author Geert van Ieperen. Created on 5-11-2018.
 */
public class BuildMenu extends SFrame {
    public BuildMenu(Game game, TrackType trackType) {
        super("Build Menu " + trackType.toString());

        SPanel mainPanel = new SPanel(1, 2);

        SToggleButton buildTrack = new SToggleButton("Build Tracks", 250, 50);
        buildTrack.addStateChangeListener(
                (active) -> game.inputHandling()
                        .setMouseTool(active ? new TrackBuilder(game, trackType, buildTrack) : null)
        );
        mainPanel.add(buildTrack, new Vector2i(0, 0));

        SToggleButton buildStation = new SToggleButton("Build Station", 250, 50);
        buildStation.addStateChangeListener(
                (active) -> game.inputHandling()
                        .setMouseTool(active ? new BasicStation.Builder(game, buildStation, trackType) : null)
        );
        mainPanel.add(buildStation, new Vector2i(0, 1));

        setMainPanel(mainPanel);
        setSize(200, 0);
    }
}
