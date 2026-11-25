package io.github.ieperen3039.trainsinspace.InputHandling.MouseTool;

import io.github.ieperen3039.ngn.InputHandling.MouseTools.MouseTarget;
import io.github.ieperen3039.ngn.InputHandling.MouseTools.MouseTool;
import io.github.ieperen3039.trainsinspace.Entities.GameEntity;
import io.github.ieperen3039.trainsinspace.InputHandling.InputMapping;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * allows clicking an entity, and if it passes a given guard, runs a given action, then deactivates.
 * @author Geert van Ieperen created on 27-5-2020.
 */
public class EntityActionTool implements MouseTool {
    private final Predicate<GameEntity> guard;
    private final Consumer<GameEntity> action;

    public EntityActionTool(Predicate<GameEntity> guard, Consumer<GameEntity> action) {
        this.guard = guard;
        this.action = action;
    }

    @Override
    public HandleResult onPress(int button, MouseTarget target, int xSc, int ySc) {
        if (button == InputMapping.DeactivateMouseTool.getButton()) {
            return HandleResult.DEACTIVATE;
        }

        if (button == InputMapping.UseMouseTool.getButton()) {
            if (target instanceof MouseTargetEntity entity) {
                if (guard.test(entity.target())) {
                    action.accept(entity.target());
                    return HandleResult.DEACTIVATE;
                }
            }
        }

        return HandleResult.CONTINUE;
    }
}
