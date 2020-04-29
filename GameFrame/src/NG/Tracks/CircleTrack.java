package NG.Tracks;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.InputHandling.ClickShader;
import NG.Network.NetworkNode;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Resources.GeneratorResource;
import NG.Resources.Resource;
import NG.Tools.Vectors;
import org.joml.Math;
import org.joml.*;

/**
 * @author Geert van Ieperen. Created on 18-9-2018.
 */
public class CircleTrack extends AbstractGameObject implements TrackPiece {
    private static final float EPSILON = 1 / 256f;
    private final TrackType type;
    private final NetworkNode startNode;
    private final NetworkNode endNode;

    /** middle of the circle describing the track */
    private final Vector3fc center;

    private final float radius;
    private final float startTheta;
    private final float angle;
    private final float endTheta;

    private final Resource<Mesh> mesh;
    private final Resource<Mesh> clickBox;

    private boolean isInvalid = false;
    private final float heightDiff;
    private boolean renderClickBox = false;

    /**
     * @param game           the current game instance
     * @param type           the type of track
     * @param startNode      the point A where this track should start
     * @param startDirection the direction in point A
     * @param endPosition    the point where this track should end
     */
    public CircleTrack(
            Game game, TrackType type, NetworkNode startNode, Vector3fc startDirection, Vector3fc endPosition
    ) {
        this(game, type, startNode, startDirection, endPosition, null);
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
            Game game, TrackType type, NetworkNode startNode, Vector3fc startDirection, NetworkNode endNode
    ) {
        this(game, type, startNode, startDirection, endNode.getPosition(), endNode);
    }

    private CircleTrack(
            Game game, TrackType type, NetworkNode startNode, Vector3fc startDirection, Vector3fc endPosition,
            NetworkNode optionalEndNode
    ) {
        super(game);
        this.type = type;
        this.startNode = startNode;

        Vector3fc startPosition = startNode.getPosition();
        Vector2fc startPosFlat = new Vector2f(startPosition.x(), startPosition.y());
        Vector2fc endPosFlat = new Vector2f(endPosition.x(), endPosition.y());
        Vector2f startToCenter = new Vector2f(startDirection.y(), -startDirection.x()); // perpendicular to startDirection
        Vector2f startToEnd = new Vector2f(endPosFlat).sub(startPosFlat);

        float dot = startToEnd.dot(startToCenter);
        if (dot < 0) { // center is on the wrong side of the direction
            startToCenter.negate();
            dot = -dot;
        }

        // derivation: see bottom
        radius = startToEnd.lengthSquared() / (2 * dot);
        startToCenter.normalize(radius);

        center = new Vector3f(startPosFlat, startPosition.z()).add(startToCenter.x, startToCenter.y, 0);

        // dotOfCross = sd.cross(ste).dot(Z)
        float dotOfCross = startDirection.x() * startToEnd.y - startDirection.y() * startToEnd.x;
        boolean isClockwise = dotOfCross < 0;

        Vector2fc vecToStart = startToCenter.negate();
        Vector2f vecToEnd = new Vector2f(endPosFlat).sub(center.x(), center.y());

        float absAngle = Vectors.angle(vecToStart, vecToEnd);
        angle = isClockwise ? -absAngle : absAngle;
        startTheta = Vectors.arcTan(vecToStart);
        endTheta = startTheta + angle;

        heightDiff = endPosition.z() - startPosition.z();
        mesh = new GeneratorResource<>(() -> type.generateCircle(radius, angle, heightDiff), Mesh::dispose);
        clickBox = new GeneratorResource<>(() -> TrackType.clickBoxCircle(radius, angle, heightDiff), Mesh::dispose);

        this.endNode = (optionalEndNode != null) ? optionalEndNode : new NetworkNode(endPosition, type, getEndDirection());
        startNode.addNode(this.endNode, this);
        this.endNode.addNode(startNode, this);
    }

    @Override
    public void update() {
        doRenderClickBox(game.keyControl().isShiftPressed());
    }

    @Override
    public void draw(SGL gl) {
        ShaderProgram shader = gl.getShader();
        Mesh meshToRender;

        if (renderClickBox || shader instanceof ClickShader) {
            meshToRender = clickBox.get();

        } else {
            meshToRender = mesh.get();
        }

        if (shader instanceof MaterialShader) {
            type.setMaterial((MaterialShader) shader);
        }

        gl.pushMatrix();
        {
            gl.translate(center);
            gl.rotate(startTheta, 0, 0, 1);
            gl.render(meshToRender, this);
        }
        gl.popMatrix();
    }

    @Override
    public void onClick(int button) {

    }

    @Override
    public Vector3f getPositionFromDistance(float distanceFromStart) {
        float length = Math.abs(radius * angle);
        float fraction = distanceFromStart / length;

        if (fraction < 0 || fraction > 1) return null;
        return getPositionFromFraction(fraction);
    }

    private Vector3f getPositionFromFraction(float fraction) {
        float currentAngle = (fraction * angle) + startTheta;

        float dx = Math.cos(currentAngle) * radius;
        float dy = Math.sin(currentAngle) * radius;
        float dz = fraction * heightDiff;
        return new Vector3f(center).add(dx, dy, dz);
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
    public Vector3f closestPointOf(Vector3fc origin, Vector3fc direction) {
        float t = Intersectionf.intersectRayPlane(origin, direction, center, Vectors.Z, EPSILON);
        Vector3f rayPoint = new Vector3f(direction).mul(t).add(origin);
        Vector3f vecToPoint = rayPoint.sub(center);
        float currentAngle = Vectors.arcTan(new Vector2f(vecToPoint.x, vecToPoint.y));
        // float currentAngle = (fraction * angle) + startTheta;
        float fraction = (currentAngle - startTheta) / angle;
        float dx = Math.cos(currentAngle) * radius;
        float dy = Math.sin(currentAngle) * radius;
        float dz = fraction * heightDiff;
        return new Vector3f(center).add(dx, dy, dz);
    }

    public boolean isClockwise() {
        return startTheta < endTheta;
    }

    @Override
    public TrackType getType() {
        return type;
    }

    public NetworkNode getStartNode() {
        return startNode;
    }

    public NetworkNode getEndNode() {
        return endNode;
    }

    @Override
    public Vector3fc getStartDirection() {
        float dx = -Math.sin(startTheta);
        float dy = Math.cos(startTheta);
        float dz = heightDiff / (radius * angle);

        if (isClockwise()) {
            return new Vector3f(-dx, -dy, dz);
        } else {
            return new Vector3f(dx, dy, dz);
        }
    }

    @Override
    public Vector3fc getEndDirection() {
        float dx = -Math.sin(endTheta);
        float dy = Math.cos(endTheta);
        float dz = heightDiff / (radius * angle);

        if (isClockwise()) { // opposite of getStartDirection
            return new Vector3f(dx, dy, dz);
        } else {
            return new Vector3f(-dx, -dy, dz);
        }
    }

    @Override
    public void doRenderClickBox(boolean renderClickBox) {
        this.renderClickBox = renderClickBox;
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
