package NG.GameState;

import NG.DataStructures.MatrixStack.SGL;
import NG.Engine.FreightGame;
import NG.Engine.Game;
import NG.Engine.GameAspect;
import NG.Entities.Entity;
import NG.Entities.MovingEntity;
import NG.Tools.Toolbox;
import org.joml.Vector2fc;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Geert van Ieperen. Created on 14-9-2018.
 */
public class GameState implements GameAspect {
    private final List<Entity> dynamicEntities;
    private final List<Entity> worldObjects;

    public GameState() {
        this.dynamicEntities = new ArrayList<>();
        this.worldObjects = new ArrayList<>();
    }

    @Override
    public void init(Game game) {

    }

    /**
     * adds an entity to the game
     * @param entity the new entity, with only its constructor called
     */
    public void addEntity(MovingEntity entity) {
        dynamicEntities.add(entity);
    }

    /**
     * updates the server state of all objects
     */
    public void update() {
        dynamicEntities.forEach(Entity::update);
    }

    /**
     * draws the objects on the screen, according to the state of the {@link FreightGame#time} object
     * @param gl the gl object to draw with
     */
    public void draw(SGL gl) {
        Toolbox.drawAxisFrame(gl);
        worldObjects.forEach(entity -> entity.draw(gl));
        dynamicEntities.forEach(entity -> entity.draw(gl));
    }

    @Override
    public void cleanup() {
        dynamicEntities.clear();
        worldObjects.clear();
    }

    public Vector3f getPosition(Vector2fc mapCoord) {
        return new Vector3f(mapCoord, 0.0f);
    }
}
