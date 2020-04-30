package NG.GUIMenu.Menu;

import NG.Core.Game;
import NG.Entities.StationBuilder;
import NG.GUIMenu.Components.SContainer;
import NG.GUIMenu.Components.SFrame;
import NG.GUIMenu.Components.SToggleButton;
import NG.Tracks.Remover;
import NG.Tracks.TrackBuilder;
import NG.Tracks.TrackType;

/**
 * @author Geert van Ieperen. Created on 5-11-2018.
 */
public class BuildMenu extends SFrame {
    public BuildMenu(Game game, TrackType trackType) {
        super("Build Menu " + trackType.toString());

        SToggleButton buildTrack = new SToggleButton("Build Tracks", 250, 50);
        buildTrack.addStateChangeListener(
                (active) -> game.inputHandling()
                        .setMouseTool(active ? new TrackBuilder(game, trackType, buildTrack) : null)
        );

        SToggleButton buildStation = new SToggleButton("Build Station", 250, 50);
        buildStation.addStateChangeListener(
                (active) -> game.inputHandling()
                        .setMouseTool(active ? new StationBuilder(game, buildStation, trackType) : null)
        );

        SToggleButton removeElement = new SToggleButton("Remove", 250, 50);
        removeElement.addStateChangeListener(
                (active) -> game.inputHandling().setMouseTool(active ? new Remover(game, removeElement) : null)
        );

        setMainPanel(SContainer.column(
                buildTrack, buildStation, removeElement
        ));
        setSize(200, 0);
    }
}
