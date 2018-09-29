package NG.Engine;

import NG.ActionHandling.GLFWListener;
import NG.Camera.Camera;
import NG.GameState.GameMap;
import NG.GameState.GameState;
import NG.Mods.Mod;
import NG.Rendering.GLFWWindow;
import NG.ScreenOverlay.Frames.GUIManager;
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

    GameMap map();

    Settings settings();

    GLFWWindow window();

    GLFWListener callbacks();

    GUIManager gui();

    Version getVersionNumber();

    Collection<Mod> modList();

    Mod getModByName(String name);
}
