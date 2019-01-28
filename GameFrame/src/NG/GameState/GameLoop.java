package NG.GameState;

import NG.ActionHandling.ClickShader;
import NG.ActionHandling.MouseTools.MouseTool;
import NG.Engine.AbstractGameLoop;
import NG.Engine.Game;
import NG.Entities.Entity;
import NG.Rendering.Light;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shapes.Primitives.Collision;
import NG.Tools.Toolbox;
import org.joml.Vector2fc;
import org.joml.Vector3fc;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A collection of entities, which manages synchronous updating and drawing.
 * @author Geert van Ieperen. Created on 14-9-2018.
 */
public class GameLoop extends AbstractGameLoop implements GameState {
    private final List<Entity> entities;
    private final List<Light> lights;
    private final Lock entityWriteLock;
    private final Lock entityReadLock;
    private Deque<Runnable> postUpdateActionQueue;
    private Game game;

    public GameLoop(String gameName, int targetTps) {
        super("Gameloop " + gameName, targetTps);
        this.entities = new ArrayList<>();
        this.lights = new ArrayList<>();
        this.postUpdateActionQueue = new ConcurrentLinkedDeque<>();
        ReadWriteLock rwl = new ReentrantReadWriteLock(false);
        this.entityWriteLock = rwl.writeLock();
        this.entityReadLock = rwl.readLock();
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
    public void addEntity(Entity entity) { // TODO group new entities like has been done in JetFighterGame
        // Thanks to the reentrant mechanism, this may also be executed by a deferred action.
        defer(() -> {
            entityWriteLock.lock();
            try {
                entities.add(entity);
            } finally {
                entityWriteLock.unlock();
            }
        });
    }

    @Override
    public void addLight(Light light) {
        defer(() -> {
            entityWriteLock.lock();
            try {
                lights.add(light);
            } finally {
                entityWriteLock.unlock();
            }
        });
    }

    /**
     * updates the server state of all objects
     */
    public void update(float deltaTime) {
        runPostUpdateActions();
        runCleaning();

        entities.forEach(Entity::update);

        runPostUpdateActions();
    }

    @Override
    public void draw(SGL gl) {
        entityReadLock.lock();
        try {
            Toolbox.drawAxisFrame(gl);
            for (Entity entity : entities) {
                gl.ifAccepted(entity, entity::draw);
            }

        } finally {
            entityReadLock.unlock();
        }
    }

    @Override
    public Collision getEntityCollision(Vector3fc from, Vector3fc to) {
        return null;
    }

    @Override
    public List<Storage> getIndustriesByRange(Vector2fc position, int range) {
        final int rangeSq = range * range;
        List<Storage> industries = new ArrayList<>();

        //TODO efficiency (binary tree?)
        entityReadLock.lock();
        try {
            for (Entity entity : entities) {
                if (entity instanceof Storage) {
                    Storage industry = (Storage) entity;
                    if (industry.getPosition().distanceSquared(position) < rangeSq) {
                        industries.add(industry);
                    }
                }
            }
        } finally {
            entityReadLock.unlock();
        }

        return industries;
    }

    @Override
    public void drawLights(SGL gl) {
        for (Light light : lights) {
            light.draw(gl);
        }
    }

    /** executes action after a gameloop completes */
    public void defer(Runnable action) {
        // TODO make a 'modifyingDefer'
        postUpdateActionQueue.offer(action);
    }

    /** execute all actions that have been deferred */
    private void runPostUpdateActions() {
        while (!postUpdateActionQueue.isEmpty()) {
            postUpdateActionQueue.remove().run();
        }
    }

    /** remove all entities from the entity list that have their doRemove flag true */
    private void runCleaning() {
        entityWriteLock.lock();
        try {
            entities.removeIf(Entity::isDisposed);
        } finally {
            entityWriteLock.unlock();
        }
    }

    @Override
    public void cleanup() {
        entityWriteLock.lock();
        entities.clear();
        lights.clear();
        postUpdateActionQueue.clear();
        entityWriteLock.unlock();
    }

    @Override
    public void writeToFile(DataOutput out) {

    }

    @Override
    public void readFromFile(DataInput in) {

    }

    @Override
    public boolean checkMouseClick(MouseTool tool, int xSc, int ySc) {
        Entity entity = ClickShader.getEntity(game, xSc, ySc);
        if (entity == null) return false;

        tool.apply(entity, xSc, ySc);
        return true;
    }

}
