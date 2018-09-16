package NG.ActionHandling;

import NG.DataStructures.PairList;
import NG.Engine.GLFWWindow;
import NG.Engine.Game;
import NG.Engine.GameModule;
import NG.ScreenOverlay.UIElement;
import NG.Tools.Logger;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

/**
 * A mapping from GLFW Callbacks to lambda functions
 * @author Geert van Ieperen. Created on 16-9-2018.
 */
public class GLFWListener implements GameModule {
    private final Collection<Consumer<Integer>> keyPressListeners;
    private final Collection<Consumer<Integer>> keyReleaseListeners;
    private final Collection<Consumer<Integer>> mouseButtonClickListeners;
    private final Collection<DragListener> mouseButtonDragListeners;
    private final Collection<Consumer<Double>> mouseScrollListeners;

    private final PairList<UIElement, Consumer<Boolean>> mouseEnterListeners;

    public GLFWListener() {
        this.keyPressListeners = new ArrayList<>();
        this.keyReleaseListeners = new ArrayList<>();
        this.mouseButtonClickListeners = new ArrayList<>();
        this.mouseButtonDragListeners = new ArrayList<>();
        this.mouseScrollListeners = new ArrayList<>();
        this.mouseEnterListeners = new PairList<>();
    }

    @Override
    public void init(Game game) {
        GLFWWindow target = game.window();

        target.registerListener(new KeyPress());
        target.registerListener(new MouseButtonPress());
        target.registerListener(new MouseScroll());
        target.registerListener(new MouseMove());
    }

    /**
     * @param action upon key press, receives the {@link org.lwjgl.glfw.GLFW} key that is pressed
     */
    public void onKeyPress(Consumer<Integer> action) {
        keyPressListeners.add(action);
    }

    /**
     * @param action upon key release, receives the {@link org.lwjgl.glfw.GLFW} key that is pressed
     */
    public void onKeyRelease(Consumer<Integer> action) {
        keyReleaseListeners.add(action);
    }

    /**
     * @param action upon mouse click, receives the {@link org.lwjgl.glfw.GLFW} mouse button that is pressed
     */
    public void onMouseButtonClick(Consumer<Integer> action) {
        mouseButtonClickListeners.add(action);
    }

    /**
     * registers a listener that tracks a mouse drag action
     * @param action see {@link DragListener}
     */
    public void onMouseButtonDrag(DragListener action) {
        mouseButtonDragListeners.add(action);
    }

    /**
     * @param action upon mouse scroll, receive the (arbitrary) scroll values
     */
    public void onMouseScroll(Consumer<Double> action) {
        mouseScrollListeners.add(action);
    }

    /**
     * given an UI element, returns whether the mouse is on the object every time the mouse moves
     * @param element a visible UI element (or arbitrary rectangle for that matter)
     * @param action  executed once
     * @deprecated not particularly efficient implementation
     */
    public void onMouseEnter(UIElement element, Consumer<Boolean> action) {
        mouseEnterListeners.add(element, action);
    }

    /**
     * @param listener a previously installed listener
     * @return success
     */
    public boolean removeListener(Object listener) {
        if (listener instanceof Consumer) {
            if (keyPressListeners.remove(listener)) return true;
            if (keyReleaseListeners.remove(listener)) return true;
            if (mouseButtonClickListeners.remove(listener)) return true;
            if (mouseScrollListeners.remove(listener)) return true;

            // mouse enter listener
            int i = mouseEnterListeners.indexOfRight(listener);
            if (i > 0) {
                mouseEnterListeners.remove(i);
                return true;
            } else return false;

        } else if (listener instanceof DragListener) {
            return mouseButtonDragListeners.remove(listener);

        }

        Logger.ASSERT.print("Request on removing a listener that was not of listener type");
        // this was not a listener
        return false;
    }

    @Override
    public void cleanup() {
        keyPressListeners.clear();
        keyReleaseListeners.clear();
        mouseButtonClickListeners.clear();
        mouseButtonDragListeners.clear();
        mouseScrollListeners.clear();
        mouseEnterListeners.clear();

        // GLFWWindow frees its own callbacks when cleaned up
    }

    private class KeyPress extends GLFWKeyCallback {
        @Override
        public void invoke(long windowHandle, int keyCode, int scancode, int action, int mods) {
            if (keyCode < 0) return;
            if (action == GLFW_PRESS) {
                keyPressListeners.forEach(l -> l.accept(keyCode));
            } else if (action == GLFW_RELEASE) {
                keyReleaseListeners.forEach(l -> l.accept(keyCode));
            }
        }
    }

    private class MouseButtonPress extends GLFWMouseButtonCallback {
        @Override
        public void invoke(long windowHandle, int button, int action, int mods) {
            if (action == GLFW_PRESS) {
                mouseButtonClickListeners.forEach(l -> l.accept(button));
                mouseButtonDragListeners.forEach(l -> l.set(button, true));
            } else if (action == GLFW_RELEASE) {

                mouseButtonDragListeners.forEach(l -> l.set(button, false));
            }
        }
    }

    private class MouseScroll extends GLFWScrollCallback {
        @Override
        public void invoke(long windowHandle, double xScroll, double yScroll) {
            mouseScrollListeners.forEach(l -> l.accept(yScroll));
        }
    }

    private class MouseMove extends GLFWCursorPosCallback {
        @Override
        public void invoke(long window, double xCoord, double yCoord) {
            int xPos = (int) xCoord;
            int yPos = (int) yCoord;

            PairList<UIElement, Consumer<Boolean>> list = mouseEnterListeners;
            for (int i = 0; i < list.size(); i++) {
                UIElement elt = list.left(i);
                Consumer<Boolean> handler = list.right(i);

                handler.accept(elt.contains(xPos, yPos));
            }

            for (DragListener listener : mouseButtonDragListeners) {
                if (listener.isPressed) listener.mouseMoved(xPos, yPos);
            }
        }
    }

    private abstract class DragListener {
        private final int targetButton;
        private boolean isPressed;

        public DragListener(int targetButton) {
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
        public abstract void mouseMoved(int xPos, int yPos);

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
