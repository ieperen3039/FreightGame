package io.github.ieperen3039.trainsinspace.InputHandling.MouseTool;

import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.Entities.Entity;
import io.github.ieperen3039.trainsinspace.Tools.Logger;
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
    public void apply(Entity entity, Vector3fc origin, Vector3fc direction) {
        MouseAction mouseAction = getMouseAction();
        if (mouseAction == MouseAction.PRESS_ACTIVATE) {
            Logger.DEBUG.print("Clicked on " + entity);
        }

        entity.reactMouse(mouseAction, game.keyControl());
    }

    @Override
    public void apply(Vector3fc position, Vector3fc origin, Vector3fc direction) {
    }
}
