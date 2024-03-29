package NG.Network;

import NG.Core.Coloring;
import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.Entities.Entity;
import NG.InputHandling.KeyControl;
import NG.InputHandling.MouseTool.AbstractMouseTool;
import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shapes.GenericShapes;
import NG.AssetHandling.Asset;
import NG.AssetHandling.GeneratorAsset;
import NG.Tools.Vectors;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import static NG.InputHandling.MouseTool.AbstractMouseTool.MouseAction.PRESS_ACTIVATE;

/**
 * @author Geert van Ieperen created on 26-5-2020.
 */
public class SignalEntity extends Signal implements Entity {
    /** number of vertices along the circle of the ring */
    private static final int RING_RESOLUTION = 128;
    /** height of middle above the floor of the track */
    private static final float INNER_RADIUS = 0.6f;
    /** size increase of the inner radius in each direction to avoid collision */
    private static final float MARGIN = 0.1f;
    /** how far each color ring is offset from the middle */
    private static final float COLOR_OFFSET = 0.1f;

    private final static Asset<Mesh> RING_MESH = new GeneratorAsset<>(() ->
            GenericShapes.createRing(INNER_RADIUS + MARGIN, RING_RESOLUTION, COLOR_OFFSET / 2f),
            Mesh::dispose
    );

    private final Vector3fc ringMiddle;
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
        super(sourceNode, inNodeDirection, allowOppositeTraffic);
        this.game = game;
        this.ringMiddle = new Vector3f(sourceNode.getPosition()).add(0, 0, INNER_RADIUS - MARGIN);
    }

    @Override
    public void update() {
        // TODO maybe color

        if (hostNode.isUnconnected()) {
            despawn(game.timer().getGameTime());
        }
    }

    @Override
    public void draw(SGL gl) {
        gl.pushMatrix();
        {
            gl.translate(ringMiddle);

            Vector3fc targetDirection = hostNode.getDirection();
            Vector3f cross = Vectors.newZVector().cross(targetDirection);
            gl.rotate(cross, Vectors.Z.angle(targetDirection));

            int sig = inNodeDirection ? 1 : -1;

            MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, getColor()));
            gl.render(RING_MESH.get(), this);

            gl.translate(0, 0, -sig * COLOR_OFFSET);
            MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, Color4f.GREEN));
            gl.render(RING_MESH.get(), this);
            gl.translate(0, 0, sig * COLOR_OFFSET);

            if (!allowOppositeTraffic) {
                gl.translate(0, 0, sig * COLOR_OFFSET);
                MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, Color4f.RED));
                gl.render(RING_MESH.get(), this);
                gl.translate(0, 0, -sig * COLOR_OFFSET);
            }
        }
        gl.popMatrix();
    }

    private Color4f getColor() {
        if (hostNode.isUnconnected()) {
            return Color4f.CYAN;
        }

        return coloring.getColor();
    }

    @Override
    public void reactMouse(AbstractMouseTool.MouseAction action, KeyControl keys) {
        if (action.equals(PRESS_ACTIVATE)) {
            if (keys.isControlPressed()) {
                allowOppositeTraffic(!allowOppositeTraffic);
            } else {
                revert();
            }
        }
    }

    public void setMarking(Coloring.Marking mark) {
        coloring.addMark(mark);
    }

    @Override
    public double getDespawnTime() {
        return despawnTime;
    }

    @Override
    public boolean isDespawnedAt(double gameTime) {
        return gameTime > despawnTime || hostNode.isUnconnected();
    }

    @Override
    public void despawn(double gameTime) {
        despawnTime = gameTime;
    }

    @Override
    public UpdateFrequency getUpdateFrequency() {
        return UpdateFrequency.NEVER;
    }

    @Override
    public final void restore(Game game) {
        if (this.game == null) {
            this.game = game;
        }
    }
}
