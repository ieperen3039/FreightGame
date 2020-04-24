package NG.GameState;

import NG.Core.FreightGame;
import NG.Core.GameAspect;
import NG.Entities.Entity;
import NG.InputHandling.MouseTools.MouseToolListener;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shapes.Primitives.Collision;
import org.joml.Vector3fc;

/**
 * A collection of all entities in the world, all lights present in the world. Allows querying for specific objects and
 * collisions.
 * @author Geert van Ieperen. Created on 21-9-2018.
 */
public interface GameState extends GameAspect, MouseToolListener {
    /**
     * adds an entity to the game in a thread-safe way.
     * @param entity the new entity, with only its constructor called
     */
    void addEntity(Entity entity);

    /**
     * draws the objects on the screen, according to the state of the {@link FreightGame#timer()} object.
     * @param gl the gl object to draw with
     */
    void draw(SGL gl);

    /**
     * cast a ray into the world, and returns the first entity hit by this ray
     * @param from starting position
     * @param to   end position, maximum how far the ray goes
     * @return the entity that is hit, or null if no such entity exists.
     */
    Collision getEntityCollision(Vector3fc from, Vector3fc to);

    /**
     * removes the given entity from the gameState. This action does not have to be executed
     * immediately.
     * @param entity an entity to be removed
     * @deprecated instead, put an entities {@link Entity#isDisposed()} to true
     */
    @Deprecated
    default void removeEntity(Entity entity) {
        entity.dispose();
    }
}
