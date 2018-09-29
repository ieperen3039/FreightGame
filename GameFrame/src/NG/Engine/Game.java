package NG.Engine;

import NG.ActionHandling.GLFWListener;
import NG.Camera.Camera;
import NG.GameState.GameState;
import NG.Mods.Mod;
import NG.Rendering.GLFWWindow;
import NG.ScreenOverlay.Frames.SFrameManager;
import NG.ScreenOverlay.ScreenOverlay;
import NG.Settings.Settings;

import java.util.Collection;

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

    SFrameManager frameManager();

    Version getVersionNumber();

    Collection<Mod> modList();

    Mod getModByName(String name);
}
