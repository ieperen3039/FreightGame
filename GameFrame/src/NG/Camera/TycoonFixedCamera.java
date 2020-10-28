package NG.Camera;

import NG.Core.Game;
import NG.Rendering.GLFWWindow;
import NG.Settings.Settings;
import NG.Tools.Vectors;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

/**
 * A camera implementation that can be moved by holding the mouse to the corners of the screen
 * @author Geert van Ieperen. Created on 18-11-2018.
 */
public class TycoonFixedCamera implements Camera {
    private static final float BORDER_MOVE_SPEED = 0.6f;
    private static final int SCREEN_MOVE_BORDER_SIZE = 50;
    private static final int SCREEN_DEAD_ZONE = 30;
    private static final float CAMERA_ZOOM_SPEED = 0.1f;

    private static final float ZOOM_SPEED_LIMIT = 0.5f;
    private static final float ROTATION_MODIFIER = 0.002f;
    public static final float DRAG_MODIFIER = 0.0015f;

    private final Vector3f focus = new Vector3f();
    private final Vector3f eyeOffset;

    private Game game;
    private float mouseXPos;
    private float mouseYPos;
    private boolean isBeingRotated = false;
    private boolean isBeginMoved = false;

    /**
     * a camera that always has the same angle to the ground. The angle can be set by the ratio between eyeOffset and
     * eyeHeight. The camera will start rotated on 45 degrees around the z-axis
     * @param initialFocus the position of the camera focus
     * @param eyeOffset    the x-offset of the camera
     * @param eyeHeight    the z-offset of the camera
     */
    public TycoonFixedCamera(Vector3fc initialFocus, float eyeOffset, float eyeHeight) {
        this.eyeOffset = new Vector3f(-eyeOffset, 0, eyeHeight);
        this.eyeOffset.rotateZ((float) (Math.PI / 4));
        focus.set(initialFocus);
    }

    @Override
    public void init(Game game) {
        this.game = game;

        Vector2i mousePosition = game.window().getMousePosition();
        mouseXPos = mousePosition.x;
        mouseYPos = mousePosition.y;
    }

    @Override
    public Vector3fc vectorToFocus() {
        return new Vector3f(eyeOffset).negate();
    }

    @Override
    public void updatePosition(float deltaTime) {
        // prevents side-of-window movement when rotating or dragging
        if (isBeingRotated || isBeginMoved) return;
        // prevent overshooting when camera is not updated.
        deltaTime = Math.min(deltaTime, 0.5f);

        Vector3f eyeDir = new Vector3f(eyeOffset);
        GLFWWindow window = game.window();
        int width = window.getWidth();
        int height = window.getHeight();

        // correction for toolbar
        int corrMouseYPos = (int) mouseYPos;
        int corrMouseXPos = (int) mouseXPos;

        // x movement
        if (corrMouseXPos < SCREEN_MOVE_BORDER_SIZE) {
            float value = positionToMovement(corrMouseXPos) * deltaTime;
            eyeDir.normalize(value).cross(getUpVector());
            focus.add(eyeDir.x, eyeDir.y, 0);

        } else {
            int xInv = width - corrMouseXPos;
            if (xInv < SCREEN_MOVE_BORDER_SIZE) {
                float value = positionToMovement(xInv) * deltaTime;
                eyeDir.normalize(value).cross(getUpVector());
                focus.sub(eyeDir.x, eyeDir.y, 0);
            }
        }

        eyeDir.set(eyeOffset);
        // y movement
        if (corrMouseYPos < SCREEN_MOVE_BORDER_SIZE) {
            float value = positionToMovement(corrMouseYPos) * deltaTime;
            eyeDir.normalize(value);
            focus.sub(eyeDir.x, eyeDir.y, 0);

        } else {
            int yInv = height - corrMouseYPos;
            if (yInv < SCREEN_MOVE_BORDER_SIZE) {
                float value = positionToMovement(yInv) * deltaTime;
                eyeDir.normalize(value);
                focus.add(eyeDir.x, eyeDir.y, 0);
            }
        }
    }

