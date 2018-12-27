package NG.ActionHandling.MouseTools;

import NG.ActionHandling.MouseMoveListener;
import NG.ActionHandling.MouseReleaseListener;
import NG.Entities.Entity;
import NG.ScreenOverlay.Frames.Components.SComponent;
import org.joml.Vector2fc;
import org.joml.Vector3fc;

/**
 * Determines the behaviour of clicking
 * @author Geert van Ieperen. Created on 22-11-2018.
 */
public interface MouseTool extends MouseMoveListener, MouseReleaseListener {

    /**
     * applies the functionality of this tool to the given component
     * @param component a component where has been clicked on
     * @param xSc       screen x position of the mouse in pixels from left
     * @param ySc       screen y position of the mouse in pixels from top
     */
    void apply(SComponent component, int xSc, int ySc);

    /**
     * applies the functionality of this tool to the given entity
     * @param entity an entity that is clicked on using this tool, always not null
     * @param rayCollision the position where the click intersected with this object's hitbox
     */
    void apply(Entity entity, Vector3fc rayCollision);

    /**
     * applies the functionality of this tool to the given position in the world
     * @param position a position in the world where is clicked.
     */
    void apply(Vector2fc position);

    void setButton(int button);
}
