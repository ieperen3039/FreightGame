package NG.ActionHandling;

import static org.lwjgl.glfw.GLFW.*;

/**
 * An helper-interface to separate the button clicks into methods
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public interface MouseClickAdapter extends MouseClickListener {
    @Override
    default void onClick(int button, int xSc, int ySc) {
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

    default void onLeftClick() {
    }

    ;

    default void onRightClick() {
    }

    ;

    default void onMiddleButtonClick() {
    }

    ;
}
