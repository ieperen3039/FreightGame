package io.github.ieperen3039.trainsinspace.Tracks;

import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.DataStructures.Generic.PairList;
import io.github.ieperen3039.trainsinspace.InputHandling.KeyControl;
import io.github.ieperen3039.trainsinspace.InputHandling.MouseTool.AbstractMouseTool;
import io.github.ieperen3039.trainsinspace.Rendering.MatrixStack.SGL;
import io.github.ieperen3039.trainsinspace.Rendering.MeshLoading.Mesh;
import io.github.ieperen3039.trainsinspace.Rendering.Shaders.MaterialShader;
import io.github.ieperen3039.trainsinspace.Rendering.Shaders.ShaderProgram;
import io.github.ieperen3039.trainsinspace.Rendering.Shapes.GenericShapes;
import io.github.ieperen3039.trainsinspace.Rendering.Shapes.Shape;
import io.github.ieperen3039.trainsinspace.AssetHandling.Asset;
import io.github.ieperen3039.trainsinspace.AssetHandling.GeneratorAsset;
import org.joml.Math;
import org.joml.*;

/**
 * a pillar that supports a track.
 * @author Geert van Ieperen created on 26-4-2021.
 */
public class TrackSupport extends TrackElement {
    public final Asset<Mesh> graphic;

    private final Vector3fc position;
    private final float orientation;
    private final Asset<AABBf> hitbox;
    private final PairList<Shape, Matrix4fc> collisionShapes;

    public TrackSupport(Game game, TrackType type, Vector3fc position, Vector3fc direction) {
        super(game, type);

        float mapHeight = game.map().getHeightAt(position.x(), position.y());
        float height = position.z() - mapHeight;

        this.position = new Vector3f(position.x(), position.y(), mapHeight);
        this.graphic = new GeneratorAsset<>(() -> getType().generateSupport(height));
        this.orientation = Math.atan2(direction.y(), direction.x());
        this.hitbox = new GeneratorAsset<>(this::computeHitbox);
        this.collisionShapes = new PairList<>(1);

        Matrix4f transform = new Matrix4f()
                .setTranslation(0, 0, 1)
                .scale(0.1f, 0.1f, height / 2);
        collisionShapes.add(GenericShapes.CUBE, transform);
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

    public Vector3fc getPosition() {
        return position;
    }

    public float getOrientation() {
        return orientation;
    }
}
