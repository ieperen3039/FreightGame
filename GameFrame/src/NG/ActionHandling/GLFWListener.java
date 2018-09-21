package NG.ActionHandling;

import NG.DataStructures.Tracked.TrackedInteger;
import NG.Engine.Game;
import NG.Engine.GameAspect;
import NG.Rendering.GLFWWindow;
import NG.Tools.Logger;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import java.util.ArrayList;
import java.util.Collection;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A mapping from lambda functions to GLFW callbacks
 * @author Geert van Ieperen. Created on 16-9-2018.
 */
public class GLFWListener implements GameAspect {
    private final Collection<KeyPressListener> keyPressListeners;
    private final Collection<KeyReleaseListener> keyReleaseListeners;
    private final Collection<MouseAnyButtonClickListener> mouseAnyButtonClickListeners;
    private final Collection<MouseDragListener> mouseButtonDragListeners;
    private final Collection<MouseScrollListener> mouseScrollListeners;

    public GLFWListener() {
        this.keyPressListeners = new ArrayList<>();
        this.keyReleaseListeners = new ArrayList<>();
        this.mouseAnyButtonClickListeners = new ArrayList<>();
        this.mouseButtonDragListeners = new ArrayList<>();
        this.mouseScrollListeners = new ArrayList<>();
    }

    @Override
    public void init(Game game) {
        GLFWWindow target = game.window();

        target.registerListener(new KeyPressCallback());
        target.registerListener(new MouseButtonPressCallback());
        target.registerListener(new MouseScrollCallback());
        target.registerListener(new MouseMoveCallback());
    }

    /**
     * @param action upon key press, receives the {@link org.lwjgl.glfw.GLFW} key that is pressed
     */
    public void onKeyPress(KeyPressListener action) {
        keyPressListeners.add(action);
    }

    /**
     * @param action upon key release, receives the {@link org.lwjgl.glfw.GLFW} key that is pressed
     */
    public void onKeyRelease(KeyReleaseListener action) {
        keyReleaseListeners.add(action);
    }

    /**
     * @param action upon mouse click, receives the {@link org.lwjgl.glfw.GLFW} mouse button that is pressed
     */
    public void onMouseButtonClick(MouseAnyButtonClickListener action) {
        mouseAnyButtonClickListeners.add(action);
    }

    /**
     * registers a listener that tracks a mouse drag action
     * @param action see {@link MouseDragListener}
     */
    public void onMouseDrag(MouseDragListener action) {
        mouseButtonDragListeners.add(action);
    }

    /**
     * @param action upon mouse scroll, receive the (arbitrary) scroll values
     */
    public void onMouseScroll(MouseScrollListener action) {
        mouseScrollListeners.add(action);
    }

    /**
     * tries to remove the given listener from all of the listener types. Even if the given listener is of multiple
     * types, all of the mare removed.
     * @param listener a previously installed listener
     * @return true iff any listener has been removed
     */
    @SuppressWarnings("SuspiciousMethodCalls")
    public boolean removeListener(Object listener) {
        boolean kp = keyPressListeners.remove(listener);
        boolean kr = keyReleaseListeners.remove(listener);
        boolean mc = mouseAnyButtonClickListeners.remove(listener);
        boolean md = mouseButtonDragListeners.remove(listener);
        boolean ms = mouseScrollListeners.remove(listener);
        return kp || kr || mc || md || ms;
    }

    @Override
    public void cleanup() {
        keyPressListeners.clear();
        keyReleaseListeners.clear();
        mouseAnyButtonClickListeners.clear();
        mouseButtonDragListeners.clear();
        mouseScrollListeners.clear();

        // GLFWWindow frees its own callbacks when cleaned up
    }

    private class KeyPressCallback extends GLFWKeyCallback {
        @Override
        public void invoke(long windowHandle, int keyCode, int scancode, int action, int mods) {
            if (keyCode < 0) return;
            if (action == GLFW_PRESS) {
                keyPressListeners.forEach(l -> l.keyPressed(keyCode));

            } else if (action == GLFW_RELEASE) {
                keyReleaseListeners.forEach(l -> l.keyReleased(keyCode));
            }
        }
    }

    private class MouseButtonPressCallback extends GLFWMouseButtonCallback {
        @Override
        public void invoke(long windowHandle, int button, int action, int mods) {
            if (action == GLFW_PRESS) {
                mouseButtonDragListeners.forEach(l -> l.set(button, true));

                double[] xBuf = new double[1];
                double[] yBuf = new double[1];
                glfwGetCursorPos(windowHandle, xBuf, yBuf);
                int x = (int) xBuf[0];
                int y = (int) yBuf[0];

                mouseAnyButtonClickListeners.forEach(l -> l.onClick(button, x, y));

            } else if (action == GLFW_RELEASE) {
                mouseButtonDragListeners.forEach(l -> l.set(button, false));
            }
        }
    }

    private class MouseScrollCallback extends GLFWScrollCallback {
        @Override
        public void invoke(long windowHandle, double xScroll, double yScroll) {
            mouseScrollListeners.forEach(l -> l.mouseScrolled(yScroll));
        }
    }

    private class MouseMoveCallback extends GLFWCursorPosCallback {
        private TrackedInteger x = new TrackedInteger(0);
        private TrackedInteger y = new TrackedInteger(0);

        @Override
        public void invoke(long window, double xCoord, double yCoord) {
            int xPos = (int) xCoord;
            int yPos = (int) yCoord;

            x.update(xPos);
            y.update(yPos);

            int xDelta = x.difference();
            int yDelta = y.difference();

            for (MouseDragListener listener : mouseButtonDragListeners) {
                if (listener.isPressed()) listener.mouseDragged(xDelta, yDelta);
            }
        }
    }

    /**
     * @author Geert van Ieperen. Created on 20-9-2018.
     */
    public abstract static class MouseDragListener {
        private final int targetButton;
        private boolean isPressed;

        public MouseDragListener(int targetButton) {
            this.targetButton = targetButton;
        }

        /** is called when a mouse button is pressed */
        public abstract void buttonPressed();

        /** is called when a mouse button is released */
        public abstract void buttonReleased();

        /**
         * between calls of button pressed and button released for the same button, this is called with the mouse
         * coordinates for each of the buttons separately
         */
        public abstract void mouseDragged(int xPos, int yPos);

        /** returns true if the button is currently held */
        public boolean isPressed() {
            return isPressed;
        }

        /** activate a button press or button release */
        private void set(int button, boolean press) {
            if (button != targetButton) return;

            if (press == isPressed) {
                Logger.ASSERT.print("Set button of drag listener to " + press + " while already in that state");
                return;
            }

            isPressed = press;
            if (press) {
                buttonPressed();
            } else {
                buttonReleased();
            }
        }
    }
}
