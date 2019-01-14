package NG.Entities;

import NG.Engine.GameTimer;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shapes.Primitives.Collision;
import NG.Rendering.Shapes.Shape;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * An entity is anything that is both visible in the world, and allows interaction with other entities (including the
 * map). Particles and other visual things are not entities.
 * @author Geert van Ieperen. Created on 14-9-2018.
 */
public interface Entity {
    /**
     * Updates the state of the entity. The frequency this method is called depend on the return value of {@link
     * #getUpdateFrequency()}. Use {@link GameTimer#getGametimeDifference()} for speed calculations and {@link
     * GameTimer#getGametime()} for position calculations
     */
    void update();

    /**
     * Draws this entity using the provided SGL object. This method may only be called from the rendering loop, and
     * should not change the internal representation of this object. Possible animations should be based on {@link
     * GameTimer#getRendertime()}.
     * @param gl the graphics object to be used for rendering. It must be initialized to the position, rotation and
     *           scaling of this object, e.g. {@code gl.getPosition(0, 0, 0)} returns the position of this entity.
     */
    void draw(SGL gl);

    /**
     * Executes when the user clicks on this entity. When {@code button == GLFW_LEFT_MOUSE_BUTTON} is clicked, an {@link
     * NG.ScreenOverlay.Frames.Components.SFrame} with information or settings of this Entity is usually opened, and
     * when {@code button == GLFW_RIGHT_MOUSE_BUTTON} is clicked, the 'active' state of this entity may toggle.
     * @param button the button that is clicked as defined in {@link NG.ActionHandling.MouseRelativeClickListener}
     */
    void onClick(int button);

    /**
     * the {@link #update()} method of this entity will be called according to the given update frequency
     * @return a constant value indicating how often to update this entity
     */
    UpdateFrequency getUpdateFrequency();

    enum UpdateFrequency {
        EVERY_FRAME, EVERY_TICK, ONCE_PER_SECOND, ONCE_UPON_A_TIME, NEVER
    }

    /**
     * determines the collision of a ray with this entity.
     * @param origin    the origin of the ray
     * @param direction the direction of the ray
     * @return a Collision object resutling from the ray, or null if the ray did not hit
     * @see Shape#getCollision(Vector3fc, Vector3fc, Vector3fc)
     */
    Collision getRayCollision(Vector3f origin, Vector3f direction);

    /**
     * Default implementation returns false.
     * @return true iff this unit should be removed from the game world before the next gameloop.
     */
    default boolean doRemove() {
        return false;
    }
}
