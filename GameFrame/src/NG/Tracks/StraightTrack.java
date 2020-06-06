package NG.Tracks;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.InputHandling.ClickShader;
import NG.InputHandling.MouseTools.AbstractMouseTool;
import NG.Network.RailNode;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Resources.GeneratorResource;
import NG.Resources.Resource;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen. Created on 18-9-2018.
 */
public class StraightTrack extends AbstractGameObject implements TrackPiece {
    private final TrackType type;

    private final RailNode startNode;
    private final RailNode endNode;

    private final Vector3fc direction;
    private final float length;
    private final boolean isModifiable;

    protected double spawnTime = Double.NEGATIVE_INFINITY;
    protected double despawnTime = Double.POSITIVE_INFINITY;

    private final Resource<Mesh> mesh;
    private final Resource<Mesh> clickBox;
    private boolean renderClickBox = false;
    private boolean isOccupied = false;

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
        super(game);

        this.type = type;
        this.startNode = startNode;
        Vector3fc displacement = new Vector3f(endNodePosition).sub(startNode.getPosition());
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
        isModifiable = modifiable;

        assert check(startNode, this.endNode, direction);
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
    public void update() {
        this.renderClickBox = game.keyControl().isShiftPressed();
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
            gl.translate(startNode.getPosition());
            gl.render(meshToRender, this);
        }
        gl.popMatrix();
    }

    @Override
    public void reactMouse(AbstractMouseTool.MouseAction action) {

    }

    @Override
    public void despawn(double gameTime) {
        despawnTime = gameTime;
    }

    @Override
    public double getSpawnTime() {
        return spawnTime;
    }

    @Override
    public double getDespawnTime() {
        return despawnTime;
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

    @Override
    public TrackType getType() {
        return type;
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
    public float getLength() {
        return length;
    }

    @Override
    public boolean isStatic() {
        return !isModifiable;
    }

    @Override
    public void setOccupied(boolean occupied) {
        this.isOccupied = occupied;
    }

    @Override
    public boolean isOccupied() {
        return isOccupied;
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
