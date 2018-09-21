package NG.Engine;

import NG.ActionHandling.GLFWListener;
import NG.Camera.Camera;
import NG.Camera.PointCenteredCamera;
import NG.GameState.GameState;
import NG.Mods.MapGeneratorMod;
import NG.Mods.Mod;
import NG.Mods.TrackMod;
import NG.ScreenOverlay.Frames.SFrameManager;
import NG.Settings.Settings;
import NG.Tools.Directory;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.List;

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
    private final SFrameManager frameManager;
    private List<Mod> mods;

    private MapGeneratorMod mapGenerator;

    public FreightGame() throws IOException {
        Logger.INFO.print("Starting the game...");
        Logger.DEBUG.print("General debug information: " +
                // manual aligning will do the trick
                "\n\tSystem OS:          " + System.getProperty("os.name") +
                "\n\tJava VM:            " + System.getProperty("java.runtime.version") +
                "\n\tFrame version:      " + getVersion() +
                "\n\tWorking directory:  " + Directory.currentDirectory() +
                "\n\tMods directory:     " + Directory.mods.getPath()
        );

        settings = new Settings();
        time = new GameTimer();

//        camera = new StaticCamera(Vectors.zeroVector(), Vectors.zVector());
        camera = new PointCenteredCamera(new Vector3f(20, 20, 20), Vectors.zeroVector());
        window = new GLFWWindow(Settings.GAME_NAME, true);
        renderer = new RenderLoop(settings.TARGET_FPS);
        gamestate = new GameState();
        inputHandler = new GLFWListener();
        frameManager = new SFrameManager();

        // load mods
        mods = JarModReader.loadMods(Directory.mods);
    }

    private void init() throws Exception {
        // init all fields
        window.init(this);
        renderer.init(this);
        camera.init(this);
        gamestate.init(this);
        inputHandler.init(this);
        frameManager.init(this);

        // init mods
        for (Mod mod : mods) {
            try {
                initMod(mod);

            } catch (Version.MisMatchException | InvalidNumberOfModulesException ex) {
                Logger.ERROR.print("Error while loading " + mod.getModName(), ex);
            }
        }

        Logger.INFO.print("Finished initialisation!\n");
    }

    private void initMod(Mod mod) throws InvalidNumberOfModulesException, Version.MisMatchException {
        mod.init(this);

        if (mod instanceof TrackMod) {
//                 trackTypes.addAll(mod.getTypes()); or sth similar

        } else if (mod instanceof MapGeneratorMod) {
            if (mapGenerator != null) throw new InvalidNumberOfModulesException(
                    "Tried loading " + mod.getModName() + " while we already have a Map Generator Mod: " + mapGenerator);

            mapGenerator = (MapGeneratorMod) mod;
        }
    }

    public void root() throws Exception {
        Logger.INFO.print("Starting game...");
        init();

        window.open();
        time.set(0);
        renderer.run();

        cleanup();
    }

    private void cleanup() {
        window.cleanup();
        renderer.cleanup();
        camera.cleanup();
        gamestate.cleanup();
        inputHandler.cleanup();
        mods.forEach(Mod::cleanup);
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

    @Override
    public Version getVersion() {
        return new Version(0, 0);
    }

    @Override
    public SFrameManager getFrameManager() {
        return frameManager;
    }

    private class InvalidNumberOfModulesException extends Exception {
        public InvalidNumberOfModulesException(String message) {
            super(message);
        }
    }
}
