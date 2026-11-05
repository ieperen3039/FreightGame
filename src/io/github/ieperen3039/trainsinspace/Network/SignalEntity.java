package io.github.ieperen3039.trainsinspace.Network;

import io.github.ieperen3039.ngn.AssetHandling.Asset;
import io.github.ieperen3039.ngn.AssetHandling.GeneratorAsset;
import io.github.ieperen3039.ngn.DataStructures.Generic.Color4f;
import io.github.ieperen3039.ngn.InputHandling.KeyControl;
import io.github.ieperen3039.ngn.Rendering.Material;
import io.github.ieperen3039.ngn.Rendering.MatrixStack.SGL;
import io.github.ieperen3039.ngn.Rendering.MeshLoading.Mesh;
import io.github.ieperen3039.ngn.Rendering.Shaders.MaterialShader;
import io.github.ieperen3039.ngn.Tools.Vectors;
import io.github.ieperen3039.trainsinspace.Core.Coloring;
import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.Entities.GameEntity;
import io.github.ieperen3039.trainsinspace.Entities.Shapes;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFW;

/**
 * @author Geert van Ieperen created on 26-5-2020.
 */
public class SignalEntity extends GameEntity {
    /**
     * number of vertices along the circle of the ring
     */
    private static final int RING_RESOLUTION = 128;
    /**
     * height of middle above the floor of the track
     */
    private static final float INNER_RADIUS = 0.6f;
    /**
     * size increase of the inner radius in each direction to avoid collision
     */
    private static final float MARGIN = 0.1f;
    /**
     * how far each color ring is offset from the middle
     */
    private static final float COLOR_OFFSET = 0.1f;

    private final static Asset<Mesh> RING_MESH = new GeneratorAsset<>(
            () ->
                    Shapes.createRing(INNER_RADIUS + MARGIN, RING_RESOLUTION, COLOR_OFFSET / 2f),
            Mesh::dispose
    );

    private final Vector3fc ringMiddle;
    private final Signal signal;
    protected transient Game game;

    private double despawnTime = Double.POSITIVE_INFINITY;
    private final Coloring coloring = new Coloring(Color4f.WHITE);

    /**
     * @param game                 game instance
     * @param sourceNode           node to attach to
     * @param inNodeDirection      whether this signal is in the same direction as sourceNode
     * @param allowOppositeTraffic whether to allow traffic in the opposite direction of sourceNode
     */
    public SignalEntity(Game game, RailNode sourceNode, boolean inNodeDirection, boolean allowOppositeTraffic) {
        signal = new Signal(sourceNode, inNodeDirection, allowOppositeTraffic);
        this.game = game;
        this.ringMiddle = new Vector3f(sourceNode.getPosition()).add(0, 0, INNER_RADIUS - MARGIN);
    }

    @Override
    public void update() {
        // TODO maybe color

        if (signal.hostNode.isUnconnected()) {
            despawn(game.timer().getTime());
        }
    }

    @Override
    public void draw(SGL gl) {
        gl.pushMatrix();
        {
            gl.translate(ringMiddle);

            Vector3fc targetDirection = signal.hostNode.getDirection();
            Vector3f cross = Vectors.Z.cross(targetDirection, new Vector3f());
            gl.rotate(cross, Vectors.Z.angle(targetDirection));

            int sig = signal.inNodeDirection ? 1 : -1;

            MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, getColor()));
            gl.render(RING_MESH.get(), entityId);

            gl.translate(0, 0, -sig * COLOR_OFFSET);
            MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, Color4f.GREEN));
            gl.render(RING_MESH.get(), entityId);
            gl.translate(0, 0, sig * COLOR_OFFSET);

            if (!signal.allowOppositeTraffic) {
                gl.translate(0, 0, sig * COLOR_OFFSET);
                MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, Color4f.RED));
                gl.render(RING_MESH.get(), entityId);
                gl.translate(0, 0, -sig * COLOR_OFFSET);
            }
        }
        gl.popMatrix();
    }

    private Color4f getColor() {
        if (signal.hostNode.isUnconnected()) {
            return Color4f.CYAN;
        }

        return coloring.getColor();
    }

    public void onMouseClick(int button, KeyControl keys) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (keys.isControlPressed()) {
                signal.allowOppositeTraffic(!signal.allowOppositeTraffic);
            } else {
                signal.revert();
            }
        }
    }

    public void setMarking(Coloring.Marking mark) {
        coloring.addMark(mark);
    }

    @Override
    public boolean isDespawnedAt(double gameTime) {
        return gameTime > despawnTime || signal.hostNode.isUnconnected();
    }

    @Override
    public UpdateFrequency getUpdateFrequency() {
        return UpdateFrequency.NEVER;
    }

    public Signal getSignal() {
        return signal;
    }
}
