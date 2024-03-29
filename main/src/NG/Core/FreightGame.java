package NG.Core;

import NG.Camera.Camera;
import NG.Camera.TycoonFixedCamera;
import NG.Entities.Entity;
import NG.GUIMenu.FrameManagers.FrameManagerImpl;
import NG.GUIMenu.FrameManagers.UIFrameManager;
import NG.GameMap.GameMap;
import NG.GameMap.HeightMap;
import NG.GameState.GameLoop;
import NG.GameState.GameState;
import NG.InputHandling.ClickShader;
import NG.InputHandling.KeyControl;
import NG.InputHandling.MouseTool.MouseToolCallbacks;
import NG.Menu.InGame.FreightGameUI;
import NG.Menu.Main.MainMenu;
import NG.Mods.InitialisationMod;
import NG.Mods.Mod;
import NG.Mods.SoftMod;
import NG.Mods.TypeCollection;
import NG.Particles.GameParticles;
import NG.Particles.ParticleShader;
import NG.Rendering.GLFWWindow;
import NG.Rendering.Lights.GameLights;
import NG.Rendering.Lights.SingleShadowMapLights;
import NG.Rendering.RenderLoop;
import NG.Rendering.Shaders.BlinnPhongShader;
import NG.AssetHandling.Asset;
import NG.Settings.Settings;
import NG.Tools.Directory;
import NG.Tools.Logger;
import org.joml.Vector3f;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A game of planning and making money.
 * <p>
 * This class initializes all gameAspects, allow for starting a game, loading mods and cleaning up afterwards. It
 * provides all aspects of the game engine through the {@link Game} interface.
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class FreightGame implements Game, ModLoader {
    public static final File SAVE_FILE = Directory.savedGames.getFile("test.sav");
    private static final Version GAME_VERSION = new Version(0, 0);

    public final RenderLoop renderer;
    private final GLFWWindow window;
    private final MouseToolCallbacks inputHandler;
    private final KeyControl keyControl;

    private Thread mainThread;
    private GameLoop gameState;
    private GameTimer time;
    private Camera gameCamera;
    private GameMap gameMap;
    private GameLights gameLights;
    private GameParticles gameParticles;
    private Settings settings;
    private UIFrameManager frameManager;
    private ClickShader clickShader;
    private PlayerStatus progress;
    private MainMenu mainMenu;

    private TypeCollection typeCollection;

    private List<Mod> allMods;
    private List<Mod> activeMods = Collections.emptyList();
    private List<Mod> permanentMods;

    public FreightGame() throws IOException {
        Logger.INFO.print("Starting up the game engine...");

        Logger.INFO.print("General debug information: " +
                // manual aligning will do the trick
                "\n\tSystem OS:          " + System.getProperty("os.name") +
                "\n\tJava VM:            " + System.getProperty("java.runtime.version") +
                "\n\tGame version:       " + getVersionNumber() +
                "\n\tWorking directory:  " + Directory.workDirectory() +
                "\n\tMods directory:     " + Directory.hardMods.getPath()
        );

        // these are not GameAspects, and thus the init() rule does not apply.
        settings = new Settings();
        time = GameTimer.create(settings.TARGET_FPS, settings.TARGET_TPS);
//        time = new FixedTimer(0, settings.TARGET_TPS); // for breakpoints only

        GLFWWindow.Settings videoSettings = new GLFWWindow.Settings(settings);
        window = new GLFWWindow(Settings.GAME_NAME, videoSettings, true);
        clickShader = new ClickShader();
        gameMap = new HeightMap();

        gameCamera = new TycoonFixedCamera(new Vector3f(), 100, 100);
        renderer = new RenderLoop(settings.TARGET_FPS);
        gameState = new GameLoop(settings.TARGET_TPS, clickShader);
        gameLights = new SingleShadowMapLights();
        gameParticles = new GameParticles();
        inputHandler = new MouseToolCallbacks();
        keyControl = inputHandler.getKeyControl();
        frameManager = new FrameManagerImpl();
        progress = new PlayerStatus();
        mainThread = Thread.currentThread();

        // load mods
        allMods = JarModReader.loadMods(Directory.hardMods);
        for (File file : Directory.softMods.getFiles()) {
            try {
                SoftMod mod = new SoftMod(file.toPath());
                allMods.add(mod);

            } catch (IOException ex) {
                Logger.WARN.print("Error loading soft mod " + file);
                Logger.ERROR.print(ex);
            }
        }
    }

    /**
     * start all elements required for showing the main frame of the game.
     * @throws Exception when the initialisation fails.
     */
    public void init() throws Exception {
        Logger.DEBUG.print("Initializing...");
        // init all fields
        gameCamera.init(this);
        renderer.init(this);
        gameState.init(this);
        gameLights.init(this);
        gameParticles.init(this);
        inputHandler.init(this);
        frameManager.init(this);
        progress.init(this);

        permanentMods = JarModReader.filterInitialisationMods(allMods, this);

        renderer.renderSequence(new BlinnPhongShader())
                .add((gl, game) -> game.lights().draw(gl))
                .add((gl, game) -> game.map().draw(gl))
                .add((gl, game) -> game.state().draw(gl))
                .add((gl, game) -> game.inputHandling().getMouseTool().draw(gl));
        // particles
        renderer.renderSequence(new ParticleShader())
                .add((gl, game) -> game.particles().draw(gl));
        // click shader
        renderer.renderSequence(clickShader)
                .add((gl, game) -> game.state().draw(gl));
        // GUIs
        renderer.addHudItem(frameManager::draw);

        mainMenu = new MainMenu(this, this, renderer::stopLoop);
        frameManager.addFrameCenter(mainMenu, window);

        gameState.start();

        Logger.printOnline(() -> String.format("%4d resources active", Asset.getNrOfActiveResources()));

        Logger.INFO.print("Finished initialisation\n");
    }

    @Override
    public void initMods(List<Mod> mods) {
        assert activeMods.isEmpty() : "Already mods loaded";
        activeMods = new ArrayList<>(mods);
        typeCollection = new TypeCollection();

        // init mods
        for (Mod mod : activeMods) {
            assert mod != null : activeMods;

            try {
                assert !(mod instanceof InitialisationMod) : "Init mods should not be loaded here";

                mod.init(this);

            } catch (Exception ex) {
                Logger.ERROR.print("Error while loading " + mod.getModName(), ex);
                mods.remove(mod);
            }
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
        frameManager.removeElement(mainMenu);
        mainMenu = null;
        frameManager.setMainGUI(new FreightGameUI(this, this));

        gameState.unPause();
    }

    @Override
    public void stopGame() {
        gameState.pause();
        gameState.cleanup();
        gameMap.cleanup();
        cleanMods();

        frameManager.clear();

        mainMenu = new MainMenu(this, this, renderer::stopLoop);
        frameManager.addFrameCenter(mainMenu, window);
    }

    public void saveGame(File target) {
        gameState.pause();
        while (!gameState.isPaused()) Thread.yield();

        try (
                FileOutputStream fileOut = new FileOutputStream(target);
                ObjectOutputStream out = new ObjectOutputStream(fileOut)
        ) {
            out.writeUTF(GAME_VERSION.toString());

            // mods
            out.writeInt(activeMods.size());
            for (Mod mod : activeMods) {
                out.writeUTF(mod.getModName());
            }

            // game aspects
            out.writeObject(settings);
            out.writeObject(time);
            out.writeObject(gameCamera);
            out.writeObject(gameMap);
            out.writeObject(gameLights);
            out.writeObject(gameParticles);

            gameState.writeTo(out);

            out.writeObject(progress);

            out.writeUTF("END");

            Logger.INFO.print("Game has been saved to " + Directory.workDirectory().relativize(target.toPath()));

        } catch (IOException ex) {
            Logger.ERROR.print(ex);

        } finally {
            gameState.unPause();
        }
    }

    public void loadGame(File target) {
        gameState.pause();
        while (!gameState.isPaused()) Thread.yield();

        try (
                FileInputStream fileIn = new FileInputStream(target);
                ObjectInputStream in = new ObjectInputStream(fileIn)
        ) {
            String version = in.readUTF();
            Logger.INFO.printf("Reading game file of version %s (current is %s)", version, GAME_VERSION);

            // mods
            cleanMods();
            int nrOfActiveMods = in.readInt();
            List<Mod> modsToLoad = new ArrayList<>(nrOfActiveMods);
            for (int i = 0; i < nrOfActiveMods; i++) {
                String modName = in.readUTF();
                modsToLoad.add(getModByName(modName));
            }
            initMods(modsToLoad);

            // game aspects
            settings = (Settings) in.readObject();
            time = (GameTimer) in.readObject();
            Camera camera = (Camera) in.readObject();
            GameMap map = (GameMap) in.readObject();
            GameLights lights = (GameLights) in.readObject();
            GameParticles particles = (GameParticles) in.readObject();

            gameState.readFrom(in);

            progress = (PlayerStatus) in.readObject();

            String tail = in.readUTF();
            if (!tail.equals("END")) throw new IOException("End of save file does not align");

            // init and write
            lights.init(this);
            particles.init(this);
            progress.init(this);
            camera.init(this);

            gameCamera = camera;
            gameMap = map;
            gameLights = lights;
            gameParticles = particles;

            for (Entity entity : gameState) {
                entity.restore(this);
            }

            Logger.INFO.print("Game state has been loaded");
            startGame();

        } catch (Exception ex) {
            Logger.ERROR.print(ex);

        } finally {
            gameState.unPause();
        }
    }

    @Override
    public GameTimer timer() {
        return time;
    }

    @Override
    public Camera camera() {
        return gameCamera;
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
    public GameLights lights() {
        return gameLights;
    }

    @Override
    public GameParticles particles() {
        return gameParticles;
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
    public MouseToolCallbacks inputHandling() {
        return inputHandler;
    }

    @Override
    public KeyControl keyControl() {
        return keyControl;
    }

    @Override
    public Version getVersionNumber() {
        return GAME_VERSION;
    }

    @Override
    public TypeCollection objectTypes() {
        return typeCollection;
    }

    @Override
    public PlayerStatus playerStatus() {
        return progress;
    }

    @Override
    public void executeOnRenderThread(Runnable action) {
        if (Thread.currentThread() == mainThread) {
            action.run();
        } else {
            renderer.defer(action);
        }
    }

    @Override
    public UIFrameManager gui() {
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
        gameState.stopLoop();
        permanentMods.forEach(Mod::cleanup);

        gameCamera.cleanup();
        gameState.cleanup();
        gameMap.cleanup();
        gameLights.cleanup();
        gameParticles.cleanup();
        inputHandler.cleanup();
        frameManager.cleanup();
        clickShader.cleanup();
        progress.cleanup();

        renderer.cleanup();
        window.cleanup();
    }

    @Override
    public void cleanMods() {
        activeMods.forEach(Mod::cleanup);
        activeMods.clear();
        typeCollection = null;
    }
}
