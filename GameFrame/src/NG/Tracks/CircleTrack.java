package NG.Tracks;

import NG.DataStructures.MatrixStack.SGL;
import NG.Engine.Game;
import NG.Tools.Vectors;
import org.joml.Vector2f;
import org.joml.Vector2fc;

/**
 * @author Geert van Ieperen. Created on 18-9-2018.
 */
public class CircleTrack implements TrackPiece {

    private final Game game;
    private final TrackMod.TrackType type;

    /** middle of the circle describing the track */
    private final Vector2fc center;

    private final float radius;
    private final boolean isClockwise;
    private final float firstTheta;
    private final float angle;

    public CircleTrack(
            Game game, TrackMod.TrackType type, Vector2fc startPosition, Vector2fc startDirection, Vector2fc endPoint
    ) {
        this.game = game;
        this.type = type;

        Vector2f startToCenter = new Vector2f(startDirection).perpendicular();
        Vector2f startToEnd = new Vector2f(endPoint).sub(startPosition);

        float dot = startToEnd.dot(startToCenter);
        if (dot < 0) { // center is on the wrong side of the direction
            startToCenter.negate();
            dot = -dot;
        }

        // derivation: see bottom
        radius = startToEnd.lengthSquared() / (2 * dot);
        startToCenter.normalize(radius);

        center = new Vector2f(startPosition).add(startToCenter);

        // dotOfCross = sd.cross(ste).dot(Z)
        float dotOfCross = startDirection.x() * startToEnd.y - startDirection.y() * startToEnd.x;
        isClockwise = dotOfCross > 0;

        Vector2fc vecToStart = startToCenter.negate();
        Vector2f vecToEnd = new Vector2f(endPoint).sub(center);

        angle = Vectors.angle(vecToStart, vecToEnd);
        firstTheta = Vectors.arcTan(vecToStart);
    }

    public float getRadius() {
        return radius;
    }

    public boolean isClockwise() {
        return isClockwise;
    }

    @Override
    public void update() {

    }

    @Override
    public void draw(SGL gl) {
        float lowerTheta = isClockwise ? firstTheta : firstTheta - angle;
        type.drawCircle(gl, center, radius, lowerTheta, angle, game.map());
    }

    @Override
    public void onClick(int button) {

    }

    @Override
    public Vector2fc getEndDirection() {
        float theta = isClockwise ? (firstTheta - angle) : (firstTheta + angle);
        return new Vector2f(-Vectors.sin(theta), Vectors.cos(theta));
    }
}

/* derivation of radius calculation
    toMid == startToCenter

    ||n * toMid|| == ||(startPos + n * toMid) - endpoint||
    ||n * toMid||^2 == ||startToEnd - n * toMid||^2
    ||n * toMid||^2 == (startToEnd - n * toMid)(startToEnd - n * toMid)
    ||n * toMid||^2 == (startToEnd - n * toMid)(startToEnd) - (startToEnd - n * toMid)(n * toMid)
    ||n * toMid||^2 == (startToEnd)(startToEnd) - (n * toMid)(startToEnd) - (startToEnd)(n * toMid) + (n * toMid)(n * toMid)
    ||n * toMid||^2 == ||startToEnd||^2 - 2*(n * toMid)(startToEnd) + ||n * toMid||^2
    ||startToEnd||^2 - 2n*(toMid)(startToEnd) == 0
    ||startToEnd||^2 == 2n*(toMid)(startToEnd)
    n == ||startToEnd||^2 / 2*dot
*/
