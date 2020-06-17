package NG.GUIMenu.Menu;

import NG.Core.Game;
import NG.Entities.Entity;
import NG.InputHandling.MouseTools.AbstractMouseTool;
import org.joml.Vector3fc;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author Geert van Ieperen created on 27-5-2020.
 */
public class EntityActionTool extends AbstractMouseTool {
    private final Predicate<Entity> guard;
    private final Consumer<Entity> action;

    public EntityActionTool(Game game, Predicate<Entity> guard, Consumer<Entity> action) {
        super(game);
        this.guard = guard;
        this.action = action;
    }

    @Override
    public void apply(Entity entity, Vector3fc origin, Vector3fc direction) {
        if (getMouseAction() == MouseAction.PRESS_ACTIVATE) {
            if (guard.test(entity)) {
                action.accept(entity);
                game.inputHandling().setMouseTool(null);
            }

        } else if (getMouseAction() == MouseAction.PRESS_DEACTIVATE) {
            game.inputHandling().setMouseTool(null);
        }
    }

    @Override
    public void apply(Vector3fc position, Vector3fc origin, Vector3fc direction) {
        if (getMouseAction() == MouseAction.PRESS_DEACTIVATE) {
            game.inputHandling().setMouseTool(null);
        }
    }
}