    @Override
    public Vector3fc getEye() {
        return new Vector3f(focus).add(eyeOffset);
    }

    @Override
    public Vector3fc getFocus() {
        return focus;
    }

    @Override
    public Vector3fc getUpVector() {
        return Vectors.Z;
    }

    @Override
    public void set(Vector3fc focus, Vector3fc eye) {
        this.focus.set(focus);
        Vector3f offset = new Vector3f(eye).sub(focus);
        this.eyeOffset.set(offset);
    }

    @Override
    public boolean isIsometric() {
        return game.settings().ISOMETRIC_VIEW;
    }

    @Override
    public void onScroll(float value) {
        resetFocus();

        Settings s = game.settings();
        float maxZoom = s.MAX_CAMERA_DIST;

        float v = Math.max(Math.min(CAMERA_ZOOM_SPEED * -value, ZOOM_SPEED_LIMIT), -ZOOM_SPEED_LIMIT);
        eyeOffset.mul(v + 1f);
        float minZoom = s.MIN_CAMERA_DIST;

        if (eyeOffset.lengthSquared() > maxZoom * maxZoom) {
            eyeOffset.normalize(maxZoom);
        } else if (eyeOffset.lengthSquared() < minZoom * minZoom) {
            eyeOffset.normalize(minZoom);
        }
    }

    @Override
    public void cleanup() {
    }

    /**
     * gives how much the camera should move given how many pixels the mouse is from the edge of the screen
     * @param pixels the number of pixels between the mouse and the edge of the screen, at least 0
     * @return how fast the camera should move in the direction
     */
    protected float positionToMovement(int pixels) {
        if (pixels >= SCREEN_MOVE_BORDER_SIZE) return 0;

        int offset = Math.min(SCREEN_MOVE_BORDER_SIZE - pixels, SCREEN_DEAD_ZONE);
        return offset * (1f / (SCREEN_MOVE_BORDER_SIZE - SCREEN_DEAD_ZONE)) * BORDER_MOVE_SPEED * eyeOffset.length();
    }

    @Override
    public void onMouseMove(int xDelta, int yDelta, float xPos, float yPos) {
//        float exactDelta = xPos - mouseXPos;
        mouseXPos = xPos;
        mouseYPos = yPos;

        if (isBeingRotated) {
            float angle = -xDelta * ROTATION_MODIFIER;
            eyeOffset.rotateZ(angle);

        } else if (isBeginMoved) {
            // x movement
            Vector3f eyeDir = new Vector3f(eyeOffset);
            eyeDir.mul(xDelta * DRAG_MODIFIER).cross(getUpVector());
            focus.add(eyeDir.x, eyeDir.y, 0);

            // y movement
            eyeDir.set(eyeOffset).mul(yDelta * DRAG_MODIFIER);
            focus.sub(eyeDir.x, eyeDir.y, 0);
        }
    }

    @Override
    public void onClick(int button, int xPos, int yPos) {
        if (button == GLFW_MOUSE_BUTTON_MIDDLE) {
            isBeingRotated = true;
            resetFocus();

        } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
            isBeginMoved = true;
        }
    }

    private void resetFocus() {
        // set focus to middle of screen
        Vector3f eyeMidOrigin = new Vector3f();
        Vector3f eyeMidDirection = new Vector3f();
        int width = game.window().getWidth();
        int height = game.window().getHeight();

        Vectors.windowCoordToRay(game, width / 2, height / 2, eyeMidOrigin, eyeMidDirection);
        eyeMidDirection.normalize(Settings.Z_FAR - Settings.Z_NEAR);
        Float tFrac = game.map().gridMapIntersection(eyeMidOrigin, eyeMidDirection);

        if (tFrac != null) {
            Vector3fc eyePos = getEye();
            focus.set(eyeMidDirection).mul(tFrac).add(eyeMidOrigin);
            eyeOffset.set(eyePos).sub(focus);
        }
    }

    @Override
    public void onRelease(int button) {
        isBeingRotated = false;
        isBeginMoved = false;
    }
}

