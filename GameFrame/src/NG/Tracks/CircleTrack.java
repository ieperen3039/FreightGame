package NG.Tracks;

import NG.Engine.Game;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shapes.Primitives.Collision;
import NG.Tools.Vectors;
import org.joml.Math;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;

import static NG.Tools.Vectors.cos;
import static NG.Tools.Vectors.sin;
import static java.lang.Math.abs;

/**
 * @author Geert van Ieperen. Created on 18-9-2018.
 */
public class CircleTrack implements TrackPiece {

    private final Game game;
    private final TrackMod.TrackType type;
    private final NetworkNodePoint startPoint;
    private final NetworkNodePoint endPoint;

    /** middle of the circle describing the track */
    private final Vector2fc center;

    private final float radius;
    private final boolean isClockwise;
    private final float startTheta;
    private final float angle;
    private final float endTheta;

    /**
     * @param game           the current game instance
     * @param type           the type of track
     * @param startPoint     a point A on the map
     * @param startDirection the direction in point A
     * @param endPoint       another point on the map, different from A.
     */
    public CircleTrack(
            Game game, TrackMod.TrackType type, NetworkNodePoint startPoint, Vector2fc startDirection,
            NetworkNodePoint endPoint
    ) {
        this.game = game;
        this.type = type;
        this.startPoint = startPoint;
        this.endPoint = endPoint;

        Vector2fc startPosition = startPoint.getPosition();
        Vector2fc endPosition = endPoint.getPosition();
        Vector2f startToCenter = new Vector2f(startDirection).perpendicular();
        Vector2f startToEnd = new Vector2f(endPosition).sub(startPosition);

        float dot = startToEnd.dot(startToCenter);
        if (dot < 0) { // center is on the wrong side of the direction
            startToCenter.negate();
            dot = startToEnd.dot(startToCenter);
        }

        // derivation: see bottom
        radius = startToEnd.lengthSquared() / (2 * dot);
        startToCenter.normalize(radius);

        center = new Vector2f(startPosition).add(startToCenter);

        // dotOfCross = sd.cross(ste).dot(Z)
        float dotOfCross = startDirection.x() * startToEnd.y - startDirection.y() * startToEnd.x;
        isClockwise = dotOfCross > 0;

        Vector2fc vecToStart = startToCenter.negate();
        Vector2f vecToEnd = new Vector2f(endPosition).sub(center);

        angle = Vectors.angle(vecToStart, vecToEnd);
        startTheta = Vectors.arcTan(vecToStart);
        endTheta = !isClockwise ? (startTheta - angle) : (startTheta + angle);
    }

    @Override
    public void update() {

    }

    @Override
    public void draw(SGL gl) {
        float lowerTheta = Math.min(startTheta, endTheta);
        type.drawCircle(gl, center, radius, lowerTheta, angle, game.map());
    }

    @Override
    public void onClick(int button) {

    }

    @Override
    public Collision getRayCollision(Vector3f origin, Vector3f direction) {
        Vector3f position = game.map().intersectWithRay(origin, direction);

        float distanceToCenter = center.distance(position.x, position.y);
        int clickWidth = game.settings().TRACK_CLICK_WIDTH;
        if (abs(distanceToCenter - radius) > clickWidth) return null;

        return new Collision(position);
    }

    public Vector2f distanceToPosition(float distanceFromStart) {
        float angleTravelled = (float) (distanceFromStart / (radius * 2 * Math.PI));
        float currentAngle = angleTravelled + startTheta;

        if (currentAngle < 0 || currentAngle > angle) {
            return null;
        }

        return angleToPosition(currentAngle);
    }

    protected Vector2f angleToPosition(float currentAngle) {
        Vector2f offset = new Vector2f(cos(currentAngle), sin(currentAngle));
        offset.mul(radius);
        return new Vector2f(center).add(offset);
    }

    @Override
    public Vector2f getStartDirection() {
        return getDirectionOf(startTheta).negate();
    }

    @Override
    public Vector2f closestPointOf(Vector2fc position) {
        Vector2f relative = new Vector2f(position).sub(center);
        return relative.normalize(radius).add(center);
    }

    @Override
    public Vector2f getEndDirection() {
        return getDirectionOf(endTheta);
    }

    protected Vector2f getDirectionOf(float theta) {
        Vector2f derivative = new Vector2f(-sin(theta), cos(theta));
        if (!isClockwise) derivative.negate();
        return derivative;
    }

    @Override
    public NetworkNodePoint getStartNodePoint() {
        return startPoint;
    }

    @Override
    public NetworkNodePoint getEndNodePoint() {
        return endPoint;
    }

    /** check whether the state of this object is correct (test method) */
    void testAssumptions(Vector2fc startDirection) {
        if (startPoint.getPosition().distance(angleToPosition(startTheta)) > 0.001f) {
            throw new IllegalStateException("calculated start position is not equal to the start point: " +
                    Vectors.toString(startPoint.getPosition()) + " != " + Vectors.toString(angleToPosition(startTheta)));
        }
        if (endPoint.getPosition().distance(angleToPosition(endTheta)) > 0.001f) {
            throw new IllegalStateException("calculated end position is not equal to the end point: " +
                    Vectors.toString(endPoint.getPosition()) + " != " + Vectors.toString(angleToPosition(endTheta)));
        }
        if (startDirection.angle(getStartDirection().negate()) > 0.001f) {
            Vector2f simulatedStartDirection = startDirection.negate(new Vector2f());
            throw new IllegalStateException("calculated start direction is not equal to the given start direction: " +
                    Vectors.toString(simulatedStartDirection) + " != " + Vectors.toString(getStartDirection()));
        }
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
