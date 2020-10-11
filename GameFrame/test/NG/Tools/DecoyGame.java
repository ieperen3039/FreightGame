package NG.Tools;

import NG.Camera.Camera;
import NG.Core.Game;
import NG.Core.GameTimer;
import NG.Core.PlayerStatus;
import NG.Core.Version;
import NG.GUIMenu.FrameManagers.UIFrameManager;
import NG.GameMap.GameMap;
import NG.GameState.GameState;
import NG.InputHandling.KeyControl;
import NG.InputHandling.MouseTool.MouseToolCallbacks;
import NG.Mods.TypeCollection;
import NG.Particles.GameParticles;
import NG.Rendering.GLFWWindow;
import NG.Rendering.Lights.GameLights;
import NG.Settings.Settings;

/**
 * @author Geert van Ieperen created on 28-4-2020.
 */
public class DecoyGame implements Game {
    @Override
    public GameTimer timer() {
        return null;
    }

    @Override
    public Camera camera() {
        return null;
    }

    @Override
    public GameState state() {
        return null;
    }

    @Override
    public GameMap map() {
        return null;
    }

    @Override
    public GameLights lights() {
        return null;
    }

    @Override
    public GameParticles particles() {
        return null;
    }

    @Override
    public Settings settings() {
        return null;
    }

    @Override
    public GLFWWindow window() {
        return null;
    }

    @Override
    public MouseToolCallbacks inputHandling() {
        return null;
    }

    @Override
    public UIFrameManager gui() {
        return null;
    }

    @Override
    public KeyControl keyControl() {
        return null;
    }

    @Override
    public Version getVersionNumber() {
        return null;
    }

    @Override
    public TypeCollection objectTypes() {
        return null;
    }

    @Override
    public void executeOnRenderThread(Runnable action) {
        action.run();
    }

    @Override
    public PlayerStatus playerStatus() {
        return null;
    }
}
