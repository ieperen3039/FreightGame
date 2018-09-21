package NG.Engine;

import NG.ActionHandling.GLFWListener;
import NG.Camera.Camera;
import NG.Camera.PointCenteredCamera;
import NG.GameState.GameLoop;
import NG.GameState.GameState;
import NG.Mods.MapGeneratorMod;
import NG.Mods.Mod;
import NG.Mods.TrackMod;
import NG.Rendering.GLFWWindow;
import NG.Rendering.RenderLoop;
import NG.ScreenOverlay.Frames.ExampleSFrame;
import NG.ScreenOverlay.Frames.SFrame;
import NG.ScreenOverlay.Frames.SFrameLookAndFeel;
import NG.ScreenOverlay.Frames.SFrameManager;
import NG.ScreenOverlay.ScreenOverlay;
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
    private final GameLoop gameState;
    private final RenderLoop renderer;
    private final ScreenOverlay overlay;
    private final Settings settings;
    private final GLFWWindow window;
    private final GLFWListener inputHandler;
    private final SFrameManager frameManager;
    private List<Mod> mods;

    public FreightGame() throws IOException {
        Logger.INFO.print("Starting up the game engine...");
        Logger.DEBUG.print("General debug information: " +
                // manual aligning will do the trick
                "\n\tSystem OS:          " + System.getProperty("os.name") +
                "\n\tJava VM:            " + System.getProperty("java.runtime.version") +
                "\n\tFrame version:      " + getVersionNumber() +
                "\n\tWorking directory:  " + Directory.currentDirectory() +
                "\n\tMods directory:     " + Directory.mods.getPath()
        );

        // these two are not GameAspects, and thus the init() rule does not apply
        settings = new Settings();
        time = new GameTimer();

//        camera = new StaticCamera(Vectors.zeroVector(), Vectors.zVector());
        camera = new PointCenteredCamera(new Vector3f(20, 20, 20), Vectors.zeroVector());
        window = new GLFWWindow(Settings.GAME_NAME, true);
        renderer = new RenderLoop(settings.TARGET_FPS);
        overlay = new ScreenOverlay();
        gameState = new GameLoop(Settings.GAME_NAME, settings.TARGET_TPS);
        inputHandler = new GLFWListener();
        frameManager = new SFrameManager();

        // load mods
        mods = JarModReader.loadMods(Directory.mods);
    }

    private void init() throws Exception {
        Logger.DEBUG.print("Initializing fields...");
        // init all fields
        window.init(this);
        renderer.init(this);
        overlay.init(this);
        camera.init(this);
        gameState.init(this);
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

        SFrame frame = ExampleSFrame.get(this);
        frameManager.addFrame(frame);
        frame.show();
//        gameState.generateMap();

        Logger.INFO.print("Finished initialisation\n");
    }

    private void initMod(Mod mod) throws InvalidNumberOfModulesException, Version.MisMatchException {
        mod.init(this);

        if (mod instanceof TrackMod) {
//                 trackTypes.addAll(mod.getTypes()); or sth similar
            Logger.DEBUG.print("Installed " + mod.getModName() + " as TrackMod");

        } else if (mod instanceof SFrameLookAndFeel) {
            if (frameManager.getLookAndFeel() != null) {
                throw new InvalidNumberOfModulesException(
                        "Tried installing " + mod.getModName() + " while we already have a LookAndFeel Mod");
            }

            frameManager.setLookAndFeel((SFrameLookAndFeel) mod);
            Logger.DEBUG.print("Installed " + mod.getModName() + " as LookAndFeel mod");

        } else if (mod instanceof MapGeneratorMod) {
            if (gameState.hasMapGenerator()) {
                throw new InvalidNumberOfModulesException(
                        "Tried installing " + mod.getModName() + " while we already have a Map Generator Mod");
            }

            gameState.setMapGenerator((MapGeneratorMod) mod);
            Logger.DEBUG.print("Installed " + mod.getModName() + " as Map Generator");

        }
    }

    public void root() throws Exception {
        init();
        Logger.INFO.print("Starting game...\n");

        gameState.start();
        window.open();
        time.set(0);
        renderer.run();

        gameState.stopLoop();

        cleanup();
    }

    private void cleanup() {
        window.cleanup();
        renderer.cleanup();
        camera.cleanup();
        gameState.cleanup();
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
    public GameState state() {
        return gameState;
    }

    @Override
    public ScreenOverlay painter() {
        return overlay;
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
    public Version getVersionNumber() {
        return new Version(0, 0);
    }

    @Override
    public SFrameManager frameManager() {
        return frameManager;
    }

    @Override
    public void doAfterGameLoop(Runnable action) {
        gameState.defer(action);
    }

    private class InvalidNumberOfModulesException extends Exception {
        public InvalidNumberOfModulesException(String message) {
            super(message);
        }
    }
}
