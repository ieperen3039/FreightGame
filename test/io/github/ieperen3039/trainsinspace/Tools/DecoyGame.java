package io.github.ieperen3039.trainsinspace.Tools;

import io.github.ieperen3039.trainsinspace.Camera.Camera;
import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.Core.GameTimer;
import io.github.ieperen3039.trainsinspace.Core.PlayerStatus;
import io.github.ieperen3039.trainsinspace.Core.Version;
import io.github.ieperen3039.trainsinspace.GUIMenu.FrameManagers.UIFrameManager;
import io.github.ieperen3039.trainsinspace.GameMap.GameMap;
import io.github.ieperen3039.trainsinspace.GameState.GameState;
import io.github.ieperen3039.trainsinspace.InputHandling.KeyControl;
import io.github.ieperen3039.trainsinspace.InputHandling.MouseTool.MouseToolCallbacks;
import io.github.ieperen3039.trainsinspace.Mods.TypeCollection;
import io.github.ieperen3039.trainsinspace.Particles.GameParticles;
import io.github.ieperen3039.trainsinspace.Rendering.GLFWWindow;
import io.github.ieperen3039.trainsinspace.Rendering.Lights.GameLights;
import io.github.ieperen3039.trainsinspace.Settings.Settings;

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
