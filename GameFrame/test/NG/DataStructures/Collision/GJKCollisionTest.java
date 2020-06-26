package NG.DataStructures.Collision;

import NG.Rendering.Shapes.GenericShapes;
import NG.Tools.Vectors;
import org.joml.Matrix4f;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Geert van Ieperen created on 26-6-2020.
 */
public class GJKCollisionTest {

    protected float angle(int degrees) {
        return (float) Math.toRadians(degrees);
    }

    @Test
    public void testTrivialTrue() {
        check(
                new Matrix4f(),
                new Matrix4f(),
                true
        );
    }

    @Test
    public void testTrivialFalse() {
        check(
                new Matrix4f()
                        .translate(2, 2, 2),
                new Matrix4f()
                        .translate(-2, -2, -2),
                false
        );
    }

    @Test
    public void testXAjacent() {
        check(
                new Matrix4f()
                        .translate(.5f, 0, 0.1f),
                new Matrix4f()
                        .translate(-1.6f, 0, 0),
                false
        );
    }

    @Test
    public void testRotatedTrue() {
        check(
                new Matrix4f()
                        .translate(2.1f, 0, 0.1f)
                        .rotateZ(angle(45)),
                new Matrix4f(),
                true
        );
    }

    @Test
    public void testRotatedFalse() {
        check(
                new Matrix4f()
                        .translate(1.9f, 1.9f, 0.1f)
                        .rotateZ(angle(45)),
                new Matrix4f(),
                false
        );
    }

    @Test
    public void testBothRotatedTrue() {
        check(
                new Matrix4f()
                        .translate(0.51f, 0.51f, 0.1f)
                        .rotateZ(angle(45)),
                new Matrix4f()
                        .rotateZ(angle(45)),
                true
        );
    }

    @Test
    public void testBothRotatedFalse() {
        check(
                new Matrix4f()
                        .translate(1.9f, 1.9f, 0.1f)
                        .rotateZ(angle(45)),
                new Matrix4f()
                        .rotateZ(angle(45)),
                false
        );
    }

    @Test
    public void testDipTrue() {
        check(
                new Matrix4f()
                        .translate(0, 0, 2.2f) // 2 + (sqrt(3) - 1) - delta
                        .rotateZ(angle(45))
                        .rotateY(angle(45)),
                new Matrix4f(),
                true
        );
    }

    @Test
    public void testDipFalse() {
        check(
                new Matrix4f()
                        .translate(0, 0, 2.8f) // 2 + (sqrt(3) - 1) + delta
                        .rotateZ(angle(45))
                        .rotateY(angle(45)),
                new Matrix4f(),
                false
        );
    }

    @Test
    public void testEdgeClipTrue() {
        check(
                new Matrix4f()
                        .translate(1, 1, 1),
                new Matrix4f()
                        .translate(-0.9f, -0.9f, 0.5f)
                        .rotate(angle(90), 1, 1, 0),
                true
        );
    }

    @Test
    public void testEdgeClipFalse() {
        check(
                new Matrix4f()
                        .translate(1, 1, 1),
                new Matrix4f()
                        .translate(-1.1f, -1.1f, 0.5f)
                        .rotate(angle(90), 1, 1, 0),
                true
        );
    }

    protected void check(Matrix4f aTransform, Matrix4f bTransform, boolean expected) {
        boolean result = GilbertJohnsonKeerthiCollision.gjk(
                GenericShapes.CUBE, aTransform, GenericShapes.CUBE, bTransform, Vectors.Z
        );
        Assert.assertEquals(expected, result);
    }
}
