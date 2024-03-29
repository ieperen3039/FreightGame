package NG.GameState;

import NG.Core.AbstractGameLoop;
import NG.Core.Game;
import NG.DataStructures.Collision.ColliderEntity;
import NG.DataStructures.Collision.GilbertJohnsonKeerthiCollision;
import NG.DataStructures.Generic.Color4f;
import NG.Entities.Entity;
import NG.InputHandling.ClickShader;
import NG.InputHandling.MouseTool.MouseTool;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shaders.ShaderProgram;
import org.joml.AABBf;
import org.joml.FrustumIntersection;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * A collection of entities, which manages synchronous updating and drawing.
 * @author Geert van Ieperen. Created on 14-9-2018.
 */
public class GameLoop extends AbstractGameLoop implements GameState {
    private final List<Entity> entities;
    private final List<Entity> newEntities;

    private final ClickShader clickShader;
    private Game game;

    public GameLoop(int targetTps, ClickShader clickShader) {
        super("Gameloop", targetTps);
        this.clickShader = clickShader;
        this.entities = new CopyOnWriteArrayList<>();
        this.newEntities = new ArrayList<>();
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
        runCleaning();

        game.timer().updateGameTime();
        entities.forEach(Entity::update);
        game.playerStatus().update();

        updateEntityList();
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

        Matrix4fc viewProjection = gl.getViewProjectionMatrix();
        FrustumIntersection fic = new FrustumIntersection(viewProjection, false);

        for (Entity entity : entities) {
            matShader.setMaterial(Color4f.MAGENTA, Color4f.WHITE, 1);

//            if (entity instanceof ColliderEntity) { // cull if possible
//                AABBf hitbox = ((ColliderEntity) entity).getHitbox();
//                boolean isVisible = fic.testAab(hitbox.minX, hitbox.minY, hitbox.minZ, hitbox.maxX, hitbox.maxY, hitbox.maxZ);
//                if (!isVisible) continue;
//            }

            entity.draw(gl);
        }
    }

    /** remove all entities from the entity list that have their doRemove flag true */
    private void runCleaning() {
        double now = game.timer().getRenderTime();
        entities.removeIf(entity -> entity.isDespawnedAt(now));
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

        for (Entity ety : entities) {
            if (ety instanceof ColliderEntity) {
                ColliderEntity colliderEntity = (ColliderEntity) ety;

                boolean mayCollide = hitbox.testAABB(colliderEntity.getHitbox());
                if (mayCollide) {
                    boolean doesCollide = GilbertJohnsonKeerthiCollision.checkCollision(entity, colliderEntity);

                    if (doesCollide) {
                        result.add(colliderEntity);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public Stream<Entity> stream() {
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
    public Iterator<Entity> iterator() {
        return entities.iterator();
    }

    /**
     * writes the list of entities to the given output. This method is thread-safe.
     * @param out the output where all entities are written to
     * @throws IOException if any entity causes an IOException. The stream is left in an undetermined state
     */
    public void writeTo(ObjectOutput out) throws IOException {
        Collection<Entity> entitiesList = entities();
        int nrEntities = entitiesList.size();
        out.writeInt(nrEntities);

        for (Entity entity : entitiesList) {
            out.writeObject(entity);
        }
    }

    /**
     * reads a list of entities from the given input, assuming it was written using {@link #writeTo(ObjectOutput)}. This
     * method should be executed while this loop is paused, or using {@link #defer(Runnable)}.
     * @param in the input to read from
     * @throws IOException            if any entity causes an IOException. The stream is left in an undetermined state.
     * @throws ClassNotFoundException if the class of any entity is not loaded.
     */
    public void readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
        synchronized (newEntities) {
            newEntities.clear();
            entities.clear();

            int nrEntities = in.readInt();
            ArrayList<Entity> list = new ArrayList<>(nrEntities);
            for (int i = 0; i < nrEntities; i++) {
                list.add((Entity) in.readObject());
            }

            // we do it like this, as `entities` is a `CopyOnWriteArray`
            entities.addAll(list);
        }
    }
}
