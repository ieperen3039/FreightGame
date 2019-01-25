package NG.Entities;

import NG.Engine.Game;
import NG.GameState.Storage;
import NG.Rendering.Shapes.Primitives.Collision;
import NG.ScreenOverlay.Frames.Components.SFrame;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen. Created on 12-11-2018.
 */
public abstract class Station extends Storage {
    private final String className = this.getClass().getSimpleName();

    private Game game;

    /** the position and orientation of the station */
    protected float orientation = 0;
    /** whether this station has been placed down. */
    protected boolean isFixed = false;
    private String stationName = "Multicast";

    public void init(Game game, Vector3fc position) {
        this.game = game;
        setPosition(position);
    }

    public void setOrientation(float orientation) {
        this.orientation = orientation;
    }

    @Override
    public void update() {

    }

    @Override
    public void onClick(int button) {

    }

    public void fixPosition() {
        isFixed = true;
    }

    @Override
    public String toString() {
        return className + " " + stationName;
    }

    @Override
    public Collision getRayCollision(Vector3f origin, Vector3f direction) {
        return null;
    }

    private class StationUI extends SFrame {
        public StationUI() {
            super(Station.this.toString(), 500, 300);

            // add buttons etc.
        }
    }
}
