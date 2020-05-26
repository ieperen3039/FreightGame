package NG.InputHandling.MouseTools;

import NG.Core.Game;
import NG.GUIMenu.Components.SComponent;
import NG.GUIMenu.FrameManagers.FrameGUIManager;
import NG.InputHandling.MouseReleaseListener;
import NG.InputHandling.MouseScrollListener;
import NG.Network.RailNode;
import NG.Rendering.MatrixStack.SGL;
import NG.Settings.Settings;
import NG.Tools.Vectors;
import NG.Tracks.RailTools;
import NG.Tracks.TrackPiece;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFW;

import static NG.InputHandling.MouseTools.AbstractMouseTool.MouseAction.*;

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
                mouseAction = PRESS_DEACTIVATE;
                break;
        }

        if (game.gui().checkMouseClick(button, x, y)) {
            releaseListener = game.gui();
            return;
        }

        game.camera().onClick(button, x, y);
        releaseListener = game.camera();

        checkMapAndEntities(x, y);
    }

    @Override
    public void onRelease(int button, int xSc, int ySc) {
        mouseAction = HOVER;

        // this is the case when a mouse-down caused a mouse tool switch
        if (releaseListener != null) {
            releaseListener.onRelease(button, xSc, ySc);
            releaseListener = null;
        }
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
    public final void mouseMoved(int xDelta, int yDelta, float xPos, float yPos) {
        if (mouseAction == PRESS_ACTIVATE) mouseAction = DRAG_ACTIVATE;
        if (mouseAction == PRESS_DEACTIVATE) mouseAction = DRAG_DEACTIVATE;

        game.gui().mouseMoved(xDelta, yDelta, xPos, yPos);
        game.camera().mouseMoved(xDelta, yDelta, xPos, yPos);

        // TODO don't check if the result is unused

        checkMapAndEntities((int) xPos, (int) yPos);
    }

    /**
     * runs {@code checkMouseClick} for the game state and game map
     * @param xSc
     * @param ySc
     */
    private void checkMapAndEntities(int xSc, int ySc) {
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

    protected static float getFraction(TrackPiece trackPiece, Vector3fc origin, Vector3fc direction) {
        float fraction = trackPiece.getFractionOfClosest(origin, direction);
        if (fraction < 0) {
            fraction = 0;
        } else if (fraction > 1) {
            fraction = 1;
        }
        return fraction;
    }

    protected static RailNode getOrCreateNode(TrackPiece trackPiece, float fraction, Game game) {
        if (game.keyControl().isControlPressed() || trackPiece.isStatic()) {
            if (fraction < 0.5f) {
                return trackPiece.getStartNode();
            } else {
                return trackPiece.getEndNode();
            }
        } else {
            double gameTime = game.timer().getGameTime();
            return RailTools.createSplit(game, trackPiece, fraction, gameTime);
        }
    }
}
