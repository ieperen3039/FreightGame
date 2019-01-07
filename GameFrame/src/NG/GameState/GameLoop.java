package NG.GameState;

import NG.ActionHandling.MouseTools.MouseTool;
import NG.Camera.Camera;
import NG.DataStructures.Color4f;
import NG.Engine.AbstractGameLoop;
import NG.Engine.Game;
import NG.Entities.Entity;
import NG.Rendering.GLFWWindow;
import NG.Rendering.Light;
import NG.Rendering.MatrixStack.SGL;
import NG.Shapes.Primitives.Collision;
import NG.Tools.Toolbox;
import NG.Tools.Vectors;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Geert van Ieperen. Created on 14-9-2018.
 */
public class GameLoop extends AbstractGameLoop implements GameState {
    private final List<Entity> entities;
    private final List<Light> lights;
    private final Lock drawLock;
    private Deque<Runnable> postUpdateActionQueue;
    private Game game;

    public GameLoop(String gameName, int targetTps) {
        super("Gameloop " + gameName, targetTps);
        this.entities = new ArrayList<>();
        this.lights = new ArrayList<>();
        this.postUpdateActionQueue = new ConcurrentLinkedDeque<>();
        this.drawLock = new ReentrantLock();

        lights.add(new Light(new Vector3f(1, 1, 2), new Color4f(1, 1, 0.8f), 0.2f, true));
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
    public void addEntity(Entity entity) {
        // Thanks to the reentrant mechanism, this may also be executed by a deferred action.
        defer(() -> {
            drawLock.lock();
            try {
                entities.add(entity);
            } finally {
                drawLock.unlock();
            }
        });
    }

    /**
     * updates the server state of all objects
     */
    public void update(float deltaTime) {
        runPostUpdateActions();
        entities.forEach(Entity::update);
        runPostUpdateActions();
    }

    @Override
    public void draw(SGL gl) {
        drawLock.lock();
        try {
            Toolbox.drawAxisFrame(gl);
            for (Entity entity : entities) {
                gl.ifAccepted(entity, entity::draw);
            }

        } finally {
            drawLock.unlock();
        }
    }

    @Override
    public Collision getEntityCollision(Vector3fc from, Vector3fc to) {
        return null;
    }

    @Override
    public List<Storage> getIndustriesByRange(Vector3fc position, int range) {
        final int rangeSq = range * range;
        List<Storage> industries = new ArrayList<>();

        //TODO efficiency
        for (Entity entity : entities) {
            if (entity instanceof Storage) {
                Storage industry = (Storage) entity;
                if (industry.getPosition().distanceSquared(position) < rangeSq) {
                    industries.add(industry);
                }
            }
        }

        return industries;
    }

    @Override
    public void removeEntity(Entity entity) {
        defer(() -> entities.remove(entity));
    }

    @Override
    public void drawLights(SGL gl) {
        for (Light light : lights) {
            light.draw(gl);
        }
    }

    /** executes action after a gameloop completes */
    public void defer(Runnable action) {
        postUpdateActionQueue.offer(action);
    }

    private void runPostUpdateActions() {
        while (!postUpdateActionQueue.isEmpty()) {
            postUpdateActionQueue.remove().run();
        }
    }

    @Override
    public void cleanup() {
        drawLock.lock();
        stopLoop(); // possibly this did not happen
        entities.clear();
        drawLock.unlock();
    }

    @Override
    public void writeToFile(DataOutput out) {

    }

    @Override
    public void readFromFile(DataInput in) {

    }

    @Override
    public boolean checkMouseClick(MouseTool tool, int xSc, int ySc) {
        GLFWWindow window = game.window();
        Camera camera = game.camera();
        Vector3f origin = new Vector3f();
        Vector3f direction = new Vector3f();

        Vectors.windowCoordToRay(camera, origin, direction, window.getWidth(), window.getHeight(), new Vector2f(xSc, ySc));

        Collision collision = getEntityCollision(origin, direction);
        if (collision == null) return false;

        tool.apply(collision.getEntity(), collision.hitPosition());
        return true;
    }
}
