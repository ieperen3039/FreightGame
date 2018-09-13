package NG.DataStructures.MatrixStack;

import nl.NG.Jetfightergame.Tools.Vectors.DirVector;
import nl.NG.Jetfightergame.Tools.Vectors.PosVector;
import nl.NG.Jetfightergame.Tools.Vectors.Vector;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Stack;

/**
 * @author Geert van Ieperen created on 27-12-2017.
 */
public class ShadowMatrix implements MatrixStack {

    private Stack<Matrix4f> matrixStack;
    private Matrix4f matrix;
    private Matrix4f inverseMatrix;

    public ShadowMatrix() {
        matrixStack = new Stack<>();
        matrix = new Matrix4f();
        inverseMatrix = null;
    }

    @Override
    public void rotate(float angle, float x, float y, float z) {
        rotate(new AxisAngle4f(angle, x, y, z));
    }

    public void rotate(AxisAngle4f rotation) {
        matrix.rotate(rotation);
        inverseMatrix = null;
    }

    @Override
    public void translate(float x, float y, float z) {
        matrix.translate(x, y, z);
        inverseMatrix = null;
    }

    @Override
    public void scale(float x, float y, float z) {
        matrix.scale(x, y, z);
        inverseMatrix = null;
    }

    @Override
    public PosVector getPosition(PosVector p) {
        PosVector result = new PosVector();
        p.mulPosition(matrix, result);
        return result;
    }

    @Override
    public DirVector getDirection(DirVector v) {
        DirVector result = new DirVector();
        v.mulDirection(matrix, result);
        return result;
    }

    @Override
    public void pushMatrix() {
        matrixStack.push(new Matrix4f(matrix));
        inverseMatrix = null;
    }

    @Override
    public void popMatrix() {
        matrix = matrixStack.pop();
        inverseMatrix = null;
    }

    @Override
    public void rotate(Quaternionf rotation) {
        matrix.rotate(rotation);
        inverseMatrix = null;
    }

    @Override
    public void translate(Vector v) {
        matrix.translate(v);
        inverseMatrix = null;
    }

    @Override
    public void multiplyAffine(Matrix4f postTransformation) {
        // first apply combinedTransformation, then the viewTransformation
        postTransformation.mul(matrix, matrix);
        inverseMatrix = null;
    }

    @Override
    public void popAll() {
        matrix = new Matrix4f();
        matrixStack = new Stack<>();
        inverseMatrix = null;
    }

    @Override
    public String toString() {
        return "ShadowMatrix{\n" +
                "matrix=" + matrix +
                ", stackSize=" + matrixStack.size() +
                "\n}";
    }

    public PosVector mapToLocal(PosVector p) {
        if (inverseMatrix == null) inverseMatrix = matrix.invertAffine(new Matrix4f());

        PosVector result = new PosVector();
        p.mulPosition(inverseMatrix, result);
        return result;
    }
}
