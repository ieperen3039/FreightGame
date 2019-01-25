package NG.ActionHandling;

import NG.ActionHandling.MouseTools.DefaultMouseTool;
import NG.ActionHandling.MouseTools.MouseTool;
import NG.Engine.Game;
import NG.Engine.GameAspect;
import NG.Rendering.GLFWWindow;
import NG.Tools.Logger;
import org.lwjgl.glfw.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A callback handler specialized on a tycoon-game
 * @author Geert van Ieperen. Created on 18-11-2018.
 */
public class FreightCallbacks implements GameAspect, KeyMouseCallbacks {
    private final DefaultMouseTool DEFAULT_MOUSE_TOOL = new DefaultMouseTool();
    private final Collection<KeyPressListener> keyPressListeners = new ArrayList<>();
    private final Collection<KeyReleaseListener> keyReleaseListeners = new ArrayList<>();
    private final Collection<MousePositionListener> mousePositionListeners = new ArrayList<>();

    private Game game;
    private KeyTypeListener keyTypeListener = null;
    private MouseTool currentTool = DEFAULT_MOUSE_TOOL;

    @Override
    public void init(Game game) {
        this.game = game;
        GLFWWindow target = game.window();
        target.setCallbacks(new KeyPressCallback(), new MouseButtonPressCallback(), new MouseMoveCallback(), new MouseScrollCallback());
        target.setTextCallback(new CharTypeCallback());
    }

    @Override
    public void cleanup() {
        keyPressListeners.clear();
        keyReleaseListeners.clear();
        mousePositionListeners.clear();
        keyTypeListener = null;
    }

    @Override
    public void setMouseTool(MouseTool tool) {
        currentTool = Objects.requireNonNullElse(tool, DEFAULT_MOUSE_TOOL);
        Logger.DEBUG.print("Set mousetool to " + currentTool);
    }

    @Override
    public MouseTool getMouseTool() {
        return currentTool;
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
    public void addKeyReleaseListener(KeyReleaseListener listener) {
        keyReleaseListeners.add(listener);
    }

    @Override
    @SuppressWarnings("SuspiciousMethodCalls")
    public boolean removeListener(Object listener) {
        boolean mp = mousePositionListeners.remove(listener);
        boolean kp = keyPressListeners.remove(listener);
        boolean kr = keyReleaseListeners.remove(listener);

        return mp || kp || kr;
    }

    /**
     * Sets a listener such that all generated key events are forwarded to this listener.
     * @param listener The new listener, or null to uninstall this listener and allow regular key presses.
     */
    public void setKeyTypeListener(KeyTypeListener listener) {
        keyTypeListener = listener;
    }

    private class KeyPressCallback extends GLFWKeyCallback {
        @Override
        public void invoke(long window, int keyCode, int scanCode, int action, int mods) {
            if (keyCode < 0) return;
            if (action == GLFW_PRESS) {
                for (KeyPressListener l : keyPressListeners) {
                    l.keyPressed(keyCode);
                }
            } else if (action == GLFW_RELEASE) {
                for (KeyReleaseListener l : keyReleaseListeners) {
                    l.keyReleased(keyCode);
                }
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

            currentTool.setButton(button);
            if (action == GLFW_PRESS) {
                if (game.gui().checkMouseClick(currentTool, x, y)) return;

                // invert y for transforming to model space (inconsistency between OpenGL and GLFW)
                y = game.window().getHeight() - y;

                if (game.state().checkMouseClick(currentTool, x, y)) return;
                game.map().checkMouseClick(currentTool, x, y);

            } else if (action == GLFW_RELEASE) {
                currentTool.onRelease(button, x, y);
            }

        }
    }

    private class MouseMoveCallback extends GLFWCursorPosCallback {
        @Override
        public void invoke(long window, double xpos, double ypos) {
            for (MousePositionListener listener : mousePositionListeners) {
                listener.mouseMoved((int) xpos, (int) ypos);
            }

            currentTool.mouseMoved((int) xpos, (int) ypos);
        }
    }

    private class MouseScrollCallback extends GLFWScrollCallback {
        @Override
        public void invoke(long window, double xoffset, double yoffset) {
            game.camera().onScroll((float) yoffset);
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