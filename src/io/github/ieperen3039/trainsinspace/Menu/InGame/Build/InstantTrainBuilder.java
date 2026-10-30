package io.github.ieperen3039.trainsinspace.Menu.InGame.Build;

import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.Entities.Locomotive;
import io.github.ieperen3039.trainsinspace.Entities.Train;
import io.github.ieperen3039.trainsinspace.Entities.Wagon;
import io.github.ieperen3039.trainsinspace.GUIMenu.Components.SToggleButton;
import io.github.ieperen3039.trainsinspace.InputHandling.MouseTool.AbstractMouseTool;
import io.github.ieperen3039.trainsinspace.InputHandling.MouseTool.EntityActionTool;
import io.github.ieperen3039.trainsinspace.Tracks.TrackPiece;

import java.util.List;

/**
 * @author Geert van Ieperen created on 21-6-2020.
 */
public class InstantTrainBuilder extends EntityActionTool {
    private final Runnable deactivation;

    public InstantTrainBuilder(Game game, SToggleButton source) {
        super(game, entity -> entity instanceof TrackPiece, e -> addTrain(game, (TrackPiece) e));
        this.deactivation = () -> source.setActive(false);
    }

    static void addTrain(Game game, TrackPiece trackPiece) {
        trackPiece.setOccupied(true);

        float trackLength = trackPiece.getLength();
        double gameTime = game.timer().getGameTime();
        Train construction = new Train(game, 0, gameTime, trackPiece);
        game.state().addEntity(construction);

        List<Locomotive.Properties> locomotiveTypes = game.objectTypes().locomotiveTypes;
        List<Wagon.Properties> wagonTypes = game.objectTypes().wagonTypes;

        construction.addElement(new Locomotive(locomotiveTypes.get(0)));
        Wagon wagon = new Wagon(wagonTypes.get(0));

        while (construction.getLength() + wagon.getProperties().length <= trackLength) {
            construction.addElement(wagon);
            wagon = new Wagon(wagonTypes.get(0));
        }

        // click on it
        construction.reactMouse(AbstractMouseTool.MouseAction.PRESS_ACTIVATE, game.keyControl());
    }

    @Override
    public void dispose() {
        deactivation.run();
    }
}
