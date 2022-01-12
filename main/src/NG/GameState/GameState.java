package NG.GameState;

import NG.Core.FreightGame;
import NG.Core.GameAspect;
import NG.DataStructures.Collision.ColliderEntity;
import NG.Entities.Entity;
import NG.InputHandling.MouseTool.MouseTool;
import NG.Rendering.MatrixStack.SGL;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A collection of all entities in the world, all lights present in the world. Allows querying for specific objects and
 * collisions.
 * @author Geert van Ieperen. Created on 21-9-2018.
 */
public interface GameState extends GameAspect, Iterable<Entity> {
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
     * checks whether an input click can be handled by this object
     * @param tool the current mouse tool
     * @param xSc  the screen x position of the mouse
     * @param ySc  the screen y position of the mouse
     * @return true iff the click has been handled by this object
     */
    boolean checkMouseClick(MouseTool tool, int xSc, int ySc, Vector3f origin, Vector3f direction);

    /**
     * In contrary to {@link #iterator()} and {@link #stream()}, this collection must contain all entities ever added
     * using {@link #addEntity(Entity)}
     * @return a copy of all entities ever added to this game state
     */
    Collection<Entity> entities();

    Collection<Entity> getCollisions(ColliderEntity entity);

    default Stream<Entity> stream() {
        return StreamSupport.stream(spliterator(), false);
    }
}
