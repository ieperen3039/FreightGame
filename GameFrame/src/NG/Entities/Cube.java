package NG.Entities;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
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

    private boolean isDisposed = false;
    private Vector3f position;

    public Cube(Game game, Vector3f position) {
        super(game);
        this.position = position;
        id = nr++;
    }

    @Override
    public Vector3fc getPosition(float time) {
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
            materialShader.setMaterial(Material.ROUGH, Color4f.GREY);
        }

        gl.pushMatrix();
        {
            gl.translate(position);
            gl.render(GenericShapes.CUBE, this);
        }
        gl.popMatrix();
    }

    @Override
    public void onClick(int button) {
        dispose();
    }

    @Override
    public void dispose() {
        isDisposed = true;
    }

    @Override
    public boolean isDisposed() {
        return isDisposed;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " " + id;
    }
}
