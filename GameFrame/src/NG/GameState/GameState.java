package NG.GameState;

import NG.DataStructures.MatrixStack.SGL;
import NG.DataStructures.Storable;
import NG.Engine.FreightGame;
import NG.Engine.GameAspect;
import NG.Entities.MovingEntity;
import org.joml.Vector2fc;
import org.joml.Vector3f;

/**
 * @author Geert van Ieperen. Created on 21-9-2018.
 */
public interface GameState extends GameAspect, Storable {
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
     * maps a 2D map coordinate to a 3D position. \result.x == mapCoord.x && result.y == mapCoord.y
     * @param mapCoord a 2D map coordinate
     * @return the 2D coordinate mapped to the surface of the inital map (or with z == 0 if no map is loaded)
     */
    Vector3f getPosition(Vector2fc mapCoord);
}
