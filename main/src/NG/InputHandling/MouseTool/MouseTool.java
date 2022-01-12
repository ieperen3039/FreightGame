package NG.InputHandling.MouseTool;

import NG.Entities.Entity;
import NG.InputHandling.MouseListener;
import NG.Rendering.MatrixStack.SGL;
import org.joml.Vector3fc;

/**
 * Determines the behaviour of clicking
 * @author Geert van Ieperen. Created on 22-11-2018.
 */
public interface MouseTool extends MouseListener {

    /**
     * draws any visual indications used by this tool
     * @param gl the rendering context
     */
    void draw(SGL gl);

    /**
     * applies the functionality of this tool to the given entity
     * @param entity    an entity that is clicked on using this tool, always not null
     * @param origin    the start point of the ray that hits this entity
     * @param direction the direction of the ray that hits this entity
     */
    void apply(Entity entity, Vector3fc origin, Vector3fc direction);

    /**
     * applies the functionality of this tool to the given position in the world
     * @param position  a position in the world where is clicked.
     * @param origin    the start point of the ray that hits this entity
     * @param direction the direction of the ray that hits this entity
     */
    void apply(Vector3fc position, Vector3fc origin, Vector3fc direction);

    /**
     * activates when this mousetool is deactivated
     */
    default void dispose() {
    }
}
