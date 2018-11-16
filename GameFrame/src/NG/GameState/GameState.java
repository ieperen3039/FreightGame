package NG.GameState;

import NG.DataStructures.MatrixStack.SGL;
import NG.DataStructures.Storable;
import NG.Engine.FreightGame;
import NG.Engine.GameAspect;
import NG.Entities.Entity;
import NG.Entities.MovingEntity;
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
    void addEntity(MovingEntity entity);

    /**
     * draws the objects on the screen, according to the state of the {@link FreightGame#time} object
     * @param gl the gl object to draw with
     */
    void draw(SGL gl);

    /**
     * cast a ray into the world, and returns the first entity hit by this ray
     * @param from
     * @param to
     */
    Entity getEntityByRay(Vector4f from, Vector4f to);

    List<Storage> getIndustriesByRange(Vector3fc position, int range);
}
