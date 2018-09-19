package NG.Engine;

import NG.ActionHandling.GLFWListener;
import NG.Camera.Camera;
import NG.GameState.GameState;
import NG.Settings.Settings;

/**
 * @author Geert van Ieperen. Created on 16-9-2018.
 */
public interface Game {
    GameTimer timer();

    Camera camera();

    GameState getGamestate();

    RenderLoop getRenderer();

    Settings settings();

    GLFWWindow window();

    GLFWListener callbacks();

    boolean menuMode();

    Version getVersion();
}
