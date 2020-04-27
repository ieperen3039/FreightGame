package NG.Tracks;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.Network.NetworkNode;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MeshLoading.Mesh;
import NG.Resources.GeneratorResource;
import NG.Resources.Resource;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen. Created on 18-9-2018.
 */
public class StraightTrack extends AbstractGameObject implements TrackPiece {
    private final TrackType type;

    private final NetworkNode startNode;
    private final NetworkNode endNode;

    private final Vector3fc direction;

    private boolean isInvalid;
    private final Resource<Mesh> mesh;

    /**
     * create a straight piece of track based on an initial node and an endposition. A new node is generated, and is
     * accessible as {@link #getEndNode()}
     * @param type            type of the track
     * @param startNode       a node this should connect to. The connection is automatically registered.
     * @param endNodePosition the position of the new end node
     */
    public StraightTrack(
            Game game, TrackType type, NetworkNode startNode, Vector3fc endNodePosition
    ) {
        super(game);
        this.type = type;
        this.startNode = startNode;
        Vector3fc displacement = new Vector3f(endNodePosition).sub(startNode.getPosition());
        this.mesh = new GeneratorResource<>(() -> type.generateStraight(displacement), Mesh::dispose);
        this.direction = new Vector3f(displacement).normalize();
        this.endNode = new NetworkNode(endNodePosition, type, direction);

        startNode.addNode(endNode, this);
        endNode.addNode(startNode, this);
    }

    /**
     * create a new straight piece of track between the two given nodes.
     * @param type      type of the track
     * @param startNode
     * @param endNode
     */
    public StraightTrack(
            Game game, TrackType type, NetworkNode startNode, NetworkNode endNode
    ) {
        super(game);
        this.type = type;
        this.startNode = startNode;
        Vector3fc displacement = new Vector3f(endNode.getPosition()).sub(startNode.getPosition());
        this.mesh = new GeneratorResource<>(() -> type.generateStraight(displacement), Mesh::dispose);
        this.direction = new Vector3f(displacement).normalize();
        this.endNode = endNode;

        startNode.addNode(endNode, this);
        endNode.addNode(startNode, this);
    }

    @Override
    public void update() {

    }

    @Override
    public void draw(SGL gl) {
        gl.pushMatrix();
        {
            gl.translate(startNode.getPosition());
            gl.render(mesh.get(), this);
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
    public Vector3f closestPointOf(Vector3fc origin, Vector3fc direction) {
        // https://en.wikipedia.org/wiki/Skew_lines#Nearest_Points
        // v1 = this, v2 = (origin, direction)
        Vector3fc cross = new Vector3f(this.direction).cross(direction); // n
        Vector3f n2Cross = new Vector3f(direction).cross(cross); // n2
        Vector3fc thisOrigin = startNode.getPosition();

        float scalar = (new Vector3f(origin).sub(thisOrigin).dot(n2Cross)) / (this.direction.dot(n2Cross));
        return new Vector3f(this.direction).mul(scalar).add(thisOrigin);
    }

    @Override
    public TrackType getType() {
        return type;
    }

    @Override
    public NetworkNode getStartNode() {
        return startNode;
    }

    @Override
    public NetworkNode getEndNode() {
        return endNode;
    }

    @Override
    public Vector3fc getEndDirection() {
        return direction;
    }

    @Override
    public Vector3fc getStartDirection() {
        return new Vector3f(direction).negate();
    }

    @Override
    public Vector3f getPositionFromDistance(float distanceFromStart) {
        return new Vector3f(direction)
                .mul(distanceFromStart)
                .add(startNode.getPosition());
    }
}
