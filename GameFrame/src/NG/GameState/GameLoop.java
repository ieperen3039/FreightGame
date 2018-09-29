package NG.GameState;

import NG.DataStructures.MatrixStack.SGL;
import NG.Engine.AbstractGameLoop;
import NG.Engine.Game;
import NG.Entities.Entity;
import NG.Entities.MovingEntity;
import NG.Mods.MapGeneratorMod;
import NG.ScreenOverlay.Frames.Components.SFiller;
import NG.ScreenOverlay.Frames.Components.SFrame;
import NG.ScreenOverlay.Frames.Components.SProgressBar;
import NG.Tools.Toolbox;
import org.joml.Vector2fc;
import org.joml.Vector3f;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import static NG.ScreenOverlay.Frames.Components.SContainer.*;

/**
 * @author Geert van Ieperen. Created on 14-9-2018.
 */
public class GameLoop extends AbstractGameLoop implements GameState {
    private final List<Entity> dynamicEntities;
    private MapGeneratorMod mapGenerator;
    private Deque<Runnable> postUpdateActionQueue;
    private int mapGeneratorSeed;
    private Game game;

    public GameLoop(String gameName, int targetTps) {
        super("Gameloop " + gameName, targetTps);
        this.dynamicEntities = new ArrayList<>();
        postUpdateActionQueue = new ConcurrentLinkedDeque<>();
    }

    @Override
    public void init(Game game) {
        this.game = game;
    }

    /**
     * Adds an entity to the gameloop in a synchronized fashion.
     * @param entity the new entity, with only its constructor called
     * @see #defer(Runnable)
     */
    @Override
    public void addEntity(MovingEntity entity) {
        // Thanks to the reentrant mechanism, this may also be executed by a deferred action.
        defer(() -> dynamicEntities.add(entity));
    }

    /**
     * updates the server state of all objects
     */
    public void update(float deltaTime) {
        runPostUpdateActions();

        mapGenerator.getWorldEntities().forEach(Entity::update);
        dynamicEntities.forEach(Entity::update);

        runPostUpdateActions();
    }

    @Override
    public synchronized void draw(SGL gl) {
        Toolbox.drawAxisFrame(gl);
        mapGenerator.getWorldEntities().forEach(entity -> entity.draw(gl));
        dynamicEntities.forEach(entity -> entity.draw(gl));
    }

    @Override
    public Vector3f getPosition(Vector2fc mapCoord) {
        return new Vector3f(mapCoord, 0.0f);
    }

    public void setMapGenerator(MapGeneratorMod generator) {
        mapGenerator = generator;
    }

    public boolean hasMapGenerator() {
        return mapGenerator != null;
    }

    /**
     * generate a map with a random seed
     * @throws IllegalStateException if no map generator has been loaded; if {@link #hasMapGenerator()} returns false
     */
    public void generateMap() {
        generateMap(Math.abs(Toolbox.random.nextInt()));
    }

    /**
     * generate a map using the provided seed value. This method runs the generation algorithm in a separate thread
     * @param seed the seed to generate the map
     * @throws IllegalStateException if no map generator has been loaded; if {@link #hasMapGenerator()} returns false
     */
    public void generateMap(int seed) {
        if (!hasMapGenerator()) throw new IllegalStateException("No map generator has been loaded");

        mapGeneratorSeed = seed;

        SFrame frame = new SFrame("Generating map...", 500, 200);
        frame.add(new SFiller(), NORTHEAST);
        frame.add(new SProgressBar(400, 50, mapGenerator::getProgress), MIDDLE);
        frame.add(new SFiller(), SOUTHWEST);
        game.frameManager().addFrame(frame);

        new Thread(() -> {
            mapGenerator.generateNew(seed, 2, 2);
            frame.dispose();
        }, "Map generator thread").start();
    }

    public synchronized void defer(Runnable action) {
        postUpdateActionQueue.offer(action);
    }

    private synchronized void runPostUpdateActions() {
        // this is the only method _removing_ things
        while (!postUpdateActionQueue.isEmpty()) {
            postUpdateActionQueue.remove().run();
        }
    }

    @Override
    public void cleanup() {
        dynamicEntities.clear();
        mapGenerator.cleanup();
    }

    public int getMapSeed() {
        return mapGeneratorSeed;
    }

    @Override
    public void writeToFile(DataOutput out) {

    }

    @Override
    public void readFromFile(DataInput in) {

    }
}
