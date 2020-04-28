package NG.InputHandling.MouseTools;

import NG.Core.Game;

/**
 * @author Geert van Ieperen created on 27-1-2019.
 */
public abstract class SurfaceBuildTool extends AbstractMouseTool {
    protected final Runnable deactivation;

    public SurfaceBuildTool(Game game, Runnable deactivation) {
        super(game);
        this.deactivation = deactivation;
    }

    @Override
    public void dispose() {
        deactivation.run();
    }
}
