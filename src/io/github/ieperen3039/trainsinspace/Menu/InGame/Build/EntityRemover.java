package io.github.ieperen3039.trainsinspace.Menu.InGame.Build;

import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.Entities.Entity;
import io.github.ieperen3039.trainsinspace.GUIMenu.Components.SToggleButton;
import io.github.ieperen3039.trainsinspace.InputHandling.MouseTool.EntityActionTool;
import io.github.ieperen3039.trainsinspace.Network.Signal;
import io.github.ieperen3039.trainsinspace.Network.SignalEntity;
import io.github.ieperen3039.trainsinspace.Tracks.RailTools;
import io.github.ieperen3039.trainsinspace.Tracks.TrackPiece;

/**
 * @author Geert van Ieperen created on 29-4-2020.
 */
public class EntityRemover extends EntityActionTool {
    protected final Runnable deactivation;

    public EntityRemover(Game game, SToggleButton source) {
        super(game, e -> true, e -> removeEntity(e, game));
        this.deactivation = () -> source.setActive(false);
    }

    private static void removeEntity(Entity entity, Game game) {
        double gameTime = game.timer().getGameTime();

        if (entity instanceof TrackPiece) {
            RailTools.removeTrackPiece((TrackPiece) entity, gameTime);

        } else if (entity instanceof SignalEntity) {
            ((Signal) entity).getNode().removeSignal(game);

        } else {
            entity.despawn(gameTime);
        }
    }

    @Override
    public void dispose() {
        deactivation.run();
    }
}
