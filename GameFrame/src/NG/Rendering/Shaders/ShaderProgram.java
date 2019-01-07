package NG.Rendering.Shaders;

import NG.DataStructures.Color4f;
import NG.DataStructures.Material;
import NG.Engine.Game;
import NG.Entities.Entity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;

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
     * initialize the uniforms for this shader
     * @param game the source of information
     */
    void initialize(Game game);

    /**
     * Allows shaders to only render specific entities.
     * @param entity an entity
     * @return true iff this entity should be rendered by this shader.
     */
    boolean accepts(Entity entity);

    void setEntity(Entity entity);

    void unsetEntity();

    /**
     * pass a pointlight to the shader
     * @param lightNumber the number which to adapt
     * @param mPosition   the position in model-space (worldspace)
     * @param color       the color of the light, with alpha as intensity
     */
    void setPointLight(int lightNumber, Vector4f mPosition, Color4f color);

    void setProjectionMatrix(Matrix4f viewProjectionMatrix);

    void setModelMatrix(Matrix4f modelMatrix);

    void setNormalMatrix(Matrix3f normalMatrix);

    void setMaterial(Color4f diffuse, Color4f specular, float reflectance);

    void setMaterial(Material material, Color4f color);

}
