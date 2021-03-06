package NG.InputHandling.MouseTool;

import NG.Core.Game;
import NG.GUIMenu.FrameManagers.UIFrameManager;
import NG.InputHandling.MouseReleaseListener;
import NG.Rendering.MatrixStack.SGL;
import NG.Settings.Settings;
import NG.Tools.Vectors;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import static NG.InputHandling.MouseTool.AbstractMouseTool.MouseAction.*;

/**
 * @author Geert van Ieperen created on 24-4-2020.
 */
public abstract class AbstractMouseTool implements MouseTool {
    protected Game game;

    private MouseReleaseListener releaseListener;
    private MouseAction mouseAction = HOVER;

    public enum MouseAction {
        PRESS_ACTIVATE, PRESS_DEACTIVATE, DRAG_ACTIVATE, DRAG_DEACTIVATE, HOVER
    }

    public AbstractMouseTool(Game game) {
        this.game = game;
        releaseListener = game.camera();
    }

    protected MouseAction getMouseAction() {
        return mouseAction;
    }

    @Override
    public void onClick(int button, int x, int y) {
        if (mouseAction != HOVER) return;

        // TODO keybindings
        switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT:
                mouseAction = PRESS_ACTIVATE;
                break;
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT:
                game.inputHandling().setMouseTool(null);
                return;
        }

        boolean guiWasClicked = game.gui().checkMouseClick(button, x, y);
        if (guiWasClicked) {
            releaseListener = game.gui();
            return;
        }

        game.camera().onClick(button, x, y);
        releaseListener = game.camera();

        checkMapAndEntities(x, y);
    }

    @Override
    public void onRelease(int button) {
        mouseAction = HOVER;

        // this is the case when a mouse-down caused a mouse tool switch
        if (releaseListener != null) {
            releaseListener.onRelease(button);
            releaseListener = null;
        }
    }

    @Override
    public void onScroll(float value) {
        Vector2i pos = game.window().getMousePosition();
        UIFrameManager gui = game.gui();

        if (gui.covers(pos.x, pos.y)) {
            gui.onScroll(value);
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
    public final void onMouseMove(int xDelta, int yDelta, float xPos, float yPos) {
        if (mouseAction == PRESS_ACTIVATE) mouseAction = DRAG_ACTIVATE;
        if (mouseAction == PRESS_DEACTIVATE) mouseAction = DRAG_DEACTIVATE;

        game.gui().onMouseMove(xDelta, yDelta, xPos, yPos);
        game.camera().onMouseMove(xDelta, yDelta, xPos, yPos);

        // TODO don't check if the result is unused

        checkMapAndEntities((int) xPos, (int) yPos);
    }

    /**
     * runs {@code checkMouseClick} for the game state and game map
     * @param xSc
     * @param ySc
     */
    protected void checkMapAndEntities(int xSc, int ySc) {
        Vector3f origin = new Vector3f();
        Vector3f direction = new Vector3f();
        Vectors.windowCoordToRay(game, xSc, ySc, origin, direction);
        direction.normalize(Settings.Z_FAR - Settings.Z_NEAR);

        if (game.state().checkMouseClick(this, xSc, ySc, origin, direction)) return;
        game.map().checkMouseClick(this, xSc, ySc, origin, direction);
    }

    @Override
    public void draw(SGL gl) {
        // TODO fancy cursor?
    }

}
