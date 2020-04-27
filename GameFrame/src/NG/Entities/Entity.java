package NG.Entities;

import NG.Core.GameObject;
import NG.Core.GameTimer;
import NG.Rendering.MatrixStack.SGL;

/**
 * An entity is anything that is both visible in the world, and allows interaction with other entities (including the
 * map). Particles and other visual things are not entities.
 * @author Geert van Ieperen. Created on 14-9-2018.
 */
public interface Entity extends GameObject {
    enum UpdateFrequency {
        EVERY_FRAME, EVERY_TICK, ONCE_PER_SECOND, ONCE_UPON_A_TIME, NEVER
    }

    /**
     * Updates the state of the entity. The frequency this method is called depend on the return value of {@link
     * #getUpdateFrequency()}. Use {@link GameTimer#getGametimeDifference()} for speed calculations and {@link
     * GameTimer#getGametime()} for position calculations
     */
    void update();

    /**
     * Draws this entity using the provided SGL object. This method may only be called from the rendering loop, and
     * should not change the internal representation of this object. Possible animations should be based on {@link
     * GameTimer#getRendertime()}. Material must be set using {@link SGL#getShader()}.
     * @param gl the graphics object to be used for rendering. It is initialized at world's origin. (no translation or
     *           scaling has been applied)
     */
    void draw(SGL gl);

    /**
     * Executes when the user clicks on this entity. When {@code button == GLFW_LEFT_MOUSE_BUTTON} is clicked, an {@link
     * NG.GUIMenu.Components.SFrame} with information or settings of this Entity is usually opened, and when {@code
     * button == GLFW_RIGHT_MOUSE_BUTTON} is clicked, the 'active' state of this entity may toggle.
     * @param button the button that is clicked as defined in {@link NG.InputHandling.MouseRelativeClickListener}
     */
    void onClick(int button);

    /**
     * Marks the track piece to be invalid, such that the {@link #isDisposed()} method returns true.
     */
    void dispose();

    /**
     * @return true iff this unit should be removed from the game world before the next gameloop.
     */
    boolean isDisposed();

    /**
     * the {@link #update()} method of this entity will be called according to the given update frequency
     * @return a constant value indicating how often to update this entity
     */
    UpdateFrequency getUpdateFrequency();

}
