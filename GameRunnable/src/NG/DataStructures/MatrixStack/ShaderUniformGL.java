package NG.DataStructures.MatrixStack;

import NG.Camera.Camera;
import NG.DataStructures.Color4f;
import NG.DataStructures.Material;
import NG.Shaders.ShaderProgram;
import NG.Tools.Toolbox;
import org.joml.*;

import java.util.Stack;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author Geert van Ieperen created on 16-11-2017.
 */
public class ShaderUniformGL implements GL2 {
    private static final int MAX_POINT_LIGHTS = 1;
    private static final float FOV = 0.3f;
    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 1000;

    private Stack<Matrix4f> matrixStack;

    private Matrix4f modelMatrix;
    private final Matrix4f viewProjectionMatrix;
    private Matrix3f normalMatrix = new Matrix3f();

    private ShaderProgram currentShader;
    private int nextLightIndex = 0;
    private boolean wireframeRendering = false;

    /**
     * @param shader
     * @param windowWidth
     * @param windowHeight
     * @param camera
     */
    public ShaderUniformGL(ShaderProgram shader, int windowWidth, int windowHeight, Camera camera) {
        currentShader = shader;

        matrixStack = new Stack<>();
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, windowWidth, windowHeight);
        Toolbox.checkGLError();
        glEnable(GL_LINE_SMOOTH);
        Toolbox.checkGLError();

        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        Toolbox.checkGLError();

        modelMatrix = new Matrix4f();
        viewProjectionMatrix = getProjection(windowWidth, windowHeight, camera);

        for (int i = 0; i < MAX_POINT_LIGHTS; i++) {
            shader.setPointLight(i, new Vector3f(), Color4f.INVISIBLE);
        }
    }

    private Matrix4f getProjection(float windowWidth, float windowHeight, Camera camera) {
        Matrix4f vpMatrix = new Matrix4f();

        // Set the projection.
        float aspectRatio = windowWidth / windowHeight;
        vpMatrix.setPerspective(FOV, aspectRatio, Z_NEAR, Z_FAR);

        // set the view
        vpMatrix.lookAt(
                camera.getEye(),
                camera.getFocus(),
                camera.getUpVector()
        );

        return vpMatrix;
    }

    @Override
    public void draw(Renderable object) {
        currentShader.setProjectionMatrix(viewProjectionMatrix);
        currentShader.setModelMatrix(modelMatrix);
        modelMatrix.normal(normalMatrix);
        currentShader.setNormalMatrix(normalMatrix);
        object.render(new Painter());
    }

    @Override
    public void setLight(Vector3fc pos, Color4f lightColor) {
        Vector3f mPosition = new Vector3f(pos);
        mPosition.mulPosition(modelMatrix);
        currentShader.setPointLight(nextLightIndex++, mPosition, lightColor);
    }

    public void setFill(boolean doFill) {
        if (doFill) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            wireframeRendering = false;
        } else {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            wireframeRendering = true;
        }
    }

    @Override
    public void setMaterial(Material material, Color4f color) {
        // TODO set material
    }

    @Override
    public void rotate(float angle, float x, float y, float z) {
        rotate(new AxisAngle4f(angle, x, y, z));
    }

    public void rotate(AxisAngle4f rotation) {
        modelMatrix.rotate(rotation);
    }

    @Override
    public void translate(float x, float y, float z) {
        modelMatrix.translate(x, y, z);
    }

    @Override
    public void scale(float x, float y, float z) {
        modelMatrix.scale(x, y, z);
    }

    @Override
    public Vector3f getPosition(Vector3f p) {
        Vector3f result = new Vector3f();
        p.mulPosition(modelMatrix, result);
        return result;
    }

    @Override
    public Vector3f getDirection(Vector3f v) {
        Vector3f result = new Vector3f();
        v.mulDirection(modelMatrix, result);
        return result;
    }

    @Override
    public void pushMatrix() {
        matrixStack.push(new Matrix4f(modelMatrix));
    }

    @Override
    public void popMatrix() {
        modelMatrix = matrixStack.pop();
    }

    @Override
    public void rotate(Quaternionf rotation) {
        modelMatrix.rotate(rotation);
    }

    @Override
    public void translate(Vector3fc v) {
        modelMatrix.translate(v);
    }

    @Override
    public void multiplyAffine(Matrix4f postTransformation) {
        modelMatrix.mul(postTransformation);
    }

    @Override
    public void popAll() {
        modelMatrix = new Matrix4f();
        matrixStack = new Stack<>();
    }

    @Override
    public Vector2f getPositionOnScreen(Vector3fc vertex) {
        Vector4f pos = new Vector4f(vertex, 1.0f);
        viewProjectionMatrix.transformProject(pos);
        if (pos.z() > 1) return null;
        else return new Vector2f(pos.x(), pos.y());
    }

    /** @return the view-projection matrix */
    public Matrix4fc getProjection() {
        return viewProjectionMatrix;
    }

    @Override
    public String toString() {
        return "ShaderUniformGL {\n" +
                "modelMatrix=" + modelMatrix +
                ", viewProjectionMatrix=" + viewProjectionMatrix +
                ", normalMatrix=" + normalMatrix +
                ", currentShader=" + currentShader.getClass() +
                ", stackSize=" + matrixStack.size() +
                "\n}";
    }
}
