package NG.GameState;

import NG.DataStructures.MatrixStack.SGL;
import NG.DataStructures.Storable;
import NG.Engine.FreightGame;
import NG.Engine.GameAspect;
import NG.Entities.MovingEntity;

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
}
