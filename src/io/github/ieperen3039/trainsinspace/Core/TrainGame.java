package io.github.ieperen3039.trainsinspace.Core;

import io.github.ieperen3039.ngn.AssetHandling.Asset;
import io.github.ieperen3039.ngn.Camera.Camera;
import io.github.ieperen3039.ngn.DataStructures.Generic.Color4f;
import io.github.ieperen3039.ngn.InputHandling.KeyControl;
import io.github.ieperen3039.ngn.InputHandling.MouseTools.MouseToolCallbacks;
import io.github.ieperen3039.ngn.InputHandling.MouseTools.UISelector;
import io.github.ieperen3039.ngn.Rendering.GLFWWindow;
import io.github.ieperen3039.ngn.Rendering.Lights.SceneLights;
import io.github.ieperen3039.ngn.Rendering.Lights.SingleShadowMapLights;
import io.github.ieperen3039.ngn.Rendering.RenderLoop;
import io.github.ieperen3039.ngn.Rendering.Shaders.BPShadowShader;
import io.github.ieperen3039.ngn.Rendering.Shaders.ClickShader;
import io.github.ieperen3039.ngn.Rendering.Shaders.ClickShaderTarget;
import io.github.ieperen3039.ngn.Tools.*;
import io.github.ieperen3039.ngn.UserInterface.FrameManagers.FrameManager;
import io.github.ieperen3039.ngn.UserInterface.FrameManagers.FrameManagerImpl;
import io.github.ieperen3039.ngn.Version;
import io.github.ieperen3039.trainsinspace.Camera.TycoonFixedCamera;
import io.github.ieperen3039.trainsinspace.GameMap.GameMap;
import io.github.ieperen3039.trainsinspace.GameMap.HeightMap;
import io.github.ieperen3039.trainsinspace.GameMap.MapSelector;
import io.github.ieperen3039.trainsinspace.GameState.GameLoop;
import io.github.ieperen3039.trainsinspace.GameState.GameState;
import io.github.ieperen3039.trainsinspace.Menu.InGame.TrainGameUI;
import io.github.ieperen3039.trainsinspace.Menu.Main.MainMenu;
import io.github.ieperen3039.trainsinspace.Mods.InitialisationMod;
import io.github.ieperen3039.trainsinspace.Mods.Mod;
import io.github.ieperen3039.trainsinspace.Mods.SoftMod;
import io.github.ieperen3039.trainsinspace.Mods.TypeCollection;
import io.github.ieperen3039.trainsinspace.Particles.GameParticles;
import io.github.ieperen3039.trainsinspace.Particles.ParticleShader;
import io.github.ieperen3039.trainsinspace.Settings.Settings;
import io.github.ieperen3039.trainsinspace.Tools.Directory;
import io.github.ieperen3039.trainsinspace.Tracks.TrackTypeBase;
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
public class TrainGame implements Game, ModLoader {
    public static final File SAVE_FILE = Directory.savedGames.getFile("test.sav");
    private static final Version GAME_VERSION = new Version(0, 0);

    public final RenderLoop renderer;
    private final GLFWWindow window;
    private final MouseToolCallbacks inputHandler;
    private final KeyControl keyControl;

    private Thread mainThread;
    private GameLoop gameState;
    private TickTimeTimer time;
    private Camera gameCamera;
    private GameMap gameMap;
    private SingleShadowMapLights gameLights;
    private GameParticles gameParticles;
    private Settings settings;
    private FrameManagerImpl frameManager;
    private ClickShader clickShader;
    private PlayerStatus progress;
    private MainMenu mainMenu;

    private TypeCollection typeCollection;

    private List<Mod> allMods = new ArrayList<>();
    private List<Mod> activeMods = Collections.emptyList();
    private List<Mod> permanentMods;

