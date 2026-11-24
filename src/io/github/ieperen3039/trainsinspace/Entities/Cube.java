package io.github.ieperen3039.trainsinspace.Entities;

import io.github.ieperen3039.ngn.DataStructures.Generic.Color4f;
import io.github.ieperen3039.ngn.InputHandling.KeyControl;
import io.github.ieperen3039.ngn.Rendering.Material;
import io.github.ieperen3039.ngn.Rendering.MatrixStack.SGL;
import io.github.ieperen3039.ngn.Rendering.Shaders.MaterialShader;
import io.github.ieperen3039.ngn.Rendering.Shaders.ShaderProgram;
import io.github.ieperen3039.ngn.Rendering.Shapes.GenericShapes;
import io.github.ieperen3039.ngn.Tools.TickTime;
import io.github.ieperen3039.trainsinspace.Core.Coloring;
import io.github.ieperen3039.trainsinspace.Core.Game;
import org.joml.Vector3f;

/**
 * @author Geert van Ieperen created on 9-1-2019.
 */
public class Cube extends GameEntity {
    private static int nr = 0;
    private final int id;
    private final Game game;
    private Vector3f position;

    private final Coloring coloring = new Coloring(Color4f.WHITE);

    public Cube(Game game, Vector3f position) {
        this.game = game;
        this.position = position;
        id = nr++;
    }

    @Override
    public void update() {
        // physics
    }

    @Override
    public void draw(SGL gl, TickTime renderTime) {
        ShaderProgram shader = gl.getShader();

        if (shader instanceof MaterialShader materialShader) {
            materialShader.setMaterial(Material.ROUGH, coloring.getColor());
        }

        gl.pushMatrix();
        {
            gl.translate(position);
            gl.render(GenericShapes.CUBE, entityId);
        }
        gl.popMatrix();
    }

    public void onMouseClick(int button, KeyControl keys) {
        despawn(game.timer().getTime());
    }

    @Override
    public void setMarking(Coloring.Marking mark) {
        coloring.addMark(mark);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " " + id;
    }

}
