package NG.Engine;

import NG.Camera.Camera;
import NG.Camera.StaticCamera;
import NG.GameState.GameState;
import NG.Settings.Settings;
import NG.Tools.Vectors;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import java.io.IOException;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

/**
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class FreightGame {
    public final GameTimer time;
    public final Camera camera;
    public final GameState gamestate;
    public final RenderLoop renderer;
    public final Settings settings;
    public final GLFWWindow window;

    public FreightGame() {
        settings = new Settings();

        time = new GameTimer();
        camera = new StaticCamera(this, Vectors.zeroVector(), Vectors.zVector());
        window = new GLFWWindow(this, settings.GAME_NAME, true);
        renderer = new RenderLoop(this, 30);
        gamestate = new GameState(this);
    }

    private void init() throws IOException {
        // init all fields
        window.init();
        renderer.init();
        camera.init();
        gamestate.init();
    }

    public void root() throws Exception {
        init();
        window.open();
        time.set(0);
        renderer.run();
    }

    public void registerKeyPressListener(Consumer<Integer> callback) {
        window.registerListener(new KeyEventHandler(callback));
    }

    public class KeyEventHandler extends GLFWKeyCallback {
        private final Consumer<Integer> handler;

        public KeyEventHandler(Consumer<Integer> handler) {
            this.handler = handler;
        }

        @Override
        public void invoke(long windowHandle, int keyCode, int scancode, int action, int mods) {
            if (keyCode < 0) return;
            if (action == GLFW_PRESS) {
                handler.accept(keyCode);
            }
        }
    }

    private class MouseButtonEventHandler extends GLFWMouseButtonCallback {
        @Override
        public void invoke(long windowHandle, int button, int action, int mods) {
            if (action == GLFW_PRESS) {
//                mousePressed(event);
            } else if (action == GLFW_RELEASE) {
//                mouseReleased(event);
            }
        }
    }

    private class MouseScrollEventHandler extends GLFWScrollCallback {
        @Override
        public void invoke(long windowHandle, double xScroll, double yScroll) {
            //
        }
    }
}
