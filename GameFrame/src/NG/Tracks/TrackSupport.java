package NG.Tracks;

import NG.Core.Game;
import NG.DataStructures.Generic.PairList;
import NG.InputHandling.KeyControl;
import NG.InputHandling.MouseTool.AbstractMouseTool;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Rendering.Shapes.GenericShapes;
import NG.Rendering.Shapes.Shape;
import NG.Resources.GeneratorResource;
import NG.Resources.Resource;
import org.joml.Math;
import org.joml.*;

import java.util.function.Predicate;

/**
 * a pillar that supports a track. This entity accepts a despawn trigger which can be set with {@link
 * #setDespawnTrigger(Predicate)}.
 * @author Geert van Ieperen created on 26-4-2021.
 */
public class TrackSupport extends TrackElement {
    public final Resource<Mesh> graphic;
    public final Vector3fc position;
    public final float orientation;

    private final Resource<AABBf> hitbox;
    private final PairList<Shape, Matrix4fc> collisionShapes;
    private Predicate<Double> despawnTrigger = super::isDespawnedAt;

    public TrackSupport(Game game, TrackType type, Vector3fc position, Vector3fc direction) {
        super(game, type);

        float mapHeight = game.map().getHeightAt(position.x(), position.y());
        float height = position.z() - mapHeight;

        this.position = new Vector3f(position.x(), position.y(), mapHeight);
        this.graphic = new GeneratorResource<>(() -> getType().generateSupport(height));
        this.orientation = Math.atan2(direction.y(), direction.x());
        this.hitbox = new GeneratorResource<>(this::computeHitbox);
        this.collisionShapes = new PairList<>(1);

        Matrix4f transform = new Matrix4f()
                .setTranslation(0, 0, 1)
                .scale(0.1f, 0.1f, height / 2);
        collisionShapes.add(GenericShapes.CUBE, transform);
    }

    @Override
    public boolean isDespawnedAt(double gameTime) {
        return super.isDespawnedAt(gameTime) || despawnTrigger.test(gameTime);
    }

    @Override
    public void update() {
    }

    @Override
    public void draw(SGL gl) {
        ShaderProgram shader = gl.getShader();

        if (shader instanceof MaterialShader) {
            type.setMaterial((MaterialShader) shader, this, coloring.getColor());
        }

        gl.pushMatrix();
        {
            gl.translate(position);
            gl.rotateXYZ(0, 0, orientation);
            gl.render(graphic.get(), null);
        }
        gl.popMatrix();
    }

    @Override
    public void reactMouse(AbstractMouseTool.MouseAction action, KeyControl keys) {
    }

    @Override
    public AABBf getHitbox() {
        return hitbox.get();
    }

    @Override
    public PairList<Shape, Matrix4fc> getConvexCollisionShapes() {
        return collisionShapes;
    }

    public void setDespawnTrigger(Predicate<Double> despawnTrigger) {
        this.despawnTrigger = despawnTrigger;
    }
}
