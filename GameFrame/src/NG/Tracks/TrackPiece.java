package NG.Tracks;

import NG.Core.AbstractGameObject;
import NG.Core.Coloring;
import NG.Core.Game;
import NG.DataStructures.Collision.ColliderEntity;
import NG.DataStructures.Generic.Color4f;
import NG.InputHandling.ClickShader;
import NG.InputHandling.KeyControl;
import NG.InputHandling.MouseTool.AbstractMouseTool;
import NG.Network.NetworkNode;
import NG.Network.RailNode;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Rendering.Shapes.GenericShapes;
import NG.Resources.GeneratorResource;
import NG.Resources.Resource;
import org.joml.AABBf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Geert van Ieperen. Created on 18-9-2018.
 */
public abstract class TrackPiece extends AbstractGameObject implements ColliderEntity {
    private static final Color4f OCCUPIED_COLOR = Color4f.GREY;
    protected final TrackType type;
    protected final boolean isModifiable;
    private Resource<AABBf> hitbox;

    protected double spawnTime = Double.NEGATIVE_INFINITY;
    protected double despawnTime = Double.POSITIVE_INFINITY;

    private boolean doRenderClickBox = false;
    private boolean isOccupied = false;
    private final Coloring coloring = new Coloring(Color4f.WHITE);

    // if any of these is occupied, this is occupied as well
    private List<TrackPiece> entangledTracks = new ArrayList<>();

    public TrackPiece(Game game, TrackType type, boolean modifiable) {
        super(game);
        this.type = type;
        isModifiable = modifiable;

        hitbox = new GeneratorResource<>(this::computeHitbox);
    }

    @Override
    public UpdateFrequency getUpdateFrequency() {
        return UpdateFrequency.NEVER;
    }

    public TrackType getType() {
        return type;
    }

    /**
     * @return the node at the position of getPositionFromFraction(0)
     */
    public abstract RailNode getStartNode();

    /**
     * @return the node at the position of getPositionFromFraction(1)
     */
    public abstract RailNode getEndNode();

    public RailNode getNot(RailNode node) {
        RailNode startNode = getStartNode();
        return node == startNode ? getEndNode() : startNode;
    }

    @Override
    public void update() {
        double gameTime = game.timer().getGameTime();
        this.doRenderClickBox = game.keyControl().isShiftPressed();

        entangledTracks.removeIf(t -> t.isDespawnedAt(gameTime));
    }

    @Override
    public void draw(SGL gl) {
        ShaderProgram shader = gl.getShader();

        if (shader instanceof MaterialShader) {
            type.setMaterial((MaterialShader) shader, this, coloring.getColor());
        }

        gl.pushMatrix();
        {
            boolean renderClickBox = doRenderClickBox || shader instanceof ClickShader;
            draw(gl, renderClickBox);
        }
        gl.popMatrix();

        if (game.settings().RENDER_COLLISION_BOX) {
            getConvexCollisionShapes().forEach((shape, transform) -> {
                gl.pushMatrix();
                gl.multiplyAffine(transform);
                for (Vector3fc point : shape.getPoints()) {
                    gl.pushMatrix();
                    gl.translate(point);
                    gl.scale(0.1f);
                    gl.render(GenericShapes.ICOSAHEDRON, this);
                    gl.popMatrix();
                }
                gl.popMatrix();
            });
        }
    }

    protected abstract void draw(SGL gl, boolean clickBox);

    @Override
    public AABBf getHitbox() {
        return hitbox.get();
    }

    @Override
    public void reactMouse(AbstractMouseTool.MouseAction action, KeyControl keys) {

    }

    public void setMarking(Coloring.Marking marking) {
        this.coloring.addMark(marking);
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

    public abstract float getFractionOfClosest(Vector3fc origin, Vector3fc direction);

    public abstract Vector3f getPositionFromFraction(float fraction);

    public abstract Vector3f getDirectionFromFraction(float fraction);

    public abstract float getLength();

    public boolean isStatic() {
        return !isModifiable;
    }

    public boolean isValid() {
        NetworkNode startNNode = getStartNode().getNetworkNode();
        NetworkNode endNNode = getEndNode().getNetworkNode();
        if (startNNode.getEntryOf(endNNode) == null || endNNode.getEntryOf(startNNode) == null) {
            throw new IllegalStateException(String.format("track %s has unconnected nodes %s and %s", this, startNNode, endNNode));
        }
        return true;
    }

    public void setOccupied(boolean occupied) {
        this.isOccupied = occupied;
        if (occupied) {
            coloring.addMark(OCCUPIED_COLOR, Coloring.Priority.OCCUPIED_TRACK);
        } else {
            coloring.removeMark(Coloring.Priority.OCCUPIED_TRACK);
        }
    }

    public boolean isOccupied() {
        if (isOccupied) return true;

        for (TrackPiece other : entangledTracks) {
            if (other.isOccupied) return true;
        }

        return false;
    }

    public abstract float getMaximumSpeed();

    public RailNode get(NetworkNode targetNode) {
        if (getStartNode().getNetworkNode().equals(targetNode)) return getStartNode();
        if (getEndNode().getNetworkNode().equals(targetNode)) return getEndNode();
        return null;
    }

    public boolean isConnectedTo(TrackPiece other) {
        return getStartNode() == other.getStartNode() || getStartNode() == other.getEndNode() ||
                getEndNode() == other.getStartNode() || getEndNode() == other.getEndNode();
    }

    public static void entangleTrackOccupation(TrackPiece track, TrackPiece trackPiece) {
        trackPiece.entangledTracks.add(track);
        track.entangledTracks.add(trackPiece);
    }
}
