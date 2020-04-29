package NG.InputHandling.MouseTools;

import NG.Core.Game;
import NG.Entities.Entity;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import org.joml.Vector3fc;

/**
 * A mouse tool that implements the standard behaviour of the pointer user input.
 *
 * <dl>
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
    public void apply(Entity entity, int xSc, int ySc) {
        if (getMouseAction() == MouseAction.PRESS_ACTIVATE) {
            Logger.DEBUG.print("Clicked on " + entity);
        }
    }

    @Override
    public void apply(Vector3fc position, int xSc, int ySc) {
        if (getMouseAction() == MouseAction.PRESS_ACTIVATE) {
            Logger.DEBUG.print("Clicked at " + Vectors.toString(position));
        }
    }

    @Override
    public void dispose() {
        // no
    }

}
