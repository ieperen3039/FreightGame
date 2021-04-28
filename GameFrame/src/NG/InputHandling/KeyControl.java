package NG.InputHandling;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

/**
 * @author Geert van Ieperen created on 27-4-2020.
 */
public class KeyControl implements KeyPressListener, KeyReleaseListener {
    private boolean isShiftPressed;
    private boolean isControlPressed;
    private boolean isAltPressed;

    private final Map<Integer, Runnable> keyListeners = new HashMap<>();

    @Override
    public void keyPressed(int keyCode) {
        setKey(keyCode, true);
        Runnable runnable = keyListeners.get(keyCode);
        if (runnable != null) runnable.run();
    }

    @Override
    public void keyReleased(int keyCode) {
        setKey(keyCode, false);
    }

    public void addKeyListener(int keyCode, Runnable action) {
        keyListeners.put(keyCode, action);
    }

    public void removeKeyListener(int keyCode) {
        keyListeners.remove(keyCode);
    }

    private void setKey(int keyCode, boolean pressed) {
        switch (keyCode) {
            case GLFW_KEY_LEFT_SHIFT:
            case GLFW_KEY_RIGHT_SHIFT:
                isShiftPressed = pressed;
                break;

            case GLFW_KEY_LEFT_CONTROL:
            case GLFW_KEY_RIGHT_CONTROL:
                isControlPressed = pressed;
                break;

            case GLFW_KEY_LEFT_ALT:
            case GLFW_KEY_RIGHT_ALT:
                isAltPressed = pressed;
                break;
        }
    }

    public boolean isShiftPressed() {
        return isShiftPressed;
    }

    public boolean isControlPressed() {
        return isControlPressed;
    }

    public boolean isAltPressed() {
        return isAltPressed;
    }
}
