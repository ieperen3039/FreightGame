package io.github.ieperen3039.trainsinspace.Menu.InGame.Build;

import io.github.ieperen3039.ngn.InputHandling.MouseTools.MouseTarget;
import io.github.ieperen3039.ngn.InputHandling.MouseTools.MouseTool;
import io.github.ieperen3039.ngn.UserInterface.Components.SToggleButton;
import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.Entities.GameEntity;
import io.github.ieperen3039.trainsinspace.Entities.Industry;
import io.github.ieperen3039.trainsinspace.Entities.StationImpl;
import io.github.ieperen3039.trainsinspace.InputHandling.InputMapping;
import io.github.ieperen3039.trainsinspace.InputHandling.MouseTool.MouseTargetMap;

/**
 * @author Geert van Ieperen created on 21-6-2020.
 */
public class IndustryBuilder implements MouseTool {
    private final Game game;
    private final Runnable deactivation;
    private Industry.Properties properties;

    public IndustryBuilder(Game game, SToggleButton button, Industry.Properties properties) {
        this.game = game;
        this.deactivation = () -> button.setActive(false);
        this.properties = properties;
    }

    @Override
    public HandleResult onPress(int button, MouseTarget target, int xSc, int ySc) {
        if (button == InputMapping.DeactivateMouseTool.getButton()) {
            return HandleResult.DEACTIVATE;
        }

        if (button != InputMapping.UseMouseTool.getButton()) return HandleResult.CONTINUE;

        if (target instanceof MouseTargetMap map) {
            Industry industry = new Industry(game, map.position(), game.timer().getTime(), properties);
            game.state().addEntity(industry);

            // reset the industries of all entities
            // TODO make this less than quadratic runtime obnoxity
            for (GameEntity entity : game.state()) {
                if (entity instanceof StationImpl station) {
                    station.recalculateNearbyIndustries();
                }
            }

            game.inputHandling().setMouseTool(null);
            return HandleResult.CAPTURE;
        }
        return HandleResult.CONTINUE;
    }

    @Override
    public void onDeactivate() {
        deactivation.run();
    }
}
