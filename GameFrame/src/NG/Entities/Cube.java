package NG.Entities;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.Rendering.MatrixStack.SGL;
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
    public Vector3fc getPosition() {
        return position;
    }

    @Override
    public void update() {
        // physics
    }

    @Override
    public void draw(SGL gl) {
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
