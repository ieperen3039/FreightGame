package NG.ActionHandling;

import static org.lwjgl.glfw.GLFW.*;

/**
 * An helper-interface to separate the button clicks into methods
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public interface MouseClickListener extends MouseAnyClickListener {
    @Override
    default void onClick(int button, int x, int y) {
        switch (button) {
            case GLFW_MOUSE_BUTTON_LEFT:
                onLeftClick();
                break;
            case GLFW_MOUSE_BUTTON_RIGHT:
                onRightClick();
                break;
            case GLFW_MOUSE_BUTTON_MIDDLE:
                onMiddleButtonClick();
                break;
        }
    }

    void onLeftClick();

    void onRightClick();

    void onMiddleButtonClick();
}
