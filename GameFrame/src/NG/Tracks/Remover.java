package NG.Tracks;

import NG.Core.Game;
import NG.Entities.Entity;
import NG.Entities.Station;
import NG.Entities.StationImpl;
import NG.GUIMenu.Components.SToggleButton;
import NG.InputHandling.MouseTools.ToggleMouseTool;
import NG.Network.NetworkNode;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 29-4-2020.
 */
public class Remover extends ToggleMouseTool {
    public Remover(Game game, SToggleButton source) {
        super(game, () -> source.setActive(false));
    }

    @Override
    public void apply(Entity entity, int xSc, int ySc) {
        if (entity instanceof TrackPiece) {
            NetworkNode.removeTrack((TrackPiece) entity);

        } else if (entity instanceof StationImpl) {
            Station station = (Station) entity;
            station.dispose();
        }
    }

    @Override
    public void apply(Vector3fc position, int xSc, int ySc) {

    }
}
