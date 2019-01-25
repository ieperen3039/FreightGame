package NG.Rendering.MatrixStack;

import NG.Camera.Camera;
import NG.DataStructures.Generic.Color4f;
import NG.Entities.Entity;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Settings.Settings;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3fc;
import org.joml.Vector4fc;

import java.util.function.Consumer;

/**
 * This is a stripped down version of a {@link org.lwjgl.opengl.GL} object.
 * @author Geert van Ieperen created on 15-11-2017.
 */
public interface SGL extends MatrixStack {

    /**
     * instructs the graphical card to render the specified mesh
     * @param object A Mesh that has not been disposed.
     */
    void render(Mesh object);

    /**
     * sets a light in the scene for this frame only. Only affects objects drawn after this light.
     * @param lightColor the color of the light, where alpha determines the brightness
     * @param position the position, where the w component is 0 for an infinitely far light
     */
    void setLight(Color4f lightColor, Vector4fc position);

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
     * executes the given draw function if entity is accepted by the shader
     * @param entity       an entity
     * @param drawFunction the function that draws this entity
     */
    void ifAccepted(Entity entity, Consumer<SGL> drawFunction);

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
    static Matrix4f getViewProjection(float windowWidth, float windowHeight, Camera camera, boolean isometric) {
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