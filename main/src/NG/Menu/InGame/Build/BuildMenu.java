package NG.Menu.InGame.Build;

import NG.Core.Game;
import NG.GUIMenu.Components.*;
import NG.GUIMenu.SComponentProperties;
import NG.Tracks.TrackType;

import java.util.List;

/**
 * @author Geert van Ieperen. Created on 5-11-2018.
 */
public class BuildMenu extends SDecorator {
    private static final SComponentProperties BUTTON_PROPERTIES = new SComponentProperties(250, 50);

    public BuildMenu(Game game, Runnable closeAction) {
        List<TrackType> trackTypes = game.objectTypes().trackTypes;
        SDropDown typeChooser = new SDropDown(game.gui(), BUTTON_PROPERTIES, 0, trackTypes);

        SToggleButton buildTrack = new SToggleButton("Build Tracks", BUTTON_PROPERTIES);
        buildTrack.addStateChangeListener(
                (active) -> game.inputHandling()
                        .setMouseTool(active ? new TrackBuilder(game, getType(trackTypes, typeChooser), buildTrack) : null)
        );

        SToggleButton buildStation = new SToggleButton("Build Station", BUTTON_PROPERTIES);
        buildStation.addStateChangeListener(
                (active) -> game.inputHandling()
                        .setMouseTool(active ? new StationBuilder(game, buildStation, getType(trackTypes, typeChooser)) : null)
        );

        SToggleButton buildSignal = new SToggleButton("Build Signal", BUTTON_PROPERTIES);
        buildSignal.addStateChangeListener(
                (active) -> game.inputHandling()
                        .setMouseTool(active ? new SignalBuilder(game, buildSignal) : null)
        );

        SToggleButton buildTrain = new SToggleButton("Build Train", BUTTON_PROPERTIES);
        buildTrain.addStateChangeListener((active) -> game.inputHandling()
                .setMouseTool(active ? new InstantTrainBuilder(game, buildTrain) : null)
        );

        SToggleButton removeElement = new SToggleButton("Remove", BUTTON_PROPERTIES);
        removeElement.addStateChangeListener(
                (active) -> game.inputHandling()
                        .setMouseTool(active ? new EntityRemover(game, removeElement) : null)
        );

        SButton closeMenu = new SButton("Close", () -> {
            game.inputHandling().setMouseTool(null);
            closeAction.run();
        });

        setContents(SContainer.column(
                typeChooser, buildTrack, buildStation, buildSignal, buildTrain, removeElement, closeMenu
        ));

        setSize(200, 0);
        setGrowthPolicy(false, false);
    }

    private TrackType getType(List<TrackType> trackTypes, SDropDown typeChooser) {
        return trackTypes.get(typeChooser.getSelectedIndex());
    }
}
