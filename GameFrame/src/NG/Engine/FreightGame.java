package NG.Engine;

import NG.ActionHandling.FreightCallbacks;
import NG.ActionHandling.KeyMouseCallbacks;
import NG.Camera.Camera;
import NG.Camera.TycoonFixedCamera;
import NG.GameState.GameLoop;
import NG.GameState.GameMap;
import NG.GameState.GameState;
import NG.GameState.HeightMap;
import NG.Mods.Mod;
import NG.Mods.TypeCollection;
import NG.Rendering.GLFWWindow;
import NG.Rendering.RenderLoop;
import NG.ScreenOverlay.Frames.BaseLF;
import NG.ScreenOverlay.Frames.GUIManager;
import NG.ScreenOverlay.Frames.SFrameLookAndFeel;
import NG.ScreenOverlay.Frames.SFrameManager;
import NG.ScreenOverlay.Menu.FreightToolBar;
import NG.ScreenOverlay.Menu.MainMenu;
import NG.ScreenOverlay.ToolBar;
import NG.Settings.Settings;
import NG.Tools.Directory;
import NG.Tools.Logger;
import NG.Tracks.TrackMod;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A game of planning and making money.
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class FreightGame implements Game, ModLoader {
    private final GameTimer time;
    private final Camera camera;
    private final GameLoop gameState;
    private final GameMap gameMap;
    private final RenderLoop renderer;
    private final Settings settings;
    private final GLFWWindow window;
    private final FreightCallbacks inputHandler;
    private final GUIManager frameManager;
    private final TypeCollection typeCollection;
    private MainMenu mainMenu;

    private List<Mod> allMods;
    private List<Mod> activeMods = Collections.emptyList();

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

        camera = new TycoonFixedCamera(new Vector3f(), 100);
        window = new GLFWWindow(Settings.GAME_NAME, true);
        renderer = new RenderLoop(settings.TARGET_FPS);
        gameState = new GameLoop(Settings.GAME_NAME, settings.TARGET_TPS);
        gameMap = new HeightMap();
        inputHandler = new FreightCallbacks();
        frameManager = new SFrameManager();

        // load mods
        allMods = JarModReader.loadMods(Directory.mods);
        typeCollection = new TypeCollection();
    }

    public void init() throws Exception {
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
        mainMenu = new MainMenu(this, this, renderer::stopLoop);
        frameManager.addFrame(mainMenu);

        BaseLF lookAndFeel = new BaseLF();
        lookAndFeel.init(this);
        frameManager.setLookAndFeel(lookAndFeel);

        Logger.INFO.print("Finished initialisation\n");
    }

    @Override
    public void initMods(List<Mod> mods) {
        assert activeMods.isEmpty() : "Already mods loaded";
        activeMods = new ArrayList<>(mods);

        // init mods
        for (Mod mod : activeMods) {
            try {
                mod.init(this);
                identifyModule(mod);

            } catch (Exception ex) {
                Logger.ERROR.print("Error while loading " + mod.getModName(), ex);
            }
        }
    }

    private void identifyModule(Mod target) throws IllegalNumberOfModulesException {
        if (target instanceof TrackMod) {
            typeCollection.addTrackTypes((TrackMod) target);
            Logger.DEBUG.print("Installed " + target.getModName() + " as TrackMod");

        } else if (target instanceof SFrameLookAndFeel) {
            if (frameManager.hasLookAndFeel()) {
                throw new IllegalNumberOfModulesException(
                        "Tried installing " + target.getModName() + " while we already have a LookAndFeel Mod");
            }

            frameManager.setLookAndFeel((SFrameLookAndFeel) target);
            Logger.DEBUG.print("Installed " + target.getModName() + " as LookAndFeel mod");
        }
    }

    public void root() throws Exception {
        init();
        Logger.INFO.print("Starting game...\n");

        // show main menu
        mainMenu.setVisible(true);
        window.open();
        renderer.run();

        cleanMods();
        cleanup();
    }

    @Override
    public void startGame() {
        mainMenu.setVisible(false);
        ToolBar toolBar = new FreightToolBar(this, this::stopGame);
        frameManager.setToolBar(toolBar);
        gameState.start();
    }

    private void stopGame() {
        gameState.stopLoop();
        frameManager.setToolBar(null);
        mainMenu.setVisible(true);
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
    public KeyMouseCallbacks inputHandling() {
        return inputHandler;
    }

    @Override
    public Version getVersionNumber() {
        return new Version(0, 0);
    }

    @Override
    public TypeCollection objectTypes() {
        return typeCollection;
    }

    @Override
    public GUIManager gui() {
        return frameManager;
    }

    @Override
    public List<Mod> allMods() {
        return Collections.unmodifiableList(allMods);
    }

    public void doAfterGameTick(Runnable action) {
        gameState.defer(action);
    }

    @Override
    public Mod getModByName(String name) {
        for (Mod mod : allMods) {
            if (mod.getModName().equals(name)) {
                return mod;
            }
        }
        return null;
    }

    private void cleanup() {
        window.cleanup();
        renderer.cleanup();
        camera.cleanup();
        gameState.cleanup();
        inputHandler.cleanup();
    }

    @Override
    public void cleanMods() {
        activeMods.forEach(Mod::cleanup);
        activeMods.clear();
    }
}
