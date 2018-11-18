package NG.Camera;

import NG.ActionHandling.MousePositionListener;
import NG.Engine.Game;
import NG.Tools.Vectors;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen. Created on 18-11-2018.
 */
public class TycoonFixedCamera implements Camera, MousePositionListener {
    private static final int SCREEN_MOVE_MINIMUM = 1;
    private static final float ZOOM_SPEED = 0.1f;
    private final Vector3f focus = new Vector3f();
    private float eyeDist;
    private Game game;
    private int mouseXPos;
    private int mouseYPos;

    public TycoonFixedCamera(Vector3fc initialFocus, int eyeDistance) {
        eyeDist = eyeDistance;
        focus.set(initialFocus);
    }

    @Override
    public void init(Game game) {
        this.game = game;
        game.callbacks().addMousePositionListener(this);
    }

    @Override
    public Vector3f vectorToFocus() {
        return null;
    }

    @Override
    public void updatePosition(float deltaTime) {
        // x movement
        if (mouseXPos < SCREEN_MOVE_MINIMUM) {
            float value = positionToMovement(mouseXPos) * deltaTime;
            focus.add(value, value, 0);

        } else {
            int xInv = mouseXPos - game.window().getWidth();
            if (xInv < SCREEN_MOVE_MINIMUM) {
                float value = positionToMovement(xInv) * deltaTime;
                focus.sub(value, value, 0);
            }
        }

        // y movement
        if (mouseYPos < SCREEN_MOVE_MINIMUM) {
            float value = positionToMovement(mouseYPos) * deltaTime;
            focus.add(-value, value, 0);

        } else {
            int yInv = mouseYPos - game.window().getHeight();
            if (yInv < SCREEN_MOVE_MINIMUM) {
                float value = positionToMovement(yInv) * deltaTime;
                focus.sub(-value, value, 0);
            }
        }
    }

    @Override
    public Vector3fc getEye() {
        return new Vector3f(focus).add(-eyeDist, -eyeDist, eyeDist);
    }

    @Override
    public Vector3fc getFocus() {
        return focus;
    }

    @Override
    public Vector3fc getUpVector() {
        return Vectors.zVector();
    }

    @Override
    public void setFocus(Vector3fc focus) {
        this.focus.set(focus);
    }

    @Override
    public void mouseScrolled(float value) {
        eyeDist *= (ZOOM_SPEED * value) + 1f;
    }

    @Override
    public void cleanup() {
        game.callbacks().removeListener(this);
    }

    @Override
    public void mouseMoved(int xPos, int yPos) {
        mouseXPos = xPos;
        mouseYPos = yPos;
    }

    /**
     * gives how much the camera should move given how many pixels the mouse is from the edge of the screen
     * @param pixels the number of pixels between the mouse and the edge of the screen, at least 0
     * @return how fast the camera should move in the direction
     */
    protected float positionToMovement(int pixels) {
        return 1f / (pixels + 1);
    }
}
