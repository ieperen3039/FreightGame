package NG.Engine;

import NG.ActionHandling.KeyMouseCallbacks;
import NG.Camera.Camera;
import NG.GameState.GameMap;
import NG.GameState.GameState;
import NG.Rendering.GLFWWindow;
import NG.ScreenOverlay.Frames.GUIManager;
import NG.Settings.Settings;

/**
 *
 * @author Geert van Ieperen. Created on 16-9-2018.
 */
public interface Game {

    GameTimer timer();

    Camera camera();

    GameState state();

    GameMap map();

    Settings settings();

    GLFWWindow window();

    KeyMouseCallbacks callbacks();

    GUIManager gui();

    Version getVersionNumber();
}
