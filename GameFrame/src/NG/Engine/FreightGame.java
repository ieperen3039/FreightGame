package NG.Engine;

import NG.ActionHandling.GLFWListener;
import NG.Camera.Camera;
import NG.Camera.PointCenteredCamera;
import NG.DataStructures.MatrixStack.SGL;
import NG.Entities.Entity;
import NG.GameState.GameLoop;
import NG.GameState.GameMap;
import NG.GameState.GameState;
import NG.GameState.HeightMap;
import NG.Mods.Mod;
import NG.Rendering.GLFWWindow;
import NG.Rendering.RenderLoop;
import NG.ScreenOverlay.Frames.BaseLF;
import NG.ScreenOverlay.Frames.GUIManager;
import NG.ScreenOverlay.Frames.SFrameLookAndFeel;
import NG.ScreenOverlay.Frames.SFrameManager;
import NG.ScreenOverlay.MainMenu;
import NG.ScreenOverlay.ToolBar;
import NG.Settings.Settings;
import NG.Tools.Directory;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import NG.Tracks.BuildMenu;
import NG.Tracks.TrackMod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
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
    private final GLFWListener inputHandler;
    private final GUIManager frameManager;
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

//        camera = new StaticCamera(Vectors.zeroVector(), Vectors.zVector());
        camera = new PointCenteredCamera(new Vector3f(20, 20, 20), Vectors.zeroVector());
        window = new GLFWWindow(Settings.GAME_NAME, true);
        renderer = new RenderLoop(settings.TARGET_FPS);
        gameState = new GameLoop(Settings.GAME_NAME, settings.TARGET_TPS);
        gameMap = new HeightMap();
        inputHandler = new GLFWListener();
        frameManager = new SFrameManager();

        // load mods
        allMods = JarModReader.loadMods(Directory.mods);
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

        inputHandler.onMouseButtonClick(this::onClick);

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
        activeMods = mods;

        // init mods
        for (Mod mod : mods) {
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
            //                 trackTypes.addAll(mod.getTypes()); or sth similar
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

    @Override
    public void cleanMods() {
        activeMods.forEach(Mod::cleanup);
        activeMods.clear();
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

    private void cleanup() {
        window.cleanup();
        renderer.cleanup();
        camera.cleanup();
        gameState.cleanup();
        inputHandler.cleanup();
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
    public List<Mod> allMods() {
        return Collections.unmodifiableList(allMods);
    }

    public void doAfterGameLoop(Runnable action) {
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

    @Override
    public void startGame() {
        mainMenu.setVisible(false);
        ToolBar toolBar = new ToolBar(this);
        toolBar.addButton("Exit", this::stopGame);
        toolBar.addButton("$$$", () -> Logger.INFO.print("( $$ _ $$) =8|$$$|"));
        toolBar.addSeparator();
        toolBar.addButton("B", () -> frameManager.addFrame(new BuildMenu()));
        frameManager.setToolBar(toolBar);
        gameState.start();
    }

    private void stopGame() {
        gameState.stopLoop();
        frameManager.setToolBar(null);
        mainMenu.setVisible(true);
    }

    private void onClick(int button, int x, int y) {
        if (frameManager.processClick(button, x, y)) return;
        if (gameState.processClick(button, x, y)) return;

        Matrix4f projection = SGL.getProjection(window.getWidth(), window.getHeight(), camera, Settings.ISOMETRIC_VIEW);
        Vector4f from = projection.transform(new Vector4f(x, y, 1, 0));
        Vector4f to = projection.transform(new Vector4f(x, y, -1, 0));

        Entity target = gameState.getEntityByRay(from, to);
        if (target != null) target.onClick(button);
    }
}
