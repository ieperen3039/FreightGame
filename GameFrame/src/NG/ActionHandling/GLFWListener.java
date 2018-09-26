package NG.ActionHandling;

import NG.DataStructures.Tracked.TrackedInteger;
import NG.Engine.Game;
import NG.Engine.GameAspect;
import NG.Rendering.GLFWWindow;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import java.util.*;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A mapping from lambda functions to GLFW callbacks
 * @author Geert van Ieperen. Created on 16-9-2018.
 */
public class GLFWListener implements GameAspect {
    private final Collection<KeyPressListener> keyPressListeners;
    private final Collection<KeyReleaseListener> keyReleaseListeners;
    private final Collection<MouseAnyClickListener> mouseClickListeners;
    private final Collection<MouseReleaseListener> mouseReleaseListeners;
    private final Collection<MouseScrollListener> mouseScrollListeners;
    private final Collection<MouseMoveListener> mouseMotionListeners;

    private final Set<Integer> clickedButtons;
    private final Map<Integer, Collection<MouseMoveListener>> mouseDragListeners;

    public GLFWListener() {
        this.keyPressListeners = new ArrayList<>();
        this.keyReleaseListeners = new ArrayList<>();
        this.mouseClickListeners = new ArrayList<>();
        this.mouseReleaseListeners = new ArrayList<>();
        this.mouseScrollListeners = new ArrayList<>();
        this.mouseMotionListeners = new ArrayList<>();
        this.mouseDragListeners = new HashMap<>();
        this.clickedButtons = new HashSet<>();
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
    public void onMouseButtonClick(MouseAnyClickListener action) {
        mouseClickListeners.add(action);
    }

    public void onMouseRelease(MouseReleaseListener action) {
        mouseReleaseListeners.add(action);
    }

    public void onMouseMove(MouseMoveListener listener) {
        mouseMotionListeners.add(listener);
    }

    /**
     * registers a listener that tracks a mouse drag action with the given button
     * @param button the button to listen to
     * @param action see {@link MouseMoveListener}
     */
    public void onMouseDrag(Integer button, MouseMoveListener action) {
        mouseDragListeners.computeIfAbsent(button, ArrayList::new).add(action);
    }

    /**
     * @param action upon mouse scroll, receive the (arbitrary) scroll values
     */
    public void onMouseScroll(MouseScrollListener action) {
        mouseScrollListeners.add(action);
    }

    /**
     * tries to remove the given listener from all of the listener types, except for mouse move listeners. Even if the
     * given listener is of multiple types, all of them are removed.
     * @param listener a previously installed listener
     * @return true iff any listener has been removed
     */
    @SuppressWarnings("SuspiciousMethodCalls")
    public boolean removeListener(Object listener) {
        boolean kp = keyPressListeners.remove(listener);
        boolean kr = keyReleaseListeners.remove(listener);
        boolean mc = mouseClickListeners.remove(listener);
        boolean mr = mouseReleaseListeners.remove(listener);
        boolean ms = mouseScrollListeners.remove(listener);
        boolean mm = mouseMotionListeners.remove(listener);

        return kp || kr || mc || mr || ms || mm;
    }

    /**
     * tries to remove the given drag listener with the given button from the list of drag listeners. If button is null,
     * it will search all drag listeners and remove the first one found.
     * @param button the button where the listener is listening to, or null if this is unknown
     * @param leaver the listener to remove
     * @return true if one listener has been removed
     */
    public boolean removeMouseDragListener(Integer button, MouseMoveListener leaver) {
        if (button != null) {
            Collection<MouseMoveListener> list = mouseDragListeners.get(button);
            if (list == null) return true;
            return list.remove(leaver);

        } else {
            for (Collection<MouseMoveListener> list : mouseDragListeners.values()) {
                if (list.remove(leaver)) return true;
            }
            return false;
        }
    }

    @Override
    public void cleanup() {
        keyPressListeners.clear();
        keyReleaseListeners.clear();
        mouseClickListeners.clear();
        mouseDragListeners.clear();
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
            double[] xBuf = new double[1];
            double[] yBuf = new double[1];
            glfwGetCursorPos(windowHandle, xBuf, yBuf);
            int x = (int) xBuf[0];
            int y = (int) yBuf[0];

            if (action == GLFW_PRESS) {
                clickedButtons.add(button);
                mouseClickListeners.forEach(l -> l.onClick(button, x, y));

            } else if (action == GLFW_RELEASE) {
                clickedButtons.remove(button);
                mouseReleaseListeners.forEach(l -> l.onRelease(button, x, y));
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

            mouseMotionListeners.forEach(l -> l.mouseMoved(xDelta, yDelta));

            for (Integer button : clickedButtons) {
                Collection<MouseMoveListener> list = mouseDragListeners.get(button);
                if (list != null) {
                    list.forEach(l -> l.mouseMoved(xDelta, yDelta));
                }
            }
        }
    }
}
