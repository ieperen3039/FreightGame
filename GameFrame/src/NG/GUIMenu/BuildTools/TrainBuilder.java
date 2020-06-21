package NG.GUIMenu.BuildTools;

import NG.Core.Game;
import NG.Entities.Locomotive;
import NG.Entities.Train;
import NG.Entities.Wagon;
import NG.GUIMenu.Components.SToggleButton;
import NG.GUIMenu.Menu.EntityActionTool;
import NG.InputHandling.MouseTools.AbstractMouseTool;
import NG.Tools.Toolbox;
import NG.Tracks.TrackPiece;

import java.util.List;

/**
 * @author Geert van Ieperen created on 21-6-2020.
 */
public class TrainBuilder extends EntityActionTool {
    private final Runnable deactivation;

    public TrainBuilder(Game game, SToggleButton source) {
        super(game, entity -> entity instanceof TrackPiece, e -> addTrain(game, (TrackPiece) e));
        this.deactivation = () -> source.setActive(false);
    }

    static void addTrain(Game game, TrackPiece trackPiece) {
        trackPiece.setOccupied(true);

        float trackLength = trackPiece.getLength();
        double gameTime = game.timer().getGameTime();
        Train construction = new Train(game, Toolbox.random.nextInt(100), gameTime, trackPiece);
        game.state().addEntity(construction);

        List<Locomotive.Properties> locomotiveTypes = game.objectTypes().locomotiveTypes;
        List<Wagon.Properties> wagonTypes = game.objectTypes().wagonTypes;

        construction.addElement(new Locomotive(locomotiveTypes.get(0)));
        Wagon wagon = new Wagon(wagonTypes.get(0));

        while (construction.getLength() + wagon.properties.length < trackLength) {
            construction.addElement(wagon);
            wagon = new Wagon(wagonTypes.get(0));
        }

        // click on it
        construction.reactMouse(AbstractMouseTool.MouseAction.PRESS_ACTIVATE);
    }

    @Override
    public void dispose() {
        deactivation.run();
    }
}
