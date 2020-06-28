package NG.GameState;

import NG.Core.AbstractGameLoop;
import NG.Core.Game;
import NG.DataStructures.Collision.ColliderEntity;
import NG.DataStructures.Collision.GilbertJohnsonKeerthiCollision;
import NG.DataStructures.Generic.Color4f;
import NG.Entities.Entity;
import NG.InputHandling.ClickShader;
import NG.InputHandling.MouseTools.MouseTool;
import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shaders.ShaderProgram;
import org.joml.AABBf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A collection of entities, which manages synchronous updating and drawing.
 * @author Geert van Ieperen. Created on 14-9-2018.
 */
public class GameLoop extends AbstractGameLoop implements GameState {
    private final List<Entity> entities;
    private final List<Entity> newEntities;

    private final Deque<Runnable> postUpdateActionQueue;
    private final ClickShader clickShader;
    private Game game;
    private Entity.Marking markBlue = new Entity.Marking();

    public GameLoop(int targetTps, ClickShader clickShader) {
        super("Gameloop", targetTps);
        this.clickShader = clickShader;
        this.entities = new CopyOnWriteArrayList<>();
        this.newEntities = new ArrayList<>();
        this.postUpdateActionQueue = new ConcurrentLinkedDeque<>();
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
    public void addEntity(Entity entity) {
        synchronized (newEntities) {
            newEntities.add(entity);
        }
    }

    /**
     * updates the server state of all objects
     */
    public void update(float deltaTime) {
        runPostUpdateActions();
        runCleaning();

        game.timer().updateGameTime();
        entities.forEach(Entity::update);

        updateEntityList();

        runPostUpdateActions();
    }

    private synchronized void updateEntityList() {
        synchronized (newEntities) {
            if (!newEntities.isEmpty()) {
                entities.addAll(newEntities);
                newEntities.clear();
            }
        }
    }

    @Override
    public void draw(SGL gl) {
        MaterialShader matShader = (d, s, r) -> {};

        ShaderProgram shader = gl.getShader();
        if (shader instanceof MaterialShader) {
            matShader = (MaterialShader) shader;
        }

        for (Entity entity : entities) {
            matShader.setMaterial(Material.ROUGH, Color4f.MAGENTA);
            entity.draw(gl);
        }
    }

    /** executes action after a gameloop completes */
    public void defer(Runnable action) {
        postUpdateActionQueue.offer(action);
    }

    /** execute all actions that have been deferred */
    private void runPostUpdateActions() {
        Runnable runnable = postUpdateActionQueue.poll();
        while (runnable != null) {
            runnable.run();
            runnable = postUpdateActionQueue.poll();
        }
    }

    /** remove all entities from the entity list that have their doRemove flag true */
    private void runCleaning() {
        entities.removeIf(entity -> entity.isDespawnedAt(game.timer().getRenderTime()));
    }

    @Override
    public boolean checkMouseClick(MouseTool tool, int xSc, int ySc, Vector3f origin, Vector3f direction) {
        Entity entity = clickShader.getEntity(game, xSc, ySc);
        if (entity == null) return false;

        tool.apply(entity, origin, direction);
        return true;
    }

    @Override
    public synchronized Collection<Entity> entities() {
        ArrayList<Entity> entities = new ArrayList<>(this.entities);

        synchronized (newEntities) {
            entities.addAll(newEntities);
        }

        return entities;
    }

    @Override
    public Collection<Entity> getCollisions(ColliderEntity entity) {
        AABBf hitbox = entity.getHitbox();
        List<Entity> result = new ArrayList<>();

        Entity.Marking oldMark = markBlue;
        markBlue = new Entity.Marking(Color4f.BLUE);

        for (Entity ety : entities) {
            if (ety instanceof ColliderEntity) {
                ColliderEntity colliderEntity = (ColliderEntity) ety;

                boolean mayCollide = hitbox.testAABB(colliderEntity.getHitbox());
                if (mayCollide) {
                    entity.setMarking(markBlue);

                    boolean doesCollide = GilbertJohnsonKeerthiCollision.checkCollision(entity, colliderEntity);

                    if (doesCollide) {
                        result.add(colliderEntity);
                    }
                }
            }
        }
        oldMark.invalidate();

        return result;
    }

    @Override
    public void cleanup() {
        synchronized (newEntities) {
            newEntities.clear();
        }

        entities.clear();
        postUpdateActionQueue.clear();
    }
}
