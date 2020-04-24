package NG.InputHandling.MouseTools;

import NG.Core.Game;
import NG.Entities.Entity;
import NG.GUIMenu.Components.SComponent;
import NG.InputHandling.MouseMoveListener;
import NG.InputHandling.MouseRelativeClickListener;
import NG.InputHandling.MouseReleaseListener;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import org.joml.Vector2i;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFW;

/**
 * A mouse tool that implements the standard behaviour of the pointer user input.
 *
 * <dl>
 * <dt>UI elements:</dt>
 * <dd>components are activated when clicked on, drag and release listeners are respected</dd>
 * <dt>Entities:</dt>
 * <dd>The entity gets selected</dd>
 * <dt>Map:</dt>
 * <dd>If an entity is selected, open an action menu</dd>
 * </dl>
 * @author Geert van Ieperen. Created on 26-11-2018.
 */
public class DefaultMouseTool extends AbstractMouseTool {

    public DefaultMouseTool(Game game) {
        super(game);
    }

    @Override
    public void apply(SComponent component, int xSc, int ySc) {
        Logger.DEBUG.print("Clicked on " + component);

        if (component instanceof MouseRelativeClickListener) {
            MouseRelativeClickListener cl = (MouseRelativeClickListener) component;
            // by def. of MouseRelativeClickListener, give relative coordinates
            Vector2i pos = component.getScreenPosition();
            cl.onClick(getButton(), xSc - pos.x, ySc - pos.y);
        }

        if (component instanceof MouseMoveListener) {
            dragListener = (MouseMoveListener) component;
            dragButton = getButton();

        } else {
            dragListener = null;
        }

        if (component instanceof MouseReleaseListener) {
            releaseListener = (MouseReleaseListener) component;

        } else {
            releaseListener = null;
        }
    }

    @Override
    public void apply(Entity entity, int xSc, int ySc) {
        Logger.DEBUG.print("Clicked on " + entity);

        if (getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            // TODO action when clicking on an entity
        }
    }

    @Override
    public void apply(Vector3fc position, int xSc, int ySc) {
        Logger.DEBUG.print("Clicked at " + Vectors.toString(position));
    }

    @Override
    public void dispose() {
        // no
    }

}
