package NG.Tracks;

import NG.Core.Game;
import NG.Entities.Entity;
import NG.GUIMenu.Components.SToggleButton;
import NG.GUIMenu.Menu.EntityActionTool;
import NG.Network.Signal;

/**
 * @author Geert van Ieperen created on 29-4-2020.
 */
public class Remover extends EntityActionTool {
    protected final Runnable deactivation;

    public Remover(Game game, SToggleButton source) {
        super(game, e -> true, e -> removeEntity(e, game));
        this.deactivation = () -> source.setActive(false);
    }

    private static void removeEntity(Entity entity, Game game) {
        double gameTime = game.timer().getGameTime();

        if (entity instanceof TrackPiece) {
            RailTools.removeTrackPiece((TrackPiece) entity, gameTime);

        } else if (entity instanceof Signal) {
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
