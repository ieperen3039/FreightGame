package io.github.ieperen3039.trainsinspace.InputHandling;

import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public enum InputMapping {
    UseMouseTool(InputType.Mouse, GLFW.GLFW_MOUSE_BUTTON_LEFT),
    DeactivateMouseTool(InputType.Mouse, GLFW.GLFW_MOUSE_BUTTON_RIGHT),
    ConfirmAction(InputType.Key, GLFW.GLFW_KEY_ENTER),
    ;

    // TODO allow keybinds like "ctrl + right click" taking KeyControl into account
    // note: these can be changed
    private final AtomicBoolean isKey;
    private final AtomicInteger button;

    InputMapping(InputType type, int button) {
        this.isKey = new AtomicBoolean(type == InputType.Key);
        this.button = new AtomicInteger(button);
    }

    public boolean isKey() {
        return isKey.getOpaque();
    }

    public int getButton() {
        return button.getOpaque();
    }

    /** changes the keybind for this variable. */
    public void setButton(int newValue) {
        isKey.setOpaque(newValue < GLFW.GLFW_MOUSE_BUTTON_1 || newValue > GLFW.GLFW_MOUSE_BUTTON_8);
        button.setOpaque(newValue);
    }

    public enum InputType {
        Mouse,
        Key
    }
}
