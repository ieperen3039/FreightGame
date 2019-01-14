package NG.GameState;

import NG.Entities.Entity;
import NG.Entities.Goods;
import NG.Rendering.Shapes.Primitives.Collision;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Collections;
import java.util.List;

/**
 * A storage can be any building that stores {@link Goods}. This includes stations, industries and possibly a
 * rearranging terrain, but not a train.
 * @author Geert van Ieperen. Created on 12-11-2018.
 */
public abstract class Storage implements Entity {
    private List<Goods> goods;
    private Vector3f position;

    public List<Goods> getGoods() {
        return Collections.unmodifiableList(goods);
    }

    public Vector3fc getPosition() {
        return position;
    }

    @Override
    public UpdateFrequency getUpdateFrequency() {
        return UpdateFrequency.NEVER;
    }

    @Override
    public Collision getRayCollision(Vector3f origin, Vector3f direction) {
        return null;
    }
}
