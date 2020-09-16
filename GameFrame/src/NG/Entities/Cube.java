package NG.Entities;

import NG.Core.AbstractGameObject;
import NG.Core.Coloring;
import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.InputHandling.KeyControl;
import NG.InputHandling.MouseTools.AbstractMouseTool;
import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Rendering.Shapes.GenericShapes;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 9-1-2019.
 */
public class Cube extends AbstractGameObject implements MovingEntity {
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
    public Vector3fc getPosition(double time) {
        return position;
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
}
