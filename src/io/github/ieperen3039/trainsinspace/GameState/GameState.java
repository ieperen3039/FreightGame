package io.github.ieperen3039.trainsinspace.GameState;

import io.github.ieperen3039.ngn.Rendering.MatrixStack.SGL;
import io.github.ieperen3039.trainsinspace.Core.GameAspect;
import io.github.ieperen3039.trainsinspace.Core.TrainGame;
import io.github.ieperen3039.trainsinspace.DataStructures.Collision.ColliderEntity;
import io.github.ieperen3039.trainsinspace.Entities.GameEntity;

import java.util.Collection;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A collection of all entities in the world, all lights present in the world. Allows querying for specific objects and
 * collisions.
 * @author Geert van Ieperen. Created on 21-9-2018.
 */
public interface GameState extends GameAspect, Iterable<GameEntity> {
    /**
     * adds an entity to the game in a thread-safe way.
     *
     * @param entity the new entity, with only its constructor called
     */
    void addEntity(GameEntity entity);

    /**
     * draws the objects on the screen, according to the state of the {@link TrainGame#timer()} object.
     * @param gl the gl object to draw with
     */
    void draw(SGL gl);

    /**
     * In contrary to {@link #iterator()} and {@link #stream()}, this collection shall contain all entities ever added
     * using {@link #addEntity(GameEntity)}
     * @return a copy of all entities ever added to this game state
     */
    Collection<GameEntity> entities();

    Collection<GameEntity> getCollisions(ColliderEntity entity);

    default Stream<GameEntity> stream() {
        return StreamSupport.stream(spliterator(), false);
    }
}
