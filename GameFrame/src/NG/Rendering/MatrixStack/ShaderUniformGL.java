package NG.Rendering.MatrixStack;

import NG.Camera.Camera;
import NG.DataStructures.Color4f;
import NG.Entities.Entity;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Tools.Toolbox;
import org.joml.*;

import java.util.Stack;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author Geert van Ieperen created on 16-11-2017.
 */
public class ShaderUniformGL implements SGL {
    private static final int MAX_POINT_LIGHTS = 1;
    private static final Painter LOCK = new Painter();

    private Stack<Matrix4f> matrixStack;

    private Matrix4f modelMatrix;
    private final Matrix4f viewProjectionMatrix;
    private Matrix3f normalMatrix = new Matrix3f();

    private ShaderProgram shader;
    private int nextLightIndex = 0;

    /**
     * @param shader the shader to use for rendering
     * @param windowWidth the width of the viewport in pixels
     * @param windowHeight the height of the viewport in pixels
     * @param viewpoint the camera that defines eye position, focus and up vector
     * @param isometric when true, no perspective transformation is used. This results in a retro tycoon style
     */
    public ShaderUniformGL(ShaderProgram shader, int windowWidth, int windowHeight, Camera viewpoint, boolean isometric) {
        this.shader = shader;

        matrixStack = new Stack<>();
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, windowWidth, windowHeight);
        glEnable(GL_LINE_SMOOTH);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        modelMatrix = new Matrix4f();
        viewProjectionMatrix = SGL.getViewProjection(windowWidth, windowHeight, viewpoint, isometric);

        for (int i = 0; i < MAX_POINT_LIGHTS; i++) {
            shader.setPointLight(i, new Vector4f(), Color4f.INVISIBLE);
        }

        Toolbox.checkGLError();
    }

    @Override
    public void render(Mesh object) {
        shader.setProjectionMatrix(viewProjectionMatrix);
        shader.setModelMatrix(modelMatrix);
        modelMatrix.normal(normalMatrix);
        shader.setNormalMatrix(normalMatrix);
        object.render(LOCK);
    }

    @Override
    public void setLight(Color4f lightColor, Vector4fc position) {
        Vector4f mPos = new Vector4f(position);
        mPos.mul(modelMatrix);
        shader.setPointLight(nextLightIndex++, mPos, lightColor);
    }

    @Override
    public void rotate(float angle, float x, float y, float z) {
        rotate(new AxisAngle4f(angle, x, y, z));
    }

    public void rotate(AxisAngle4f rotation) {
        modelMatrix.rotate(rotation);
    }

    public ShaderProgram getShader() {
        return shader;
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
        modelMatrix.mulAffine(postTransformation);
    }

    @Override
    public Vector2f getPositionOnScreen(Vector3fc vertex) {
        Vector4f pos = new Vector4f(vertex, 1.0f);
        viewProjectionMatrix.transformProject(pos);
        if (pos.z() > 1) return null;
        else return new Vector2f(pos.x(), pos.y());
    }

    @Override
    public void ifAccepted(Entity entity, Consumer<SGL> drawFunction) {
        if (shader.accepts(entity)) {
            shader.setEntity(entity);
            drawFunction.accept(this);
            shader.unsetEntity();
        }
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
                ", shader=" + shader.getClass() +
                ", stackSize=" + matrixStack.size() +
                "\n}";
    }
}
