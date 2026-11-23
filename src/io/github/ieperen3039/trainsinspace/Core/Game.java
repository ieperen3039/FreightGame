package io.github.ieperen3039.trainsinspace.Core;

import io.github.ieperen3039.ngn.Core.Main;
import io.github.ieperen3039.ngn.Rendering.Lights.SceneLights;
import io.github.ieperen3039.ngn.UserInterface.FrameManagers.FrameManager;
import io.github.ieperen3039.trainsinspace.Content.Scenario.Scenario;
import io.github.ieperen3039.trainsinspace.GameMap.GameMap;
import io.github.ieperen3039.trainsinspace.GameState.GameState;
import io.github.ieperen3039.trainsinspace.Mods.TypeCollection;
import io.github.ieperen3039.trainsinspace.Particles.GameParticles;
import io.github.ieperen3039.trainsinspace.Settings.Settings;

/**
 * A collection of references to any major element of the game.
 * @author Geert van Ieperen. Created on 16-9-2018.
 */
public interface Game extends Main {
    @Override
    Settings settings();

    @Override
    FrameManager gui();

    SceneLights lights();

    GameState state();

    PlayerStatus playerStatus();

    GameMap map();

    GameParticles particles();

    TypeCollection objectTypes();

    void loadScenario(Scenario emptyScenario);
}
