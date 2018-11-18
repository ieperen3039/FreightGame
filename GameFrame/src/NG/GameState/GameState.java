package NG.GameState;

import NG.DataStructures.MatrixStack.SGL;
import NG.DataStructures.Storable;
import NG.Engine.FreightGame;
import NG.Engine.GameAspect;
import NG.Entities.Entity;
import NG.ScreenOverlay.Frames.ClickHandler;
import org.joml.Vector3fc;
import org.joml.Vector4f;

import java.util.List;

/**
 * @author Geert van Ieperen. Created on 21-9-2018.
 */
public interface GameState extends GameAspect, Storable, ClickHandler {
    /**
     * adds an entity to the game in a thread-safe way.
     * @param entity the new entity, with only its constructor called
     */
    void addEntity(Entity entity);

    /**
     * draws the objects on the screen, according to the state of the {@link FreightGame#time} object. Must be called
     * after {@link #drawLights(SGL)}
     * @param gl the gl object to draw with
     */
    void draw(SGL gl);

    /**
     * initializes the lights on the scene. Should be called before {@link #draw(SGL)}
     * @param gl the current gl object
     */
    void drawLights(SGL gl);

    /**
     * cast a ray into the world, and returns the first entity hit by this ray
     * @param from starting position
     * @param to end position, maximum how far the ray goes
     * @return the entity that is hit, or null if no such entity exists.
     */
    Entity getEntityByRay(Vector4f from, Vector4f to);

    /**
     * queries all industries in range of the given position.
     * @param position a position in the world
     * @param range    a maximum distance
     * @return all storage objects with a distance to {@code position} less or equal than distance.
     */
    List<Storage> getIndustriesByRange(Vector3fc position, int range);
}
