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
import org.joml.*;

import java.lang.Math;

/**
 * @author Geert van Ieperen. Created on 18-9-2018.
 */
public class StraightTrack extends TrackPiece {
    private final RailNode startNode;
    private final RailNode endNode;

    private final Vector3fc direction;
    private final float length;

    protected final Resource<Mesh> mesh;
    protected final Resource<Mesh> clickBox;
    private PairList<Shape, Matrix4fc> collisionShapes;

    /**
     * create a straight piece of track based on an initial node and an endposition. A new node is generated, and is
     * accessible as {@link #getEndNode()}. The connection is automatically registered.
     * @param type            type of the track
     * @param startNode       a node this should connect to.
     * @param endNodePosition the position of the new end node
     * @param modifiable      sets the canBeModified flag
     */
    public StraightTrack(
            Game game, TrackType type, RailNode startNode, Vector3fc endNodePosition, boolean modifiable
    ) {
        this(game, type, startNode, null, endNodePosition, modifiable);
    }

    /**
     * create a new straight piece of track between the two given nodes. The connection is automatically registered.
     * @param type       type of the track
     * @param startNode  one node to connect
     * @param endNode    another no to connect
     * @param modifiable sets the canBeModified flag
     */
    public StraightTrack(
            Game game, TrackType type, RailNode startNode, RailNode endNode, boolean modifiable
    ) {
        this(game, type, startNode, endNode, endNode.getPosition(), modifiable);
    }

    /**
     * create a new straight piece of track between the two given nodes. The connection is automatically registered.
     * @param type            type of the track
     * @param startNode       one node to connect
     * @param endNode         another no to connect
     * @param endNodePosition
     * @param modifiable      sets the canBeModified flag
     */
    public StraightTrack(
            Game game, TrackType type, RailNode startNode, RailNode endNode, Vector3fc endNodePosition,
            boolean modifiable
    ) {
        super(game, type, modifiable);

        this.startNode = startNode;
        Vector3fc startNodePosition = startNode.getPosition();
        Vector3fc displacement = new Vector3f(endNodePosition).sub(startNodePosition);
        this.length = displacement.length();
        this.direction = new Vector3f(displacement).div(length);
        this.endNode = endNode != null ? endNode : new RailNode(endNodePosition, type, direction);

        if (length > CircleTrack.MAX_RENDER_SIZE) {
            Vector3f newDisplacement = new Vector3f(direction).mul(10);
            this.mesh = new GeneratorResource<>(() -> type.generateStraight(newDisplacement), Mesh::dispose);
            this.clickBox = new GeneratorResource<>(() -> TrackType.clickBoxStraight(newDisplacement), Mesh::dispose);

        } else {
            this.mesh = new GeneratorResource<>(() -> type.generateStraight(displacement), Mesh::dispose);
            this.clickBox = new GeneratorResource<>(() -> TrackType.clickBoxStraight(displacement), Mesh::dispose);
        }

        collisionShapes = new PairList<>();

        Shape shape = GenericShapes.CUBE;
        Quaternionf rotation = Vectors.xTo(displacement);
        Matrix4f transformation = new Matrix4f()
                .translate(startNodePosition)
                .rotate(rotation)
                .scale(length, Settings.TRACK_WIDTH, Settings.TRACK_HEIGHT_SPACE)
                .scale(0.5f) // as we transform a 2x2x2 cube
                .translate(1, 0, 1);
        collisionShapes.add(shape, transformation);

        assert check(startNode, this.endNode, direction);
    }

    @Override
    public float getMaximumSpeed() {
        return type.getMaximumSpeed();
    }

    @Override
    public PairList<Shape, Matrix4fc> getConvexCollisionShapes() {
        return collisionShapes;
    }

    private static boolean check(RailNode startNode, RailNode endNode, Vector3fc direction) {
        Vector3fc ste = startNode.getDirectionTo(endNode.getPosition());
        float angleSTE = new Vector3f(ste.x(), ste.y(), 0).normalize()
                .angle(new Vector3f(direction.x(), direction.y(), 0).normalize());
        if (angleSTE > Math.toRadians(1)) throw new IllegalArgumentException(String.valueOf(Math.toDegrees(angleSTE)));

        Vector3fc ets = endNode.getDirectionTo(startNode.getPosition());
        float angleETS = new Vector3f(ets.x(), ets.y(), 0).normalize()
                .angle(new Vector3f(direction.x(), direction.y(), 0).normalize());
        if (angleETS < Math.toRadians(179)) {
            throw new IllegalArgumentException(String.valueOf(Math.toDegrees(angleETS)));
        }

        return true;
    }

    @Override
    protected void draw(SGL gl, boolean renderClickBox) {
        gl.pushMatrix();
        {
            gl.translate(startNode.getPosition());
            gl.render(renderClickBox ? clickBox.get() : mesh.get(), this);
        }
        gl.popMatrix();
    }

    @Override
    public float getFractionOfClosest(Vector3fc origin, Vector3fc direction) {
        // https://en.wikipedia.org/wiki/Skew_lines#Nearest_Points
        // v1 = this, v2 = (origin, direction)
        Vector3fc nDirection = new Vector3f(direction).normalize();
        Vector3fc cross = new Vector3f(this.direction).cross(nDirection); // n
        Vector3f n2Cross = new Vector3f(nDirection).cross(cross); // n2
        Vector3fc thisOrigin = startNode.getPosition();

        float scalar = (new Vector3f(origin).sub(thisOrigin).dot(n2Cross)) / (this.direction.dot(n2Cross));
        return scalar / length;
    }

    public float getLength() {
        return length;
    }

    protected Mesh getMesh() {
        return mesh.get();
    }

    public Mesh getClickBox() {
        return clickBox.get();
    }

    @Override
    public RailNode getStartNode() {
        return startNode;
    }

    @Override
    public RailNode getEndNode() {
        return endNode;
    }

    @Override
    public Vector3f getDirectionFromFraction(float fraction) {
        return new Vector3f(direction);
    }

    @Override
    public Vector3f getPositionFromFraction(float fraction) {
        return new Vector3f(direction)
                .mul(fraction * length)
                .add(startNode.getPosition());
    }

    @Override
    public String toString() {
        return "StraightTrack{startNode=" + startNode + ", endNode=" + endNode + ", length=" + length + "}";
    }
}
