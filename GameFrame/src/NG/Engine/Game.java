package NG.Engine;

import NG.ActionHandling.GLFWListener;
import NG.Camera.Camera;
import NG.GameState.GameState;
import NG.Rendering.GLFWWindow;
import NG.ScreenOverlay.Frames.SFrameManager;
import NG.ScreenOverlay.ScreenOverlay;
import NG.Settings.Settings;

/**
 *
 * @author Geert van Ieperen. Created on 16-9-2018.
 */
public interface Game {
    GameTimer timer();

    Camera camera();

    GameState state();

    ScreenOverlay painter();

    Settings settings();

    GLFWWindow window();

    GLFWListener callbacks();

    Version getVersionNumber();

    SFrameManager frameManager();

    /**
     * action is executed between two invocations of {@link AbstractGameLoop#update(float)} of the game loop.
     * @param action an action that would otherwise interfere with the gameloop.
     */
    void doAfterGameLoop(Runnable action);
}
