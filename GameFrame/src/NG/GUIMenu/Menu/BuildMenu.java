package NG.GUIMenu.Menu;

import NG.Core.Game;
import NG.Entities.StationBuilder;
import NG.GUIMenu.Components.SContainer;
import NG.GUIMenu.Components.SDropDown;
import NG.GUIMenu.Components.SFrame;
import NG.GUIMenu.Components.SToggleButton;
import NG.Network.SignalBuilder;
import NG.Tracks.Remover;
import NG.Tracks.TrackBuilder;
import NG.Tracks.TrackType;

import java.util.List;

/**
 * @author Geert van Ieperen. Created on 5-11-2018.
 */
public class BuildMenu extends SFrame {
    public BuildMenu(Game game) {
        super("Track Build Menu");

        List<TrackType> trackTypes = game.objectTypes().trackTypes;
        SDropDown typeChooser = new SDropDown(game.gui(), 250, 50, trackTypes.get(0), trackTypes);

        SToggleButton buildTrack = new SToggleButton("Build Tracks", 250, 50);
        buildTrack.addStateChangeListener(
                (active) -> game.inputHandling()
                        .setMouseTool(active ? new TrackBuilder(game, trackTypes.get(typeChooser.getSelectedIndex()), buildTrack) : null)
        );

        SToggleButton buildStation = new SToggleButton("Build Station", 250, 50);
        buildStation.addStateChangeListener(
                (active) -> game.inputHandling()
                        .setMouseTool(active ? new StationBuilder(game, buildStation, trackTypes.get(typeChooser.getSelectedIndex())) : null)
        );

        SToggleButton buildSignal = new SToggleButton("Build signal", 250, 50);
        buildSignal.addStateChangeListener(
                (active) -> game.inputHandling()
                        .setMouseTool(active ? new SignalBuilder(game, buildSignal) : null)
        );

        SToggleButton removeElement = new SToggleButton("Remove", 250, 50);
        removeElement.addStateChangeListener(
                (active) -> game.inputHandling().setMouseTool(active ? new Remover(game, removeElement) : null)
        );

        setMainPanel(SContainer.column(
                typeChooser, buildTrack, buildStation, buildSignal, removeElement
        ));
        setSize(200, 0);
    }
}
