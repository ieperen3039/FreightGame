package NG.Tracks;

import NG.Engine.Game;
import NG.Rendering.MatrixStack.SGL;
import NG.Tools.Vectors;
import org.joml.Vector2f;
import org.joml.Vector2fc;

/**
 * @author Geert van Ieperen. Created on 18-9-2018.
 */
public class StraightTrack implements TrackPiece {
    private Game game;
    private final TrackMod.TrackType type;

    private final Vector2fc startCoord;
    private final NetworkNodePoint startNode;
    private final NetworkNodePoint endNode;
    private final Vector2fc direction;
    private final float length;

    public StraightTrack(Game game, TrackMod.TrackType type, NetworkNodePoint startNode, NetworkNodePoint endNode) {
        this.game = game;
        this.type = type;
        startCoord = new Vector2f(startNode.getPosition());
        this.startNode = startNode;
        this.endNode = endNode;
        Vector2f diff = new Vector2f(endNode.getPosition()).sub(startCoord);
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

    @Override
    public Vector2fc getStartDirection() {
        return direction;
    }

    @Override
    public Vector2fc getDirectionOf(NetworkNodePoint nodePoint) {
        return direction;
    }

    @Override
    public Vector2f closestPointOf(Vector2fc position) {
        Vector2fc end = endNode.getPosition();
        return Vectors.getIntersectionPointLine(position, startCoord, end);
    }

    @Override
    public NetworkNodePoint getStartNodePoint() {
        return startNode;
    }

    @Override
    public NetworkNodePoint getEndNodePoint() {
        return endNode;
    }
}
