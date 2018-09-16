package NG.Engine;

import NG.ActionHandling.GLFWListener;
import NG.Camera.Camera;
import NG.Camera.StaticCamera;
import NG.GameState.GameState;
import NG.Settings.Settings;
import NG.Tools.Vectors;

/**
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class FreightGame implements Game {
    private final GameTimer time;
    private final Camera camera;
    private final GameState gamestate;
    private final RenderLoop renderer;
    private final Settings settings;
    private final GLFWWindow window;
    private final GLFWListener inputHandler;

    public FreightGame() {
        settings = new Settings();
        time = new GameTimer();

        camera = new StaticCamera(Vectors.zeroVector(), Vectors.zVector());
        window = new GLFWWindow(settings.GAME_NAME, true);
        renderer = new RenderLoop(30);
        gamestate = new GameState();
        inputHandler = new GLFWListener();
    }

    private void init() throws Exception {
        // init all fields
        window.init(this);
        renderer.init(this);
        camera.init(this);
        gamestate.init(this);
        inputHandler.init(this);
    }

    public void root() throws Exception {
        init();
        window.open();
        time.set(0);
        renderer.run();
    }

    @Override
    public GameTimer timer() {
        return time;
    }

    @Override
    public Camera camera() {
        return camera;
    }

    @Override
    public GameState getGamestate() {
        return gamestate;
    }

    @Override
    public RenderLoop getRenderer() {
        return renderer;
    }

    @Override
    public Settings settings() {
        return settings;
    }

    @Override
    public GLFWWindow window() {
        return window;
    }

    @Override
    public GLFWListener callbacks() {
        return inputHandler;
    }

    @Override
    public boolean menuMode() {
        return time.isPaused();
    }
}
