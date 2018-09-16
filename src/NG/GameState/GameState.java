package NG.GameState;

import NG.DataStructures.MatrixStack.SGL;
import NG.Engine.FreightGame;
import NG.Entities.Entity;
import NG.Tools.Toolbox;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Geert van Ieperen. Created on 14-9-2018.
 */
public class GameState {
    private final List<Entity> dynamicEntities;
    private final List<Entity> worldObjects;

    public GameState(FreightGame freightGame) {
        this.dynamicEntities = new ArrayList<>();
        this.worldObjects = new ArrayList<>();
    }

    public void init() {

    }

    /**
     * updates the server state of all objects
     */
    public void update() {
        dynamicEntities.forEach(Entity::update);
    }

    /**
     * draws the objects on the screen, accroding to the state of the {@link FreightGame#time} object
     * @param gl the gl object to draw with
     */
    public void draw(SGL gl) {
        Toolbox.drawAxisFrame(gl);
        dynamicEntities.forEach(entity -> entity.draw(gl));
        worldObjects.forEach(entity -> entity.draw(gl));
    }
}
