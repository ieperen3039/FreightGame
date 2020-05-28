package NG.GameState;

import NG.Core.AbstractGameLoop;
import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.Entities.Entity;
import NG.InputHandling.ClickShader;
import NG.InputHandling.MouseTools.MouseTool;
import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Rendering.Shapes.Primitives.Collision;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.*;
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
    private final Lock entityWriteLock;
    private final Lock entityReadLock;
    private final Deque<Runnable> postUpdateActionQueue;
    private final ClickShader clickShader;
    private Game game;

    public GameLoop(int targetTps, ClickShader clickShader) {
        super("Gameloop", targetTps);
        this.clickShader = clickShader;
        this.entities = new ArrayList<>();
        this.postUpdateActionQueue = new ConcurrentLinkedDeque<>();

        ReadWriteLock rwl = new ReentrantReadWriteLock(false);
        this.entityWriteLock = rwl.writeLock();
        this.entityReadLock = rwl.readLock();
    }

    @Override
    public void init(Game game) throws Exception {
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

    /**
     * updates the server state of all objects
     */
    public void update(float deltaTime) {
        runPostUpdateActions();
        runCleaning();

        game.timer().updateGameTime();
        entities.forEach(Entity::update);

        runPostUpdateActions();
    }

    @Override
    public void draw(SGL gl) {
        entityReadLock.lock();
        try {
            MaterialShader matShader = (d, s, r) -> {};

            ShaderProgram shader = gl.getShader();
            if (shader instanceof MaterialShader) {
                matShader = (MaterialShader) shader;
            }

            for (Entity entity : entities) {
                matShader.setMaterial(Material.ROUGH, Color4f.MAGENTA);
                entity.draw(gl);
            }

        } finally {
            entityReadLock.unlock();
        }
    }

    @Override
    public Collision getEntityCollision(Vector3fc from, Vector3fc to) {
        return null;
    }

    /** executes action after a gameloop completes */
    public void defer(Runnable action) {
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
            entities.removeIf(entity -> entity.isDespawnedAt(game.timer().getRenderTime()));
        } finally {
            entityWriteLock.unlock();
        }
    }

    @Override
    public boolean checkMouseClick(MouseTool tool, int xSc, int ySc, Vector3f origin, Vector3f direction) {
        Entity entity = clickShader.getEntity(game, xSc, ySc);
        if (entity == null) return false;

        tool.apply(entity, origin, direction);
        return true;
    }

    @Override
    public Collection<Entity> entities() {
        return Collections.unmodifiableList(entities);
    }

    @Override
    public void cleanup() {
        entityWriteLock.lock();
        entities.clear();
        postUpdateActionQueue.clear();
        entityWriteLock.unlock();
    }
}
