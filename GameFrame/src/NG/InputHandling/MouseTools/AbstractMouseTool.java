package NG.InputHandling.MouseTools;

import NG.Core.Game;
import NG.GUIMenu.Components.SComponent;
import NG.GUIMenu.FrameManagers.FrameGUIManager;
import NG.InputHandling.MouseReleaseListener;
import NG.InputHandling.MouseScrollListener;
import org.joml.Vector2i;

/**
 * @author Geert van Ieperen created on 24-4-2020.
 */
public abstract class AbstractMouseTool implements MouseTool {
    protected Game game;

    private MouseReleaseListener releaseListener;
    private int pressedButton = -1;

    public AbstractMouseTool(Game game) {
        this.game = game;
    }

    protected int getButton() {
        return pressedButton;
    }

    @Override
    public void onClick(int button, int x, int y) {
        if (pressedButton != -1) return;
        pressedButton = button;

        if (game.gui().checkMouseClick(button, x, y)) {
            releaseListener = game.gui();
            return;
        }

        game.camera().onClick(button, x, y);
        releaseListener = game.camera();

        if (game.state().checkMouseClick(this, x, y)) return;
        game.map().checkMouseClick(this, x, y);
    }

    @Override
    public void onRelease(int button, int xSc, int ySc) {
        if (button != pressedButton) return;
        pressedButton = -1;

        releaseListener.onRelease(button, xSc, ySc);
        releaseListener = null;
    }

    @Override
    public void onScroll(float value) {
        Vector2i pos = game.window().getMousePosition();
        FrameGUIManager gui = game.gui();

        SComponent component = gui.getComponentAt(pos.x, pos.y);

        if (component != null) {
            if (component instanceof MouseScrollListener) {
                MouseScrollListener listener = (MouseScrollListener) component;
                listener.onScroll(value);
            }

            return;
        }

        // camera
        game.camera().onScroll(value);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public void mouseMoved(int xDelta, int yDelta, float xPos, float yPos) {
        game.gui().mouseMoved(xDelta, yDelta, xPos, yPos);
        game.camera().mouseMoved(xDelta, yDelta, xPos, yPos);
    }
}
