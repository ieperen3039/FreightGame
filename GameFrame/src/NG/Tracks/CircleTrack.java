package NG.Tracks;

import NG.Core.Game;
import NG.DataStructures.Generic.PairList;
import NG.Network.RailNode;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shapes.GenericShapes;
import NG.Rendering.Shapes.Shape;
import NG.Resources.GeneratorResource;
import NG.Resources.Resource;
import NG.Settings.Settings;
import NG.Tools.Vectors;
import org.joml.Math;
import org.joml.*;

/**
 * @author Geert van Ieperen. Created on 18-9-2018.
 */
public class CircleTrack extends TrackPiece {
    private static final float EPSILON = 1 / 256f;
    public static final float MAX_RENDER_SIZE = 50f;
    private final RailNode startNode;
    private final RailNode endNode;

    /** middle of the circle describing the track */
    private final Vector3fc center;

    private final float radius;
    private final float startTheta;
    private final float angle;
    private final float endTheta;
    private final float heightDiff;

    private final Resource<Mesh> mesh;
    private final Resource<Mesh> clickBox;
    private final PairList<Shape, Matrix4fc> collisionShapes;

    /**
     * @param game           the current game instance
     * @param type           the type of track
     * @param startNode      the point A where this track should start
     * @param startDirection the direction in point A
     * @param endPosition    the point where this track should end
     */
    public CircleTrack(
            Game game, TrackType type, RailNode startNode, Vector3fc startDirection, Vector3fc endPosition
    ) {
        this(game, type, startNode, startDirection, endPosition, null, true);
    }

    /**
     * A circle track with known end direction
     * @param game           the current game instance
     * @param type           the type of track
     * @param startNode      point A where this track should start
     * @param startDirection the direction in point A
     * @param endNode        point B where this track should end
     */
    public CircleTrack(
            Game game, TrackType type, RailNode startNode, Vector3fc startDirection, RailNode endNode
    ) {
        this(game, type, startNode, startDirection, endNode.getPosition(), endNode, true);
    }

