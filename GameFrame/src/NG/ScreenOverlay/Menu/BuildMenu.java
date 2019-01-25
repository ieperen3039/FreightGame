package NG.ScreenOverlay.Menu;

import NG.Engine.Game;
import NG.ScreenOverlay.Frames.Components.SButton;
import NG.ScreenOverlay.Frames.Components.SFrame;
import NG.ScreenOverlay.Frames.Components.SPanel;
import NG.ScreenOverlay.Frames.Components.SToggleButton;
import NG.Tracks.TrackBuilder;
import NG.Tracks.TrackMod;
import org.joml.Vector2i;

import java.util.List;

/**
 * @author Geert van Ieperen. Created on 5-11-2018.
 */
public class BuildMenu extends SFrame {
    public BuildMenu(Game game) {
        super("Build Menu");
        List<TrackMod.TrackType> tracks = game.objectTypes().getTrackTypes();

        SPanel mainPanel = new SPanel(1, tracks.size() + 2);

        for (int i = 0; i < tracks.size(); i++) {
            TrackMod.TrackType trackType = tracks.get(i);
            SToggleButton buildTrack = new SToggleButton(trackType.name(), 250, 50);
            mainPanel.add(buildTrack, new Vector2i(0, i));

            buildTrack.addStateChangeListener(
                    (active) -> game.inputHandling()
                            .setMouseTool(active ? new TrackBuilder(game, trackType, buildTrack) : null)
            );
        }

        SButton demolishTracks = new SButton("Remove", 250, 50);
        mainPanel.add(demolishTracks, new Vector2i(0, tracks.size()));

        setMainPanel(mainPanel);
        setSize(200, 0);
    }
}
