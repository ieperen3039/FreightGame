package NG.InputHandling.MouseTools;

import NG.Core.Game;
import NG.GUIMenu.Components.SComponent;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3fc;

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
    public void apply(Vector3fc position, int xSc, int ySc) {
        apply(new Vector2f(position.x(), position.y()));
    }

    protected abstract void apply(Vector2fc position);

    @Override
    public void apply(SComponent component, int xSc, int ySc) {
        defaultMouseTool.apply(component, xSc, ySc);
    }

    @Override
    public void dispose() {
        deactivation.run();
    }
}
