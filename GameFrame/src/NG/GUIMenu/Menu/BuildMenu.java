package NG.GUIMenu.Menu;

import NG.Core.Game;
import NG.Entities.StationBuilder;
import NG.GUIMenu.Components.SContainer;
import NG.GUIMenu.Components.SDropDown;
import NG.GUIMenu.Components.SFrame;
import NG.GUIMenu.Components.SToggleButton;
import NG.GUIMenu.SComponentProperties;
import NG.Network.SignalBuilder;
import NG.Tracks.Remover;
import NG.Tracks.TrackBuilder;
import NG.Tracks.TrackType;

import java.util.List;

/**
 * @author Geert van Ieperen. Created on 5-11-2018.
 */
public class BuildMenu extends SFrame {
    private static final SComponentProperties BUTTON_PROPERTIES = new SComponentProperties(250, 50);

    public BuildMenu(Game game) {
        super("Track Build Menu");

        List<TrackType> trackTypes = game.objectTypes().trackTypes;
        SDropDown typeChooser = new SDropDown(game.gui(), BUTTON_PROPERTIES, 0, trackTypes);

        SToggleButton buildTrack = new SToggleButton("Build Tracks", BUTTON_PROPERTIES);
        buildTrack.addStateChangeListener(
                (active) -> game.inputHandling()
                        .setMouseTool(active ? new TrackBuilder(game, trackTypes.get(typeChooser.getSelectedIndex()), buildTrack) : null)
        );

        SToggleButton buildStation = new SToggleButton("Build Station", BUTTON_PROPERTIES);
        buildStation.addStateChangeListener(
                (active) -> game.inputHandling()
                        .setMouseTool(active ? new StationBuilder(game, buildStation, trackTypes.get(typeChooser.getSelectedIndex())) : null)
        );

        SToggleButton buildSignal = new SToggleButton("Build signal", BUTTON_PROPERTIES);
        buildSignal.addStateChangeListener(
                (active) -> game.inputHandling()
                        .setMouseTool(active ? new SignalBuilder(game, buildSignal) : null)
        );

        SToggleButton removeElement = new SToggleButton("Remove", BUTTON_PROPERTIES);
        removeElement.addStateChangeListener(
                (active) -> game.inputHandling().setMouseTool(active ? new Remover(game, removeElement) : null)
        );

        setMainPanel(SContainer.column(
                typeChooser, buildTrack, buildStation, buildSignal, removeElement
        ));
        setSize(200, 0);
    }
}
