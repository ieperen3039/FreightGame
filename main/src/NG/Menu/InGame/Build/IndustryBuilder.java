package NG.Menu.InGame.Build;

import NG.Core.Game;
import NG.Entities.Entity;
import NG.Entities.Industry;
import NG.Entities.StationImpl;
import NG.GUIMenu.Components.SToggleButton;
import NG.InputHandling.MouseTool.AbstractMouseTool;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 21-6-2020.
 */
public class IndustryBuilder extends AbstractMouseTool {
    private final Runnable deactivation;
    private Industry.Properties properties;

    public IndustryBuilder(Game game, SToggleButton button, Industry.Properties properties) {
        super(game);
        this.deactivation = () -> button.setActive(false);
        this.properties = properties;
    }

    @Override
    public void apply(Entity entity, Vector3fc origin, Vector3fc direction) {

    }

    @Override
    public void apply(Vector3fc position, Vector3fc origin, Vector3fc direction) {
        if (getMouseAction() == MouseAction.PRESS_ACTIVATE) {
            Industry industry = new Industry(game, position, game.timer().getGameTime(), properties);
            game.state().addEntity(industry);

            // reset the industries of all entities
            // TODO make this less than quadratic runtime obnoxity
            for (Entity entity : game.state()) {
                if (entity instanceof StationImpl) {
                    StationImpl station = (StationImpl) entity;
                    station.recalculateNearbyIndustries();
                }
            }

            game.inputHandling().setMouseTool(null);
        }
    }

    @Override
    public void dispose() {
        deactivation.run();
    }
}
