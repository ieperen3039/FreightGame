package NG.DataStructures.MatrixStack;

import NG.Camera.Camera;
import NG.DataStructures.Color4f;
import NG.DataStructures.Material;
import NG.Settings.Settings;
import NG.Shaders.ShaderProgram;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 15-11-2017.
 */
public interface SGL extends MatrixStack {

    /**
     * instructs the graphical card to render the specified mesh
     * @param object
     */
    void render(Mesh object);

    /**
     * sets a light in the scene for this frame only. Only affects objects drawn after this light.
     * @param pos        the position of the light
     * @param lightColor the color of the light, where alpha determines the brightness
     */
    void setLight(Vector3fc pos, Color4f lightColor);

    /**
     * sets the surface properties for the next meshes to the specified material, of which the color is blended into the
     * material.
     * @param material the new material properties
     * @param color    a color transformation. The alpha value determines how much of the color is blended into the
     *                 material
     */
    void setMaterial(Material material, Color4f color);

    /**
     * sets the surface properties for the next meshes to the specified material. The results is equal to {@code
     * setMaterial(material, Color4f.WHITE)}.
     * @param material the new material properties
     */
    default void setMaterial(Material material) {
        setMaterial(material, Color4f.WHITE);
    }

    /** @return the shader that is used for rendering. */
    ShaderProgram getShader();

    /**
     * maps the local vertex to its position on screen
     * @param vertex a position vector in local space
     * @return the position as ([-1, 1], [-1, 1]) on the view. Note that these coordinates may fall out of the given
     *         range if it is not in the player's FOV. returns null if this vertex is behind the player.
     */
    Vector2f getPositionOnScreen(Vector3fc vertex);

    /**
     * Objects should use GPU calls only in their render method. To prevent invalid uses of the {@link
     * Mesh#render(Painter)} object, a Painter object is required to call that render method.
     */
    class Painter {
        /**
         * Objects should call GPU calls only in their render method. This render method may only be called by a GL2
         * object, to prevent drawing calls while the GPU is not initialized. For this reason, the Painter constructor
         * is protected.
         */
        protected Painter() {
        }
    }

    /**
     * Calculates a projection matrix based on a camera position and the given parameters of the viewport
     * @param windowWidth  the width of the viewport in pixels
     * @param windowHeight the height of the viewport in pixels
     * @param camera       the camera position and orientation.
     * @param isometric    if true, an isometric projection will be calculated. Otherwise a perspective transformation
     *                     is used.
     * @return a projection matrix, such that modelspace vectors multiplied with this matrix will be transformed to
     *         viewspace.
     */
    static Matrix4f getProjection(float windowWidth, float windowHeight, Camera camera, boolean isometric) {
        Matrix4f vpMatrix = new Matrix4f();

        // Set the projection.
        float aspectRatio = windowWidth / windowHeight;

        if (isometric) {
            float visionSize = camera.vectorToFocus().length();
            vpMatrix.orthoSymmetric(aspectRatio * visionSize, visionSize, Settings.Z_NEAR, Settings.Z_FAR);
        } else {
            vpMatrix.setPerspective(Settings.FOV, aspectRatio, Settings.Z_NEAR, Settings.Z_FAR);
        }

        // set the view
        vpMatrix.lookAt(
                camera.getEye(),
                camera.getFocus(),
                camera.getUpVector()
        );

        return vpMatrix;
    }
}