    private CircleTrack(
            Game game, TrackType type, RailNode startNode, Vector3fc startDirection, Vector3fc endPosition,
            RailNode optionalEndNode, boolean modifiable
    ) {
        super(game, type, modifiable);
        assert startNode.getPosition().distanceSquared(endPosition) > 0 : startNode;

        this.startNode = startNode;

        Vector3fc startPosition = startNode.getPosition();
        Vector2fc startPosFlat = new Vector2f(startPosition.x(), startPosition.y());
        Vector2fc endPosFlat = new Vector2f(endPosition.x(), endPosition.y());
        Vector2f startToCenter = new Vector2f(startDirection.y(), -startDirection.x()).normalize(); // perpendicular to startDirection
        Vector2f startToEnd = new Vector2f(endPosFlat).sub(startPosFlat);

        float dot = startToEnd.dot(startToCenter);
        if (dot < 0) { // center is on the wrong side of the direction
            startToCenter.negate();
            dot = -dot;
        }

        // derivation: see bottom
        radius = startToEnd.lengthSquared() / (2 * dot);
        startToCenter.normalize(radius);

        center = new Vector3f(startPosition).add(startToCenter.x, startToCenter.y, 0);

        // dotOfCross = sd.cross(ste).dot(Z)
        float dotOfCross = startDirection.x() * startToEnd.y - startDirection.y() * startToEnd.x;
        boolean isClockwise = dotOfCross < 0;

        Vector2fc vecToStart = startToCenter.negate();
        Vector2f vecToEnd = new Vector2f(endPosFlat).sub(center.x(), center.y());

        float absAngle = Vectors.angle(vecToStart, vecToEnd);
        angle = isClockwise ? -absAngle : absAngle;
        float arcTan = Vectors.arcTan(vecToStart);
        if (arcTan < 0) arcTan += 2 * Math.PI;

        startTheta = arcTan;
        endTheta = startTheta + angle;

        assert !Float.isNaN(radius) : startToEnd + " - " + startToCenter;
        assert !Float.isNaN(angle) : vecToStart + " - " + vecToEnd;

        heightDiff = endPosition.z() - startPosition.z();
        if (radius * angle > MAX_RENDER_SIZE) {
            Vector3f newDisplacement = new Vector3f(startToEnd.x, startToEnd.y, heightDiff).normalize(10);
            mesh = new GeneratorResource<>(() -> getType().generateStraight(newDisplacement), Mesh::dispose);
            clickBox = new GeneratorResource<>(() -> TrackType.clickBoxStraight(newDisplacement), Mesh::dispose);

        } else {
            mesh = new GeneratorResource<>(() -> getType().generateCircle(radius, angle, heightDiff), Mesh::dispose);
            clickBox = new GeneratorResource<>(() -> TrackType.clickBoxCircle(radius, angle, heightDiff), Mesh::dispose);
        }

        this.endNode = (optionalEndNode != null)
                ? optionalEndNode
                : new RailNode(game, endPosition, type, angleToDirection(endTheta));

        // calculate collision shapes
        collisionShapes = new PairList<>();
        int collisionResolution = (int) Math.max(getLength() / Settings.TRACK_COLLISION_BOX_LENGTH, angle / Math.toRadians(45)) + 1;

        Vector3fc oldPosition = startPosition;
        Vector3fc newPosition;
        Vector3f oldToNew = new Vector3f();

        for (int i = 0; i < collisionResolution; i++) {
            float fraction = (i + 1f) / collisionResolution;
            newPosition = getPositionFromFraction(fraction);
            oldToNew.set(newPosition).sub(oldPosition);

            Shape shape = GenericShapes.CUBE;
            Quaternionf rotation = Vectors.xTo(oldToNew);
            Matrix4f transformation = new Matrix4f()
                    .translate(oldPosition)
                    .rotate(rotation)
                    .scale(oldToNew.length(), Settings.TRACK_WIDTH, Settings.TRACK_HEIGHT_SPACE)
                    .scale(0.5f) // as we transform a 2x2x2 cube
                    .translate(1, 0, 1);

            collisionShapes.add(shape, transformation);

            oldPosition = newPosition;
        }

        for (TrackSupport s : getTrackSupports()) {
            game.state().addEntity(s);
        }
    }

    @Override
    protected void draw(SGL gl, boolean renderClickBox) {
        gl.pushMatrix();
        {
            gl.translate(center);
            gl.rotate(startTheta, 0, 0, 1);
            gl.render(renderClickBox ? clickBox.get() : mesh.get(), this);
        }
        gl.popMatrix();
    }

