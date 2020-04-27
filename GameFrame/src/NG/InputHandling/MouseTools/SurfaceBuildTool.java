package NG.InputHandling.MouseTools;

import NG.Core.Game;
import NG.GUIMenu.Components.SComponent;

/**
 * @author Geert van Ieperen created on 27-1-2019.
 */
public abstract class SurfaceBuildTool extends AbstractMouseTool {
    protected final Runnable deactivation;
    private final MouseTool defaultMouseTool;

    public SurfaceBuildTool(Game game, Runnable deactivation) {
        super(game);
        this.deactivation = deactivation;
        defaultMouseTool = game.inputHandling().getDefaultMouseTool();
    }

    @Override
    public void apply(SComponent component, int xSc, int ySc) {
        defaultMouseTool.apply(component, xSc, ySc);
    }

    @Override
    public void dispose() {
        deactivation.run();
    }
}
