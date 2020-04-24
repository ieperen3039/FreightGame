package NG.Entities;

import NG.Core.Game;
import NG.GUIMenu.Components.SFrame;
import NG.GameState.Storage;
import NG.Rendering.Shapes.Primitives.Collision;
import NG.Tools.Logger;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFW;

/**
 * @author Geert van Ieperen. Created on 12-11-2018.
 */
public abstract class Station extends Storage {
    private static int nr = 1;
    private final String className = this.getClass().getSimpleName() + " " + (nr++);

    protected Game game;
    protected String stationName = "X";

    /** the position and orientation of the station */
    protected float orientation = 0;
    /** whether this station has been placed down. */
    protected boolean isFixed = false;

    public Station(Game game, Vector2f position) {
        super(position);
        this.game = game;
    }

    public void setOrientation(float orientation) {
        if (isFixed) {
            Logger.ERROR.print("Tried changing state of a fixed station");
            return;
        }
        this.orientation = orientation;
    }

    public void fixPosition() {
        isFixed = true;
    }

    @Override
    public void setPosition(Vector2fc position) {
        if (isFixed) {
            Logger.ERROR.print("Tried changing state of a fixed station");
            return;
        }

        super.setPosition(position);
    }

    @Override
    public String toString() {
        return className + " : " + stationName;
    }

    @Override
    public Collision getRayCollision(Vector3fc origin, Vector3fc direction) {
        return null;
    }

    @Override
    public void onClick(int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            game.gui().addFrame(new StationUI());
        }
    }

    protected class StationUI extends SFrame {
        StationUI() {
            super(Station.this.toString(), 500, 300);

            // add buttons etc.
        }
    }

}
