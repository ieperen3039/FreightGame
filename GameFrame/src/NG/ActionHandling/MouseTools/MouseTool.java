package NG.ActionHandling.MouseTools;

import NG.ActionHandling.MouseMoveListener;
import NG.ActionHandling.MouseReleaseListener;
import NG.Entities.Entity;
import NG.ScreenOverlay.Frames.Components.SComponent;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/**
 * Determines the behaviour of clicking
 * @author Geert van Ieperen. Created on 22-11-2018.
 */
public abstract class MouseTool implements MouseMoveListener, MouseReleaseListener {
    private int button;

    /**
     * applies the functionality of this tool to the given component
     * @param component a component where has been clicked on
     * @param xSc       screen x position of the mouse in pixels from left
     * @param ySc       screen y position of the mouse in pixels from top
     */
    public abstract void apply(SComponent component, int xSc, int ySc);

    /**
     * applies the functionality of this tool to the given entity
     * @param entity an entity that is clicked on using this tool, always not null
     * @param rayCollision the position where the click intersected with this object's hitbox
     */
    public abstract void apply(Entity entity, Vector3f rayCollision);

    /**
     * applies the functionality of this tool to the given position in the world
     * @param position a position in the world where is clicked.
     */
    public abstract void apply(Vector2fc position);

    /**
     * returns the button that caused any of the {@code apply(...)} functions
     * @return the enum value of a button, often {@link GLFW#GLFW_MOUSE_BUTTON_LEFT} or {@link
     *         GLFW#GLFW_MOUSE_BUTTON_RIGHT}
     */
    public int getButton() {
        return button;
    }

    /**
     * sets the button field. Should only be called by the input handling
     * @param button a button enum, often {@link GLFW#GLFW_MOUSE_BUTTON_LEFT} or {@link GLFW#GLFW_MOUSE_BUTTON_RIGHT}
     */
    public void setButton(int button) {
        this.button = button;
    }
}
