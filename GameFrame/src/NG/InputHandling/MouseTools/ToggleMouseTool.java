package NG.InputHandling.MouseTools;

import NG.Core.Game;

/**
 * @author Geert van Ieperen created on 27-1-2019.
 */
public abstract class ToggleMouseTool extends AbstractMouseTool {
    protected final Runnable deactivation;

    public ToggleMouseTool(Game game, Runnable deactivation) {
        super(game);
        this.deactivation = deactivation;
    }

    @Override
    public void dispose() {
        deactivation.run();
    }
}
