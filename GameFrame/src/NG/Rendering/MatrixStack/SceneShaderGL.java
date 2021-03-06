package NG.Rendering.MatrixStack;

import NG.Camera.Camera;
import NG.Entities.Entity;
import NG.Rendering.GLFWWindow;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.SceneShader;
import NG.Rendering.Shaders.ShaderProgram;
import org.joml.*;

/**
 * @author Geert van Ieperen created on 16-11-2017.
 */
public class SceneShaderGL extends AbstractSGL {
    private final Matrix4f viewProjectionMatrix;
    private Matrix3f normalMatrix = new Matrix3f();

    private SceneShader shader;

    /**
     * @param shader    the shader to use for rendering
     * @param viewpoint the camera that defines eye position, focus and up vector
     * @param window
     */
    public SceneShaderGL(SceneShader shader, Camera viewpoint, GLFWWindow window) {
        super();
        this.shader = shader;
        viewProjectionMatrix = viewpoint.getViewProjection(window);
    }

    public SceneShaderGL(Matrix4f viewProjectionMatrix, SceneShader shader) {
        this.viewProjectionMatrix = viewProjectionMatrix;
        this.shader = shader;
    }

    @Override
    public void render(Mesh mesh, Entity sourceEntity) {
        Matrix4f modelMatrix = getModelMatrix();
        modelMatrix.normal(normalMatrix);

        shader.setProjectionMatrix(viewProjectionMatrix);
        shader.setModelMatrix(modelMatrix);
        shader.setNormalMatrix(normalMatrix);

        mesh.render(LOCK);
    }

    public ShaderProgram getShader() {
        return shader;
    }

    @Override
    public Matrix4fc getViewProjectionMatrix() {
        return viewProjectionMatrix;
    }

    public Vector2f getPositionOnScreen(Vector3fc vertex) {
        Vector4f pos = new Vector4f(vertex, 1.0f);
        getViewProjectionMatrix().transformProject(pos);
        if (pos.z() > 1) {
            return null;
        } else {
            return new Vector2f(pos.x(), pos.y());
        }
    }

    @Override
    public String toString() {
        return "ShaderUniformGL {\n" +
                "modelMatrix=" + getModelMatrix() +
                ", viewProjectionMatrix=" + viewProjectionMatrix +
                ", normalMatrix=" + normalMatrix +
                ", shader=" + shader.getClass() +
                "\n}";
    }
}
