package NG.ScreenOverlay.Frames.Components;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

/**
 * @author Geert van Ieperen. Created on 22-9-2018.
 */
public abstract class SClickable extends SComponent {
    @Override
    public void onClick(int button, int x, int y) {
        if (button == GLFW_MOUSE_BUTTON_LEFT) {
            onLeftClick();

        } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
            onRightClick();
        }
    }

    public abstract void onLeftClick();

    public abstract void onRightClick();
}
