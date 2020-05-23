package NG.Tracks;

import NG.Core.Game;
import NG.Entities.Entity;
import NG.Entities.Station;
import NG.Entities.StationGhost;
import NG.GUIMenu.Components.SToggleButton;
import NG.InputHandling.MouseTools.ToggleMouseTool;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 29-4-2020.
 */
public class Remover extends ToggleMouseTool {
    public Remover(Game game, SToggleButton source) {
        super(game, () -> source.setActive(false));
    }

    @Override
    public void apply(Entity entity, Vector3fc origin, Vector3fc direction) {
        double gameTime = game.timer().getGameTime();
        switch (getMouseAction()) {
            case PRESS_ACTIVATE:
                if (entity instanceof TrackPiece) {
                    RailTools.removeTrackPiece((TrackPiece) entity, gameTime);

                } else if (entity instanceof StationGhost) {
                    Station station = (Station) entity;
                    station.despawn(gameTime);
                }
        }
    }

    @Override
    public void apply(Vector3fc position, Vector3fc origin, Vector3fc direction) {

    }
}
