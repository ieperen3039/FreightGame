package NG.Tracks;

import NG.Engine.Game;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shapes.Primitives.Collision;
import NG.Tools.Vectors;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;

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

    private boolean isInvalid;

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
        gl.pushMatrix();
        {
            type.drawStraight(gl, startCoord, direction, length, game.map());
        }
        gl.popMatrix();
    }

    @Override
    public void onClick(int button) {

    }

    @Override
    public void dispose() {
        isInvalid = true;
    }

    @Override
    public boolean isDisposed() {
        return isInvalid;
    }

    @Override
    public Collision getRayCollision(Vector3f origin, Vector3f direction) {
        Vector3f position = game.map().intersectWithRay(origin, direction);
        Vector2fc coordinate = new Vector2f(position.x, position.y);
        int clickWidth = game.settings().TRACK_CLICK_WIDTH;
        float offset = coordinate.distance(closestPointOf(coordinate));
        if (offset > clickWidth) return null;
        return new Collision(position);
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
