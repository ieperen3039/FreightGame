package NG.Entities;

import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shapes.Primitives.Collision;
import NG.Rendering.Shapes.ShapesGeneric;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 9-1-2019.
 */
public class Cube implements MovingEntity {
    private Vector3f position;
    private boolean isAlive = true;
    private static int nr = 0;
    private final int id;

    public Cube(Vector3f position) {
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
            gl.render(ShapesGeneric.CUBE);
        }
        gl.popMatrix();
    }

    @Override
    public void onClick(int button) {
        isAlive = false;
    }

    @Override
    public Collision getRayCollision(Vector3f origin, Vector3f direction) {
        return null;
    }

    @Override
    public boolean doRemove() {
        return !isAlive;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " " + id;
    }
}
