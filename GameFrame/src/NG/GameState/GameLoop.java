package NG.GameState;

import NG.DataStructures.MatrixStack.SGL;
import NG.Engine.AbstractGameLoop;
import NG.Engine.Game;
import NG.Entities.Entity;
import NG.Entities.MovingEntity;
import NG.Mods.MapGeneratorMod;
import NG.Tools.Toolbox;
import org.joml.Vector2fc;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @author Geert van Ieperen. Created on 14-9-2018.
 */
public class GameLoop extends AbstractGameLoop implements GameState {
    public static final float WORLD_SECTION_SIZE = 16f;
    private final List<Entity> dynamicEntities;
    private List<Entity> worldObjects;
    private MapGeneratorMod mapGenerator;
    private Deque<Runnable> postUpdateActionQueue;
    private int mapGeneratorSeed;

    public GameLoop(String gameName, int targetTps) {
        super("Gameloop " + gameName, targetTps);
        this.dynamicEntities = new ArrayList<>();
        this.worldObjects = new ArrayList<>();
        postUpdateActionQueue = new ConcurrentLinkedDeque<>();
    }

    @Override
    public void init(Game game) {
    }

    /**
     * Adds an entity to the gameloop in a synchronized fashion. Thanks to the reentrant mechanism, this may also be
     * executed by a deferred action.
     * @param entity the new entity, with only its constructor called
     * @see #defer(Runnable)
     */
    @Override
    public void addEntity(MovingEntity entity) {
        // #synchronisation
        defer(() -> dynamicEntities.add(entity));
    }

    /**
     * updates the server state of all objects
     */
    public void update(float deltaTime) {
        runPostUpdateActions();

        dynamicEntities.forEach(Entity::update);

        runPostUpdateActions();
    }

    @Override
    public synchronized void draw(SGL gl) {
        Toolbox.drawAxisFrame(gl);
        worldObjects.forEach(entity -> entity.draw(gl));
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
        new Thread(() -> mapGenerator.generateNew(seed), "Map generator thread").start();
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
        worldObjects.clear();
    }

    public int getMapSeed() {
        return mapGeneratorSeed;
    }
}