    @Override
    public float getLength() {
        float dx = Math.abs(radius * angle);
        float dy = heightDiff;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public float getMaximumSpeed() {
        return getType().getMaximumSpeed(radius);
    }

    @Override
    public Vector3f getPositionFromFraction(float fraction) {
        assert (fraction >= 0 && fraction <= 1) : fraction;

        float currentAngle = (fraction * angle) + startTheta;

        float dx = Math.cos(currentAngle) * radius;
        float dy = Math.sin(currentAngle) * radius;
        float dz = fraction * heightDiff;

        return new Vector3f(center).add(dx, dy, dz);
    }

    @Override
    public Vector3f getDirectionFromFraction(float fraction) {
        assert (fraction >= 0 && fraction <= 1) : fraction;

        float targetAngle = (fraction * angle) + startTheta;
        return angleToDirection(targetAngle);
    }

    private Vector3f angleToDirection(float absoluteAngle) {
        float dx = -Math.sin(absoluteAngle);
        float dy = Math.cos(absoluteAngle);
        float dz = heightDiff / Math.abs(radius * angle);

        if (!isClockwise()) {
            return new Vector3f(-dx, -dy, dz);
        } else {
            return new Vector3f(dx, dy, dz);
        }
    }

    @Override
    public float getFractionOfClosest(Vector3fc origin, Vector3fc direction) {
        float t = (center.z() - origin.z()) / direction.z();
        Vector3f rayPoint = new Vector3f(direction).mul(t).add(origin);
        Vector3f vecToPoint = rayPoint.sub(center);
        float currentAngle = Vectors.arcTan(new Vector2f(vecToPoint.x, vecToPoint.y));

        if (currentAngle > Math.max(startTheta, endTheta) + Math.PI / 2) {
            currentAngle -= (Math.PI * 2);
        }
        if (currentAngle < Math.min(startTheta, endTheta) - Math.PI / 2) {
            currentAngle += (Math.PI * 2);
        }

        assert !(currentAngle > Math.max(startTheta, endTheta) + Math.PI / 2);
        assert !(currentAngle < Math.min(startTheta, endTheta) - Math.PI / 2);

        // float currentAngle = (fraction * angle) + startTheta;
        return (currentAngle - startTheta) / angle;
    }

    public boolean isClockwise() {
        return startTheta < endTheta;
    }

    public RailNode getStartNode() {
        return startNode;
    }

    public RailNode getEndNode() {
        return endNode;
    }

    protected Mesh getMesh() {
        return mesh.get();
    }

    public Mesh getClickBox() {
        return clickBox.get();
    }

    @Override
    public PairList<Shape, Matrix4fc> getConvexCollisionShapes() {
        return collisionShapes;
    }

    @Override
    public String toString() {
        return "CircleTrack{center=" + Vectors.toString(center) + ", radius=" + radius + ", angle=" + angle + "}";
    }

    public float getRadius() {
        return radius;
    }

    @Override
    public void restoreFields(Game game) {
        super.restoreFields(game);
        startNode.restore(game);
        endNode.restore(game);
    }

    /** @see #getCircleDescription(Vector2fc, Vector2fc, Vector2fc) */
    public static Description getCircleDescription(Vector3fc startPos, Vector3fc startDir, Vector3fc endPos) {
        return getCircleDescription(
                new Vector2f(startPos.x(), startPos.y()),
                new Vector2f(startDir.x(), startDir.y()),
                new Vector2f(endPos.x(), endPos.y())
        );
    }

    /**
     * computes circle parameters in the same way as {@link #CircleTrack(Game, TrackType, RailNode, Vector3fc,
     * Vector3fc)}.
     * @param startPos point A
     * @param startDir the direction in point A
     * @param endPos   point B
     * @return a center, and radius that cuts A with direction startDir, and B. The absolute angle of the circle piece
     * is also given.
     */
    public static Description getCircleDescription(Vector2fc startPos, Vector2fc startDir, Vector2fc endPos) {
        Vector2f startToCenter = new Vector2f(startDir.y(), -startDir.x()).normalize();
        Vector2fc startToEnd = new Vector2f(endPos).sub(startPos);
        float dot = startToEnd.dot(startToCenter);
        if (dot < 0) { // center is on the wrong side of the direction
            startToCenter.negate();
            dot = -dot;
        }

        // derivation: see bottom
        float radius = startToEnd.lengthSquared() / (2 * dot);
        startToCenter.normalize(radius);

        Vector2fc center = new Vector2f(startPos).add(startToCenter);
        Vector2fc vecToStart = startToCenter.negate();
        Vector2fc vecToEnd = new Vector2f(endPos).sub(center);

        float absAngle = Vectors.angle(vecToStart, vecToEnd);
        if (startToEnd.dot(startDir) < 0) {
            absAngle = (float) (2 * Math.PI - absAngle);
        }
        return new Description(center, absAngle, radius);
    }

    public static float getCircleRadius(Vector2fc startPos, Vector2fc startDir, Vector2fc endPos) {
        Vector2f startToCenter = new Vector2f(startDir.y(), -startDir.x()).normalize();
        Vector2fc startToEnd = new Vector2f(endPos).sub(startPos);

        // derivation: see bottom
        float dot = Math.abs(startToEnd.dot(startToCenter));
        if (dot < 1 / 128f) return Float.POSITIVE_INFINITY;

        return startToEnd.lengthSquared() / (2 * dot);
    }

    public static class Description {
        public final Vector2fc center;
        public final float angle;
        public final float radius;

        public Description(Vector2fc center, float angle, float radius) {
            this.center = center;
            this.angle = angle;
            this.radius = radius;
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
