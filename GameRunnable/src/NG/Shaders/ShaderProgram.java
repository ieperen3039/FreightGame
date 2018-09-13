package NG.Shaders;

import NG.DataStructures.Color4f;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * @author Geert van Ieperen created on 7-1-2018.
 */
public interface ShaderProgram {

    /**
     * Bind the renderer to the current rendering state
     */
    void bind();

    /**
     * Unbind the renderer from the current rendering state
     */
    void unbind();

    /**
     * Cleanup the renderer to a state of disposal
     */
    void cleanup();

    /**
     * Link the program and cleanup the shaders.
     * @throws ShaderException If an error occures linking the shader code.
     */
    void link() throws ShaderException;

    /**
     * pass a pointlight to the shader
     * @param lightNumber the number which to adapt
     * @param mPosition   the position in model-space (worldspace)
     * @param color       the color of the light, with alpha as intensity
     */
    void setPointLight(int lightNumber, Vector3f mPosition, Color4f color);

    void setProjectionMatrix(Matrix4f viewProjectionMatrix);

    void setModelMatrix(Matrix4f modelMatrix);

    void setNormalMatrix(Matrix3f normalMatrix);

    void setMaterial(Color4f diffuse, Color4f specular, float reflectance);
}
