package NG.Tracks;

import NG.Core.Coloring;
import NG.Core.Game;
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

import static org.lwjgl.opengl.GL11.glDepthMask;

/**
 * @author Geert van Ieperen. Created on 18-9-2018.
 */
public abstract class TrackPiece extends TrackElement {
    private static final Color4f OCCUPIED_COLOR = Color4f.GREY;

    private Resource<AABBf> hitbox;
    protected final boolean isModifiable;

    private boolean doRenderClickBox = false;
    private boolean isOccupied = false;

    // if any of these is occupied, this is occupied as well (needs no restoring)
    private List<TrackPiece> entangledTracks = new ArrayList<>();

    public TrackPiece(Game game, TrackType type, boolean modifiable) {
        super(game, type);
        this.isModifiable = modifiable;

        hitbox = new GeneratorResource<>(this::computeHitbox);
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
        this.doRenderClickBox = game.keyControl().isAltPressed();

        entangledTracks.removeIf(t -> t.isDespawnedAt(gameTime));
    }

    @Override
    public void draw(SGL gl) {
        ShaderProgram shader = gl.getShader();

        if (shader instanceof MaterialShader) {
            type.setMaterial((MaterialShader) shader, this, coloring.getColor());
        }

        boolean renderClickBox = doRenderClickBox || shader instanceof ClickShader;
        draw(gl, renderClickBox);

        if (game.settings().RENDER_COLLISION_BOX) {
            getConvexCollisionShapes().forEach((s, m) -> {
                gl.pushMatrix();
                {
                    glDepthMask(false); // read but not write
                    Color4f reddish = new Color4f(0.5f, 0, 0, 0.2f);
                    MaterialShader.ifPresent(gl, mat -> mat.setMaterial(reddish, Color4f.BLACK, 1));
                    gl.multiply(m);
                    gl.render(GenericShapes.CUBE, this);
                    glDepthMask(true);
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

    /**
     * generates and returns all supports necessary for this piece of track, except for the begin and end. These
     * supports are not yet added to the game state
     */
    protected List<TrackSupport> getTrackSupports() {
        TrackType type = getType();
        float trackLength = getLength();
        int nrSupports = (int) (trackLength / type.getMaxSupportLength()) + 1;
        List<TrackSupport> list = new ArrayList<>();

        for (int i = 1; i < nrSupports; i++) {
            float fraction = (float) i / nrSupports;
            Vector3f position = getPositionFromFraction(fraction);
            Vector3f direction = getDirectionFromFraction(fraction);

            TrackSupport support = new TrackSupport(game, type, position, direction);
            support.setDespawnTrigger(this::isDespawnedAt);
            list.add(support);
        }

        return list;
    }

}
