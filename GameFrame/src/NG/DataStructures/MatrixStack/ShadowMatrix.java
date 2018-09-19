package NG.DataStructures.MatrixStack;

import org.joml.*;

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
    public Vector3f getPosition(Vector3f p) {
        Vector3f result = new Vector3f();
        p.mulPosition(matrix, result);
        return result;
    }

    @Override
    public Vector3f getDirection(Vector3f v) {
        Vector3f result = new Vector3f();
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
    public void translate(Vector3fc v) {
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
    public String toString() {
        return "ShadowMatrix{\n" +
                "matrix=" + matrix +
                ", stackSize=" + matrixStack.size() +
                "\n}";
    }

    public Vector3f mapToLocal(Vector3f p) {
        if (inverseMatrix == null) inverseMatrix = matrix.invertAffine(new Matrix4f());

        Vector3f result = new Vector3f();
        p.mulPosition(inverseMatrix, result);
        return result;
    }
}
