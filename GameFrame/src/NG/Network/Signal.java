package NG.Network;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.Entities.Entity;
import NG.InputHandling.MouseTools.AbstractMouseTool;
import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shapes.GenericShapes;
import NG.Resources.GeneratorResource;
import NG.Resources.Resource;
import NG.Tools.Vectors;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import static NG.InputHandling.MouseTools.AbstractMouseTool.MouseAction.PRESS_ACTIVATE;

/**
 * @author Geert van Ieperen created on 26-5-2020.
 */
public class Signal extends AbstractGameObject implements Entity {
    /** number of vertices along the circle of the ring */
    private static final int RING_RESOLUTION = 128;
    /** height of middle above the floor of the track */
    private static final float INNER_RADIUS = 0.6f;
    /** size increase of the inner radius in each direction to avoid collision */
    private static final float MARGIN = 0.1f;
    /** how far each color ring is offset from the middle */
    private static final float COLOR_OFFSET = 0.1f;

    private final static Resource<Mesh> RING_MESH = new GeneratorResource<>(() ->
            GenericShapes.createRing(INNER_RADIUS + MARGIN, RING_RESOLUTION, COLOR_OFFSET / 2f), Mesh::dispose
    );

    private final RailNode targetNode;
    private final Vector3fc ringMiddle;

    /** whether to allow traffic in the direction of targetNode */
    private boolean inSameDirection;
    /** whether to allow traffic in the opposite direction of targetNode */
    private boolean inOppositeDirection;

    private double despawnTime = Double.POSITIVE_INFINITY;

    /**
     * @param game                game instance
     * @param targetNode          node to attach to
     * @param inSameDirection     whether to allow traffic in the direction of targetNode
     * @param inOppositeDirection whether to allow traffic in the opposite direction of targetNode
     */
    public Signal(Game game, RailNode targetNode, boolean inSameDirection, boolean inOppositeDirection) {
        super(game);
        this.inOppositeDirection = inOppositeDirection;
        assert inSameDirection || inOppositeDirection : "Signal is a block";
        this.targetNode = targetNode;
        this.inSameDirection = inSameDirection;
        this.ringMiddle = new Vector3f(targetNode.getPosition()).add(0, 0, INNER_RADIUS - MARGIN);
    }

    @Override
    public void update() {
        // TODO color
    }

    @Override
    public void draw(SGL gl) {
        gl.pushMatrix();
        {
            gl.translate(ringMiddle);

            Vector3fc targetDirection = targetNode.getDirection();
            Vector3f cross = Vectors.newZVector().cross(targetDirection);
            gl.rotate(cross, (float) (Math.PI / 2));

            MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, Color4f.BLACK));
            gl.render(RING_MESH.get(), this);

            if (inSameDirection) {
                gl.translate(0, 0, COLOR_OFFSET);
                MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, Color4f.GREEN));
                gl.render(RING_MESH.get(), this);
                gl.translate(0, 0, -COLOR_OFFSET);
            }
            if (inOppositeDirection) {
                gl.translate(0, 0, -COLOR_OFFSET);
                MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, Color4f.GREEN));
                gl.render(RING_MESH.get(), this);
                gl.translate(0, 0, COLOR_OFFSET);
            }
        }
        gl.popMatrix();
    }

    @Override
    public void reactMouse(AbstractMouseTool.MouseAction action) {
        if (action.equals(PRESS_ACTIVATE)) {
            if (inSameDirection && inOppositeDirection) {
                inOppositeDirection = false;

            } else if (inSameDirection) {
                inOppositeDirection = true;
                inSameDirection = false;

            } else if (inOppositeDirection) {
                inSameDirection = true;

            } else {
                assert false : "Impassible signal " + this;
                inSameDirection = true;
            }
        }
    }

    @Override
    public double getDespawnTime() {
        return despawnTime;
    }

    @Override
    public void despawn(double gameTime) {
        despawnTime = gameTime;
    }

    @Override
    public UpdateFrequency getUpdateFrequency() {
        return UpdateFrequency.NEVER;
    }

    public RailNode getNode() {
        return targetNode;
    }
}
