package NG.Tracks;

import NG.DataStructures.MatrixStack.SGL;
import NG.Engine.Game;
import org.joml.Vector2f;
import org.joml.Vector2fc;

/**
 * @author Geert van Ieperen. Created on 18-9-2018.
 */
public class StraightTrack implements TrackPiece {
    private Game game;
    private final TrackMod.TrackType type;

    private final Vector2fc startCoord;
    private final Vector2fc endCoord;
    private final Vector2fc direction;
    private final float length;

    public StraightTrack(Game game, TrackMod.TrackType type, Vector2fc begin, Vector2fc end) {
        this.game = game;
        this.type = type;
        startCoord = new Vector2f(begin);
        endCoord = new Vector2f(end);
        Vector2f diff = new Vector2f(end).sub(begin);
        this.length = diff.length();
        direction = diff.normalize();
    }

    @Override
    public void update() {

    }

    @Override
    public void draw(SGL gl) {
        type.drawStraight(gl, startCoord, direction, length, game.map());
    }

    @Override
    public void onClick(int button) {

    }


    @Override
    public Vector2fc getEndDirection() {
        return direction;
    }
}
