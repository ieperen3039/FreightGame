package NG.Entities;

import NG.Engine.Game;
import NG.GameState.Storage;
import NG.Rendering.Shapes.Primitives.Collision;
import NG.Settings.Settings;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Geert van Ieperen. Created on 12-11-2018.
 */
public abstract class Station extends Storage {
    private Game game;

    /** the position and orientation of the station */
    protected Vector3f position = new Vector3f();
    protected float orientation = 0;
    /** whether this station has been placed down. */
    protected boolean isFixed = false;

    void init(Game game, Vector3fc position) {
        this.game = game;
        moveTo(position, 0);
    }

    public void moveTo(Vector3fc newPosition, float newOrientation) {
        position.set(newPosition);
        orientation = newOrientation;
    }

    @Override
    public void update() {
        List<Goods> reachableGoods = new ArrayList<>();
        List<Storage> elements = game.state().getIndustriesByRange(position, Settings.STATION_RANGE);
        for (Storage element : elements) {
            reachableGoods.addAll(element.getGoods());
        }

        //TODO
    }

    @Override
    public void onClick(int button) {

    }

    @Override
    public Collision getRayCollision(Vector3f origin, Vector3f direction) {
        return null;
    }

    @Override
    public Vector3fc getPosition() {
        return position;
    }
}
