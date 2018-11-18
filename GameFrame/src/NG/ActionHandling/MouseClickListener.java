package NG.ActionHandling;

import org.lwjgl.glfw.GLFW;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public interface MouseClickListener {
    /**
     * whenever the user clicks, this event is fired
     * @param button the button number, Often one of:<br><table><tr><td>{@link GLFW#GLFW_MOUSE_BUTTON_LEFT} | </td><td>{@link
     *               GLFW#GLFW_MOUSE_BUTTON_LEFT} | </td><td>{@link GLFW#GLFW_MOUSE_BUTTON_MIDDLE} | </td></tr></table>
     * @param xSc    the x position on the screen
     * @param ySc    the y position on the screen
     */
    void onClick(int button, int xSc, int ySc);
}
