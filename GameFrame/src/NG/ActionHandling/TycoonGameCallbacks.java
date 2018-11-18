package NG.ActionHandling;

import NG.Engine.Game;
import NG.Engine.GameAspect;
import NG.Rendering.GLFWWindow;
import org.lwjgl.glfw.*;

import java.util.ArrayList;
import java.util.Collection;

import static org.lwjgl.glfw.GLFW.*;

/**
 * @author Geert van Ieperen. Created on 18-11-2018.
 */
public class TycoonGameCallbacks implements GameAspect, KeyMouseCallbacks {
    private final Collection<KeyPressListener> keyPressListeners = new ArrayList<>();
    private final Collection<MousePositionListener> mousePositionListeners = new ArrayList<>();

    private KeyTypeListener keyTypeListener = null;
    private MouseClickListener mouseClickListener = null;
    private MouseReleaseListener mouseReleaseListener = null;
    private MouseScrollListener scrollListener = null;

    @Override
    public void init(Game game) {
        GLFWWindow target = game.window();
        target.setCallbacks(new KeyPressCallback(), new MouseButtonPressCallback(), new MouseMoveCallback(), new MouseScrollCallback());
        target.setTextCallback(new CharTypeCallback());
    }

    @Override
    public void cleanup() {
        keyPressListeners.clear();
    }

    @Override
    public void addMousePositionListener(MousePositionListener listener) {
        mousePositionListeners.add(listener);
    }

    @Override
    public void addKeyPressListener(KeyPressListener listener) {
        keyPressListeners.add(listener);
    }

    @Override
    @SuppressWarnings("SuspiciousMethodCalls")
    public boolean removeListener(Object listener) {
        boolean mp = mousePositionListeners.remove(listener);
        boolean kp = keyPressListeners.remove(listener);

        return mp || kp;
    }

    /**
     * Sets a listener such that all generated key events are forwarded to this listener.
     * @param listener The new listener, or null to uninstall this listener and allow regular key presses.
     */
    public void setKeyTypeListener(KeyTypeListener listener) {
        keyTypeListener = listener;
    }

    /**
     * Sets the click listener such that all clicks are captured by this listener
     * @param listener The new listener, or null to uninstall this listener
     */
    public void setMouseClickListener(MouseClickListener listener) {
        mouseClickListener = listener;
    }

    /**
     * Sets the click release listener such that whenever a click is released, this listener is notified
     * @param listener The new listener, or null to uninstall this listener
     */
    public void setMouseReleaseListener(MouseReleaseListener listener) {
        mouseReleaseListener = listener;
    }

    /**
     * Sets the scroll listener, such that whenever is scrolled, this listener is notified
     * @param listener The new listener, or null to uninstall this listener
     */
    public void setScrollListener(MouseScrollListener listener) {
        scrollListener = listener;
    }

    private class KeyPressCallback extends GLFWKeyCallback {
        @Override
        public void invoke(long window, int keyCode, int scanCode, int action, int mods) {
            if (keyCode < 0) return;
            if (action == GLFW_PRESS) {
                keyPressListeners.forEach(l -> l.keyPressed(keyCode));
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

            if (action == GLFW_PRESS && mouseClickListener != null) {
                mouseClickListener.onClick(button, x, y);

            } else if (action == GLFW_RELEASE && mouseReleaseListener != null) {
                mouseReleaseListener.onRelease(button, x, y);
            }
        }
    }

    private class MouseMoveCallback extends GLFWCursorPosCallback {
        @Override
        public void invoke(long window, double xpos, double ypos) {
            for (MousePositionListener listener : mousePositionListeners) {
                listener.mouseMoved((int) xpos, (int) ypos);
            }
        }
    }

    private class MouseScrollCallback extends GLFWScrollCallback {
        @Override
        public void invoke(long window, double xoffset, double yoffset) {
            if (scrollListener != null) {
                scrollListener.mouseScrolled((float) yoffset);
            }
        }
    }

    private class CharTypeCallback extends GLFWCharCallback {
        @Override
        public void invoke(long window, int codepoint) {
            if (keyTypeListener != null && Character.isAlphabetic(codepoint)) {
                char s = Character.toChars(codepoint)[0];
                keyTypeListener.keyTyped(s);
            }
        }
    }
}
