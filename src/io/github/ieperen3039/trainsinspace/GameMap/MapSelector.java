package io.github.ieperen3039.trainsinspace.GameMap;

import io.github.ieperen3039.ngn.InputHandling.MouseTools.MouseSelector;
import io.github.ieperen3039.ngn.InputHandling.MouseTools.MouseTarget;
import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.InputHandling.MouseTool.MouseTargetMap;
import org.joml.Vector3f;
import org.joml.primitives.Rayf;

public class MapSelector implements MouseSelector {
    private Game game;
    private GameMap map;

    public MapSelector(Game game, GameMap map) {
        this.game = game;
        this.map = map;
    }

    @Override
    public MouseTarget pick(int xSc, int ySc) {
        Rayf ray = game.camera().unproject(xSc, ySc, game.getViewPort());

        float rayLength = game.settings().Z_FAR;
        Vector3f origin = new Vector3f(ray.oX, ray.oY, ray.oZ);
        Vector3f extent = new Vector3f(ray.dX, ray.dY, ray.dZ).normalize(rayLength);

        Float t = map.gridMapIntersection(origin, extent);
        if (t == null) return null;

        return new MouseTargetMap(extent.mul(t).add(origin));
    }
}