    public TrainGame() throws IOException {
        Logger.INFO.print("Starting up the game engine...");

        Logger.INFO.print("General debug information: " +
                // manual aligning will do the trick
                "\n\tSystem OS:          " + System.getProperty("os.name") +
                "\n\tJava VM:            " + System.getProperty("java.runtime.version") +
                "\n\tGame version:       " + getVersionNumber() +
                "\n\tWorking directory:  " + Directory.workDirectory() +
                "\n\tMods directory:     " + Directory.mods.getPath()
        );

        settings = new Settings();
        settings.TITLE = "Trains in SPAAAACE";
        settings.BACKGROUND_COLOR = Color4f.BLACK;
        settings.PRINT_ROLL = true;
        settings.PRINT_ROLL_COLOR = Color4f.WHITE;
        settings.Z_NEAR = 1f;
        settings.Z_FAR = 250;
        settings.MAX_CAMERA_DIST = settings.Z_FAR / 2f;
        settings.MIN_CAMERA_DIST = settings.Z_NEAR * 2f;
        Logger.doPrintCallsites = true;

        time = new RealTimeTimer();
//        time = new FixedTimer(0, settings.TARGET_TPS); // for breakpoints only

        window = new GLFWWindow(settings.TITLE, new GLFWWindow.Settings(settings));

        ClickShaderTarget clickShaderTarget = new ClickShaderTarget(window);
        clickShader = new ClickShader(clickShaderTarget);
        gameMap = new HeightMap();

        gameCamera = new TycoonFixedCamera(new Vector3f(), 100, 100);
        renderer = new RenderLoop(settings.TARGET_FPS);
        gameState = new GameLoop(settings.TARGET_TPS, clickShaderTarget);
        gameLights = new SingleShadowMapLights();
        gameParticles = new GameParticles();
        inputHandler = new MouseToolCallbacks();
        keyControl = inputHandler.getKeyControl();
        frameManager = new FrameManagerImpl();
        progress = new PlayerStatus();
        typeCollection = new TypeCollection();
        mainThread = Thread.currentThread();

        inputHandler.setSelectionLayers(
                gameState,
                new MapSelector(this, gameMap),
                new UISelector(frameManager)
        );

        // load mods
        File[] files = Directory.mods.getFiles();
        if (files.length == 0) {
            Logger.INFO.print("No mods found");
        }
        for (File modDir : files) {
            try {
                File description = new File(modDir.getPath(), "description.json");
                if (!description.exists()) continue;

                SoftMod mod = new SoftMod(modDir.toPath());
                Logger.INFO.print("Found mod " + mod.name + " " + mod.version);
                allMods.add(mod);

            } catch (IOException ex) {
                Logger.WARN.print("Error loading soft mod " + modDir);
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
        frameManager.init(window, this, renderer.getFontLoader());
        progress.init(this);

        permanentMods = JarModReader.filterInitialisationMods(allMods, this);
        gameLights.addDirectionalLight(Vectors.Z, Color4f.BLACK, 0);

        renderer.renderSequence(new BPShadowShader())
                .add(gameLights::draw)
                .add(gameMap::draw)
                .add(gameState::draw);
        // particles
        renderer.renderSequence(new ParticleShader(settings.PARTICLE_SIZE))
                .add(gameParticles::draw);
        // click shader
        renderer.renderSequence(clickShader)
                .add(gameState::draw);
        // GUIs
        renderer.addHudItem(frameManager::draw);

        mainMenu = new MainMenu(this, this, renderer::initiateStop);
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
        typeCollection.trackTypes.add(new TrackTypeBase());

        // init mods
        for (Mod mod : activeMods) {
            assert mod != null : activeMods;

            try {
                assert !(mod instanceof InitialisationMod) : "Init mods should not be loaded here";

                Logger.DEBUG.print("Loading mod " + mod.getModName());
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
        mainMenu.dispose();
        mainMenu = null;
        frameManager.setMainPanel(new TrainGameUI(this, this));

        gameState.unPause();
    }

    @Override
    public void stopGame() {
        gameState.pause();
        gameState.cleanup();
        gameMap.cleanup();
        cleanMods();

        frameManager.clear();

        mainMenu = new MainMenu(this, this, renderer::initiateStop);
        frameManager.addFrameCenter(mainMenu, window);
    }

    public void saveGame(File target) {
        gameState.pause();

        try {
            Thread.sleep(1000 / settings.TARGET_TPS);
        } catch (Exception e) {
            Toolbox.display(e);
            return;
        }

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
            String saveFileVersion = in.readUTF();
            Logger.INFO.printf("Reading savegame from \"%s\" (this is %s)", saveFileVersion, GAME_VERSION);

            new Version(saveFileVersion).requireAtLeast(0, 0);

            // mods
            cleanMods();
            int nrOfActiveMods = in.readInt();
            List<Mod> modsToLoad = new ArrayList<>(nrOfActiveMods);
            for (int i = 0; i < nrOfActiveMods; i++) {
                String modName = in.readUTF();
                Mod loadedMod = getModByName(modName);

                if (loadedMod != null) {
                    modsToLoad.add(loadedMod);
                } else {
                    Logger.WARN.printf("Savegame contains unknown mod \"%s\"", modName);
                }
            }
            initMods(modsToLoad);

            // game aspects
            settings = (Settings) in.readObject();
            time = (TickTimeTimer) in.readObject();
            Camera camera = (Camera) in.readObject();
            GameMap map = (GameMap) in.readObject();
            SingleShadowMapLights lights = (SingleShadowMapLights) in.readObject();
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

            Logger.INFO.print("Game state has been loaded");
            startGame();

        } catch (Exception ex) {
            Logger.ERROR.print(ex);

        } finally {
            gameState.unPause();
        }
    }

    @Override
    public TickTimeTimer timer() {
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
    public FrameManager gui() {
        return frameManager;
    }

    @Override
    public SceneLights lights() {
        return gameLights;
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
        gameState.initiateStop();
        permanentMods.forEach(Mod::cleanup);

        gameCamera.cleanup();
        gameState.cleanup();
        gameMap.cleanup();
        gameLights.cleanup();
        gameParticles.cleanup();
        inputHandler.cleanup();
        clickShader.cleanup();
        progress.cleanup();

        frameManager.clear();

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
