package NG.Rendering.Shaders;

import NG.DataStructures.Generic.Color4f;
import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;

import java.util.function.Consumer;

/**
 * @author Geert van Ieperen created on 1-2-2019.
 */
public interface MaterialShader {
    default void setMaterial(Material material) {
        setMaterial(material.baseColor, material.specular, material.reflectance);
    }

    /**
     * sets the material properties to be shaded
     * @param material the base properties of the material
     * @param color    a blending color, such that the result is a colored version of the material.
     */
    default void setMaterial(Material material, Color4f color) {
        Color4f baseColor = material.baseColor.overlay(color);
        setMaterial(baseColor, material.specular, material.reflectance);
    }

    /**
     * sets the material properties to be shaded
     * @param diffuse     the natural (diffuse and ambient) color of this object.
     * @param specular    the color of the reflection on this object
     * @param reflectance a factor of reflectance, where a higher value means a higher shinyness.
     * @see #setMaterial(Material, Color4f)
     */
    void setMaterial(Color4f diffuse, Color4f specular, float reflectance);

    static void ifPresent(SGL gl, Consumer<MaterialShader> action) {
        ShaderProgram shader = gl.getShader();
        if (shader instanceof MaterialShader) {
            action.accept((MaterialShader) shader);
        }
    }
}
