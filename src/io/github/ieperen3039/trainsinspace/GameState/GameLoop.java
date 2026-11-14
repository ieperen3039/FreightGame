package io.github.ieperen3039.trainsinspace.GameState;

import io.github.ieperen3039.ngn.Core.GenericThreadLoop;
import io.github.ieperen3039.ngn.DataStructures.Generic.Color4f;
import io.github.ieperen3039.ngn.InputHandling.MouseTools.MouseSelector;
import io.github.ieperen3039.ngn.InputHandling.MouseTools.MouseTarget;
import io.github.ieperen3039.ngn.Rendering.MatrixStack.SGL;
import io.github.ieperen3039.ngn.Rendering.Shaders.ClickShaderTarget;
import io.github.ieperen3039.ngn.Rendering.Shaders.MaterialShader;
import io.github.ieperen3039.ngn.Rendering.Shaders.ShaderProgram;
import io.github.ieperen3039.ngn.Tools.TickTime;
import io.github.ieperen3039.ngn.Tools.TickTimeTimer;
import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.DataStructures.Collision.ColliderEntity;
import io.github.ieperen3039.trainsinspace.DataStructures.Collision.GilbertJohnsonKeerthiCollision;
import io.github.ieperen3039.trainsinspace.Entities.GameEntity;
import io.github.ieperen3039.trainsinspace.InputHandling.MouseTool.MouseTargetEntity;
import org.joml.FrustumIntersection;
import org.joml.Matrix4fc;
import org.joml.Vector2ic;
import org.joml.primitives.AABBfc;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * A collection of entities, which manages synchronous updating and drawing.
 *
 * @author Geert van Ieperen. Created on 14-9-2018.
 */
public class GameLoop extends GenericThreadLoop implements GameState, MouseSelector {
    private final List<GameEntity> entities;
    private final List<GameEntity> newEntities;

    private final Map<Integer, GameEntity> entityIdMap = new HashMap<>();

    private final ClickShaderTarget clickShader;
    private TickTimeTimer timer;
    private Game game;

    public GameLoop(int targetTps, ClickShaderTarget clickShader) {
        super("Gameloop", targetTps);
        this.clickShader = clickShader;
        this.entities = new CopyOnWriteArrayList<>();
        this.newEntities = new ArrayList<>();
    }

    @Override
    public void init(Game game) throws Exception {
        this.game = game;
    }

    @Override
    public void addEntity(GameEntity entity) {
        synchronized (newEntities) {
            newEntities.add(entity);
        }
        synchronized (entityIdMap) {
            entityIdMap.put(entity.entityId, entity);
        }
    }

    /**
     * updates the server state of all objects
     */
    public void update(TickTime realTime) {
        runCleaning(realTime.getTime());

        entities.forEach(GameEntity::update);
        game.playerStatus().update();

        updateEntityList();
    }

    private void updateEntityList() {
        synchronized (newEntities) {
            if (!newEntities.isEmpty()) {
                entities.addAll(newEntities);
                newEntities.clear();
            }
        }
    }

    // note: visualizes the GameState
    @Override
    public void draw(SGL gl, TickTime time) {
        MaterialShader matShader = (d, s, r) -> {
        };

        ShaderProgram shader = gl.getShader();
        if (shader instanceof MaterialShader) {
            matShader = (MaterialShader) shader;
        }

        Matrix4fc viewProjection = gl.getViewProjectionMatrix();
        FrustumIntersection fic = new FrustumIntersection(viewProjection, false);

        for (GameEntity entity : entities) {
            matShader.setMaterial(Color4f.MAGENTA, Color4f.WHITE, 1);

            if (entity instanceof ColliderEntity collider) { // cull if possible
                AABBfc hitbox = collider.getHitbox();
                boolean isVisible = fic.testAab(hitbox.minX(), hitbox.minY(), hitbox.minZ(), hitbox.maxX(), hitbox.maxY(), hitbox.maxZ());
                if (!isVisible) continue;
            }

            entity.draw(gl, time);
        }
    }

    /**
     * remove all entities from the entity list that have their doRemove flag true
     */
    private void runCleaning(double renderTime) {
        entities.removeIf(entity -> entity.isDespawnedAt(renderTime));
    }

    @Override
    public MouseTarget pick(int xSc, int ySc) {
        Vector2ic position = clickShader.getMousePosition();
        assert position.x() == xSc && position.y() == ySc;

        int entityId = clickShader.getValue();
        GameEntity entity = getEntityById(entityId);
        if (entity == null) return null;

        return new MouseTargetEntity(entity);
    }

    private GameEntity getEntityById(int entityId) {
        synchronized (entityIdMap) {
            return entityIdMap.get(entityId);
        }
    }

    @Override
    public synchronized Collection<GameEntity> entities() {
        ArrayList<GameEntity> entities = new ArrayList<>(this.entities);

        synchronized (newEntities) {
            entities.addAll(newEntities);
        }

        return entities;
    }

    @Override
    public Collection<GameEntity> getCollisions(ColliderEntity entity) {
        AABBfc hitbox = entity.getHitbox();
        List<GameEntity> result = new ArrayList<>();

        for (GameEntity ety : entities) {
            if (ety instanceof ColliderEntity colliderEntity) {
                boolean mayCollide = hitbox.containsAABB(colliderEntity.getHitbox());
                if (!mayCollide) continue;

                boolean doesCollide = GilbertJohnsonKeerthiCollision.checkCollision(entity, colliderEntity);
                if (!doesCollide) continue;

                result.add(ety);
            }
        }

        return result;
    }

    @Override
    public Stream<GameEntity> stream() {
        return entities.stream();
    }

    @Override
    public void cleanup() {
        synchronized (newEntities) {
            newEntities.clear();
        }

        entities.clear();
    }

    @Override
    public Iterator<GameEntity> iterator() {
        return entities.iterator();
    }

    /**
     * writes the list of entities to the given output. This method is thread-safe.
     *
     * @param out the output where all entities are written to
     * @throws IOException if any entity causes an IOException. The stream is left in an undetermined state
     */
    public void writeTo(ObjectOutput out) throws IOException {
        Collection<GameEntity> entitiesList = entities();
        int nrEntities = entitiesList.size();
        out.writeInt(nrEntities);

        for (GameEntity entity : entitiesList) {
            out.writeObject(entity);
        }
    }

    /**
     * reads a list of entities from the given input, assuming it was written using {@link #writeTo(ObjectOutput)}. This
     * method should be executed while this loop is paused, or using {@link #defer(Runnable)}.
     *
     * @param in the input to read from
     * @throws IOException            if any entity causes an IOException. The stream is left in an undetermined state.
     * @throws ClassNotFoundException if the class of any entity is not loaded.
     */
    public void readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
        synchronized (newEntities) {
            newEntities.clear();
            entities.clear();

            int nrEntities = in.readInt();
            ArrayList<GameEntity> list = new ArrayList<>(nrEntities);
            for (int i = 0; i < nrEntities; i++) {
                list.add((GameEntity) in.readObject());
            }

            // we do it like this, as `entities` is a `CopyOnWriteArray`
            entities.addAll(list);
        }
    }
}
