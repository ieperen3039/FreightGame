package NG.Entities;

import NG.DataStructures.MatrixStack.SGL;
import NG.Engine.GameTimer;
import org.joml.AABBf;

/**
 * An entity is anything that is both visible in the world, and allows interaction with other entities (including the
 * map). Particles and other visual things are not entities.
 * @author Geert van Ieperen. Created on 14-9-2018.
 */
public interface Entity {
    /**
     * Updates the entity one game tick further. Try using {@link GameTimer#getGametimeDifference()} for speed
     * calculations and {@link GameTimer#getGametime()} for position calculations
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

    default AABBf hitbox() {
        return null; // TODO implement clickboxes
    }

    ;
}
