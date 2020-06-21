package NG.Content;

import NG.Camera.Camera;
import NG.Core.Game;
import NG.Core.ModLoader;
import NG.GameMap.DefaultMapGenerator;
import NG.GameMap.MapGeneratorMod;
import NG.Mods.Mod;
import NG.Settings.Settings;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;

/**
 * @author Geert van Ieperen created on 19-6-2020.
 */
public abstract class Scenario {
    public static final int X_SIZE = 100;
    public static final int Y_SIZE = 100;

    private final ModLoader modLoader;

    public Scenario(ModLoader modLoader) {
        this.modLoader = modLoader;
    }

    public void apply(Game game) {
        Settings settings = game.settings();

        // load mods
        List<Mod> modsToLoad = getMods(modLoader);
        modLoader.initMods(modsToLoad);

        // set elements
        setMap(game, settings);
        setEntities(game, settings);
        setCamera(game, settings);
        setLight(game, settings);

        // start
        modLoader.startGame();
    }

    /**
     * @return a list of all mods used in this scenario
     */
    protected abstract List<Mod> getMods(ModLoader modLoader);

    protected void setMap(Game game, Settings settings) {
        // random map
        MapGeneratorMod mapGenerator = new DefaultMapGenerator(0);
        mapGenerator.setSize(X_SIZE, Y_SIZE);
        mapGenerator.setProperty(DefaultMapGenerator.MAJOR_AMPLITUDE, 100);
        mapGenerator.setProperty(DefaultMapGenerator.MINOR_AMPLITUDE, 0);
        game.map().generateNew(game, mapGenerator);
    }

    /**
     * set all entities in this scenario. The default does nothing
     */
    protected void setEntities(Game game, Settings settings) {

    }

    /**
     * set the camera to the initial position. The default centers the camera on the map
     */
    protected void setCamera(Game game, Settings settings) {
        // set camera to middle of map
        Camera cam = game.camera();
        Vector2f size = game.map().getSize();
        Vector3f cameraFocus = new Vector3f(size.x / 2, size.y / 2, 0);
        Vector3f cameraEye = new Vector3f(cameraFocus).add(-20, -20, 20);
        cam.set(cameraFocus, cameraEye);
    }

    /**
     * set the lights on this map. The default installs one sunlight element
     */
    protected void setLight(Game game, Settings settings) {
        // add light
        game.lights().addDirectionalLight(
                new Vector3f(1, 1.5f, 0.5f), settings.SUNLIGHT_COLOR, settings.SUNLIGHT_INTENSITY
        );
    }

    public static class Empty extends Scenario {
        public Empty(ModLoader loader) {
            super(loader);
        }

        @Override
        protected List<Mod> getMods(ModLoader modLoader) {
            return modLoader.allMods();
        }
    }
}
