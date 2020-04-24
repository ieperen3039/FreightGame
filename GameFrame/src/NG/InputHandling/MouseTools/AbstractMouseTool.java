package NG.InputHandling.MouseTools;

import NG.Core.Game;
import NG.GUIMenu.FrameManagers.FrameGUIManager;
import NG.InputHandling.MouseMoveListener;
import NG.InputHandling.MouseReleaseListener;
import org.joml.Vector2i;

/**
 * @author Geert van Ieperen created on 24-4-2020.
 */
public abstract class AbstractMouseTool implements MouseTool {
    protected int dragButton = 0;
    protected MouseMoveListener dragListener = null;
    protected MouseReleaseListener releaseListener = null;
    protected Game game;

    private int button;

    public AbstractMouseTool(Game game) {
        this.game = game;
    }

    @Override
    public void onRelease(int button, int xSc, int ySc) {
        if (button != dragButton) return;

        dragListener = null;
        if (releaseListener != null) {
            releaseListener.onRelease(button, xSc, ySc);
            releaseListener = null;
        }
    }

    @Override
    public void mouseMoved(int xDelta, int yDelta) {
        if (dragListener == null) return;
        dragListener.mouseMoved(xDelta, yDelta);
    }

    @Override
    public void onClick(int button, int x, int y) {
        setButton(button);

        if (game.gui().checkMouseClick(this, x, y)) return;

        if (game.state().checkMouseClick(this, x, y)) return;
        game.map().checkMouseClick(this, x, y);
    }

    private void setButton(int button) {
        this.button = button;
    }

    protected int getButton() {
        return button;
    }

    @Override
    public void onScroll(float value) {
        Vector2i pos = game.window().getMousePosition();
        FrameGUIManager gui = game.gui();
        gui.checkMouseScroll(pos.x, pos.y, value);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
