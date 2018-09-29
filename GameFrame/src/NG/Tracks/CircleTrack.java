package NG.Tracks;

import NG.DataStructures.MatrixStack.SGL;
import NG.Engine.Game;
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
    /** polar coordinates of the track piece */
    private final float startRadian;
    private final float endRadian;
    private final float radius;

    public CircleTrack(Game game, Vector2fc startPosition, Vector2fc startDirection, Vector2fc endPoint, TrackMod.TrackType type) {
        this.game = game;
        this.type = type;

        Vector2f startToCenter = new Vector2f(startDirection).perpendicular();
        Vector2f startToEnd = new Vector2f(endPoint).sub(startPosition);
        float dot = startToEnd.dot(startToCenter);
        if (dot < 0) startToCenter.negate();

        // derivation: see bottom
        radius = startToEnd.lengthSquared() / (2 * dot);

        startToCenter.mul(radius);
        center = new Vector2f(startPosition).add(startToCenter);
        startToCenter.negate();
        startRadian = (float) Math.atan2(startToCenter.y, startToCenter.x);
        Vector2f midToCenter = new Vector2f(center).sub(endPoint);
        endRadian = (float) Math.atan2(midToCenter.y, midToCenter.x);
    }

    @Override
    public Vector2fc getEndPosition() {
        return new Vector2f(center)
                .add((float) Math.sin(endRadian), (float) Math.cos(endRadian))
                .mul(radius);
    }

    @Override
    public Vector2fc getStartPosition() {
        return new Vector2f(center)
                .add((float) Math.sin(startRadian), (float) Math.cos(startRadian))
                .mul(radius);
    }

    @Override
    public Vector2fc getStartDirection() {
        return null;
    }

    @Override
    public Vector2fc getEndDirection() {
        return null;
    }

    public float getRadius() {
        return radius;
    }

    @Override
    public void update() {

    }

    @Override
    public void draw(SGL gl) {
        type.drawCircle(gl, center, radius, startRadian, endRadian, game.map());
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