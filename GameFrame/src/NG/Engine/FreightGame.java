package NG.Engine;

import NG.ActionHandling.GLFWListener;
import NG.Camera.Camera;
import NG.Camera.PointCenteredCamera;
import NG.GameState.*;
import NG.Mods.Mod;
import NG.Rendering.GLFWWindow;
import NG.Rendering.RenderLoop;
import NG.ScreenOverlay.Frames.GUIManager;
import NG.ScreenOverlay.Frames.SFrameLookAndFeel;
import NG.ScreenOverlay.Frames.SFrameManager;
import NG.ScreenOverlay.MainMenu;
import NG.Settings.Settings;
import NG.Tools.Directory;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import NG.Tracks.TrackMod;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class FreightGame implements Game {
    private final GameTimer time;
    private final Camera camera;
    private final GameLoop gameState;
    private final GameMap gameMap;
    private final RenderLoop renderer;
    private final Settings settings;
    private final GLFWWindow window;
    private final GLFWListener inputHandler;
    private final GUIManager frameManager;

    private List<Mod> mods;
    private MapGeneratorMod mapGenerator;
    private MainMenu mainMenu;

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

        // these two are not GameAspects, and thus the init() rule does not apply.
        settings = new Settings();
        time = new GameTimer();

//        camera = new StaticCamera(Vectors.zeroVector(), Vectors.zVector());
        camera = new PointCenteredCamera(new Vector3f(20, 20, 20), Vectors.zeroVector());
        window = new GLFWWindow(Settings.GAME_NAME, true);
        renderer = new RenderLoop(settings.TARGET_FPS);
        gameState = new GameLoop(Settings.GAME_NAME, settings.TARGET_TPS);
        gameMap = new HeightMap();
        inputHandler = new GLFWListener();
        frameManager = new SFrameManager();

        // load mods
        mods = JarModReader.loadMods(Directory.mods);
    }

    private void init() throws Exception {
        Logger.DEBUG.print("Initializing...");
        // init all fields
        window.init(this);
        renderer.init(this);
        camera.init(this);
        gameState.init(this);
        inputHandler.init(this);
        frameManager.init(this);
        gameMap.init(this);

        renderer.addHudItem(frameManager::draw);
        mainMenu = new MainMenu(this, renderer::stopLoop);
        frameManager.addFrame(mainMenu);

        // init mods
        for (Mod mod : mods) {
            try {
                initMod(mod);

            } catch (Version.MisMatchException | InvalidNumberOfModulesException ex) {
                Logger.ERROR.print("Error while loading " + mod.getModName(), ex);
            }
        }

        if (!frameManager.hasLookAndFeel())
            throw new InvalidNumberOfModulesException("No LookAndFeel mod has been supplied");

        Logger.INFO.print("Finished initialisation\n");
    }

    private void initMod(Mod mod) throws InvalidNumberOfModulesException, Version.MisMatchException {
        mod.init(this);

        if (mod instanceof TrackMod) {
//                 trackTypes.addAll(mod.getTypes()); or sth similar
            Logger.DEBUG.print("Installed " + mod.getModName() + " as TrackMod");

        } else if (mod instanceof SFrameLookAndFeel) {
            if (frameManager.hasLookAndFeel()) {
                throw new InvalidNumberOfModulesException(
                        "Tried installing " + mod.getModName() + " while we already have a LookAndFeel Mod");
            }

            frameManager.setLookAndFeel((SFrameLookAndFeel) mod);
            Logger.DEBUG.print("Installed " + mod.getModName() + " as LookAndFeel mod");

        }
    }

    public void root() throws Exception {
        init();
        Logger.INFO.print("Starting game...\n");

        // show main menu
        mainMenu.setVisible(true);
        window.open();
        renderer.run();

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
    public GameMap map() {
        return gameMap;
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
    public GUIManager gui() {
        return frameManager;
    }

    @Override
    public Collection<Mod> modList() {
        return Collections.unmodifiableList(mods);
    }

    public void doAfterGameLoop(Runnable action) {
        gameState.defer(action);
    }

    @Override
    public Mod getModByName(String name) {
        for (Mod mod : mods) {
            if (mod.getModName().equals(name)) {
                return mod;
            }
        }
        return null;
    }

    private class InvalidNumberOfModulesException extends Exception {
        public InvalidNumberOfModulesException(String message) {
            super(message);
        }
    }
}
