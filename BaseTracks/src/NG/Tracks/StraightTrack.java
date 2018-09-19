package NG.Tracks;

import NG.DataStructures.MatrixStack.SGL;
import NG.Engine.Game;
import NG.GameState.GameState;
import NG.Tools.Toolbox;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;

/**
 * @author Geert van Ieperen. Created on 18-9-2018.
 */
public class StraightTrack implements TrackPiece {
    private final Vector2fc startCoord;
    private final Vector2fc endCoord;
    private final Vector2fc direction;
    private final float length;
    private Game game;

    public StraightTrack(Game game, Vector2fc begin, Vector2fc end) {
        this.game = game;
        startCoord = new Vector2f(begin);
        endCoord = new Vector2f(end);
        Vector2f diff = new Vector2f(end).sub(begin);
        this.length = diff.length();
        direction = diff.normalize();
    }

    /**
     * append a straight piece of track of the given length to the given piece of track
     * @param game   length of this game
     * @param parent the piece whose end to connect to
     * @param length length of the new piece
     */
    public StraightTrack(Game game, TrackPiece parent, float length) {
        Vector2fc begin = parent.getEndPosition();
        Vector2fc dir = parent.getEndDirection();
        Vector2f displace = new Vector2f(dir).mul(length);
        Vector2fc end = displace.add(begin);

        this.game = game;
        this.length = length;
        startCoord = new Vector2f(begin);
        endCoord = end;
        direction = dir;
    }

    @Override
    public Vector2fc getEndPosition() {
        return endCoord;
    }

    @Override
    public Vector2fc getStartPosition() {
        return startCoord;
    }

    @Override
    public Vector2fc getStartDirection() {
        return direction;
    }

    @Override
    public Vector2fc getEndDirection() {
        return direction;
    }

    @Override
    public void update() {

    }

    @Override
    public void draw(SGL gl) {
        GameState gamestate = game.getGamestate();
        Vector3f coord = gamestate.getPosition(startCoord);

        float trackSpacing = game.settings().TRACK_SPACING;
        float pieces = trackSpacing * length;

        for (int i = 0; i < pieces; i++) {
            gl.pushMatrix();
            {
                Vector2f diff = new Vector2f(direction).mul(trackSpacing * i);
                diff.add(startCoord);
                gl.translate(coord);
                Toolbox.drawAxisFrame(gl);
            }
            gl.popMatrix();
        }
    }
}
