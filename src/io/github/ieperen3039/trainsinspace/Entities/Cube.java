package io.github.ieperen3039.trainsinspace.Entities;

import io.github.ieperen3039.trainsinspace.Core.AbstractGameObject;
import io.github.ieperen3039.trainsinspace.Core.Coloring;
import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.DataStructures.Generic.Color4f;
import io.github.ieperen3039.trainsinspace.InputHandling.KeyControl;
import io.github.ieperen3039.trainsinspace.InputHandling.MouseTool.AbstractMouseTool;
import io.github.ieperen3039.trainsinspace.Rendering.Material;
import io.github.ieperen3039.trainsinspace.Rendering.MatrixStack.SGL;
import io.github.ieperen3039.trainsinspace.Rendering.Shaders.MaterialShader;
import io.github.ieperen3039.trainsinspace.Rendering.Shaders.ShaderProgram;
import io.github.ieperen3039.trainsinspace.Rendering.Shapes.GenericShapes;
import org.joml.Vector3f;

/**
 * @author Geert van Ieperen created on 9-1-2019.
 */
public class Cube extends AbstractGameObject implements Entity {
    private static int nr = 0;
    private final int id;
    private Vector3f position;

    private boolean isDisposed = false;
    private double despawnTime;
    private double spawnTime;
    private final Coloring coloring = new Coloring(Color4f.WHITE);

    public Cube(Game game, Vector3f position) {
        super(game);
        this.position = position;
        id = nr++;
    }

    @Override
    public void update() {
        // physics
    }

    @Override
    public void draw(SGL gl) {
        ShaderProgram shader = gl.getShader();

        if (shader instanceof MaterialShader) {
            MaterialShader materialShader = (MaterialShader) shader;
            materialShader.setMaterial(Material.ROUGH, coloring.getColor());
        }

        gl.pushMatrix();
        {
            gl.translate(position);
            gl.render(GenericShapes.CUBE, this);
        }
        gl.popMatrix();
    }

    @Override
    public void reactMouse(AbstractMouseTool.MouseAction action, KeyControl keys) {
        despawn(game.timer().getGameTime());
    }

    @Override
    public void setMarking(Coloring.Marking mark) {
        coloring.addMark(mark);
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
    public String toString() {
        return this.getClass().getSimpleName() + " " + id;
    }

    @Override
    public void restoreFields(Game game) {

    }
}
