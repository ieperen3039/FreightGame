package NG.InputHandling.MouseTools;

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
     * @param entity an entity that is clicked on using this tool, always not null
     * @param xSc    screen x position of the mouse in pixels from left
     * @param ySc    screen y position of the mouse in pixels from top
     */
    void apply(Entity entity, int xSc, int ySc);

    /**
     * applies the functionality of this tool to the given position in the world
     * @param position a position in the world where is clicked.
     * @param xSc      screen x position of the mouse in pixels from left
     * @param ySc      screen y position of the mouse in pixels from top
     */
    void apply(Vector3fc position, int xSc, int ySc);

    /**
     * sets this mousetool to be no longer used.
     */
    void dispose();

}
