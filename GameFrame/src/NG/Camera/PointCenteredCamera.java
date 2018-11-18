package NG.Camera;

import NG.ActionHandling.GLFWListener;
import NG.ActionHandling.MouseMoveListener;
import NG.ActionHandling.MouseScrollListener;
import NG.Engine.Game;
import NG.Settings.Settings;
import NG.Tools.Vectors;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

/**
 * The standard camera that rotates using dragging. Some of the code originates from the RobotRace sample code provided by the TU Eindhoven
 */
public class PointCenteredCamera implements Camera, MouseScrollListener {

    private static final float THETA_MIN = 0.01f;
    private static final float THETA_MAX = ((float) Math.PI) - 0.01f;
    private static final float PHI_MAX = (float) (2 * Math.PI);
    /** Ratio of distance in pixels dragged and radial change of camera. */
    private static final float DRAG_PIXEL_TO_RADIAN = -0.025f;

    /** The point to which the camera is looking. */
    private final Vector3f focus = new Vector3f();

    /** cached eye position */
    private final Vector3f eye = new Vector3f();

    /** we follow the ISO convention. Phi gives rotation, theta the height */
    private float theta;
    private float phi;
    private float vDist = 10f;

    private GLFWListener callbacks;
    private MouseMoveListener onDrag = new TurnCameraOnDrag();
    private Game game;

    public PointCenteredCamera(Vector3f eye, Vector3f focus) {
        set(eye, focus, null);
    }

    /**
     * @param eye normalized vector to eye
     * @return phi
     */
    private static float getPhi(Vector3f eye) {
        return (float) Math.atan2(eye.y(), eye.x());
    }

    /**
     * @param eye   normalized vector to eye
     * @param vDist distance to origin
     * @return theta
     */
    private static float getTheta(Vector3f eye, float vDist) {
        return (float) Math.acos(eye.z() / vDist);
    }

    public PointCenteredCamera(Vector3f focus, float theta, float phi) {
        this.focus.set(focus);
        this.theta = theta;
        this.phi = phi;
    }

    @Override
    public void init(Game game) {
        this.game = game;
        updatePosition(0);
        callbacks = game.callbacks();
        callbacks.onMouseDrag(GLFW_MOUSE_BUTTON_LEFT, onDrag);
        callbacks.onMouseScroll(this);
    }

    @Override
    public void cleanup() {
        callbacks.removeMouseDragListener(GLFW_MOUSE_BUTTON_LEFT, onDrag);
    }

    /**
     * updates the eye position
     * @param deltaTime not used
     */
    @Override
    public void updatePosition(float deltaTime) {
        double eyeX = vDist * Math.sin(theta) * Math.cos(phi);
        double eyeY = vDist * Math.sin(theta) * Math.sin(phi);
        double eyeZ = vDist * Math.cos(theta);

        // add to focus and store in eye
        focus.add((float) eyeX, (float) eyeY, (float) eyeZ, eye);
    }

    @Override
    public Vector3f vectorToFocus() {
        return new Vector3f(eye).sub(focus);
    }

    @Override
    public Vector3fc getEye() {
        return eye;
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
    public void set(Vector3fc newEyePosition, Vector3fc newFocusPosition, Vector3fc newUpVector) {
        eye.set(newEyePosition);
        focus.set(newFocusPosition);

        Vector3f focToEye = new Vector3f(newEyePosition).sub(newFocusPosition);
        vDist = focToEye.length();
        phi = getPhi(focToEye);
        theta = getTheta(focToEye, vDist);
    }

    public void mouseScrolled(double s) {
        Settings set = game.settings();
        vDist = (float) Math.min(vDist * ((set.CAMERA_ZOOM_SPEED * s) + 1f), set.MAX_CAMERA_DIST);
    }

    private class TurnCameraOnDrag implements MouseMoveListener {
        TurnCameraOnDrag() {
        }

        public void mouseMoved(int xDelta, int yDelta) {
            final int s = 1;

            theta += yDelta * DRAG_PIXEL_TO_RADIAN * s;
            phi += xDelta * DRAG_PIXEL_TO_RADIAN * s;

            theta = Math.max(THETA_MIN, Math.min(THETA_MAX, theta));
            phi = phi % PHI_MAX;
        }
    }
}
