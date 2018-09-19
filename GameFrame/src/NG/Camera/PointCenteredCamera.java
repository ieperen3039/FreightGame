package NG.Camera;

import NG.ActionHandling.GLFWListener;
import NG.ActionHandling.GLFWListener.DragListener;
import NG.Engine.Game;
import NG.Tools.Vectors;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

/**
 * The standard camera that rotates using dragging. Some of the code originates from the RobotRace sample code provided by the TU Eindhoven
 */
public class PointCenteredCamera implements Camera {

    private static final float ZOOM_SPEED = -0.1f;
    private static final float THETA_MIN = 0.01f;
    private static final float THETA_MAX = ((float) Math.PI) - 0.01f;
    private static final float PHI_MAX = (float) (2 * Math.PI);
    /** Ratio of distance in pixels dragged and radial change of camera. */
    private static final float DRAG_PIXEL_TO_RADIAN = -0.025f;
    private final int maxDist = 100;

    /** The point to which the camera is looking. */
    public final Vector3f focus;

    /** cached eye position */
    private Vector3f eye;

    /** we follow the ISO convention. Phi gives rotation, theta the height */
    private float theta;
    private float phi;
    private float vDist = 10f;

    private GLFWListener callbacks;
    private DragListener onDrag = new TurnCameraOnDrag();
    private Consumer<Double> scrollListener = s -> vDist = (float) Math.min(vDist * ((ZOOM_SPEED * s) + 1f), maxDist);

    public PointCenteredCamera(Vector3f eye, Vector3f focus) {
        this.focus = focus;
        this.eye = eye;

        Vector3f focToEye = new Vector3f(eye).sub(focus);
        vDist = focToEye.length();
        phi = getPhi(focToEye);
        theta = getTheta(focToEye, vDist);
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
        this.focus = focus;
        this.theta = theta;
        this.phi = phi;
    }

    @Override
    public void init(Game game) {
        updatePosition(0);
        callbacks = game.callbacks();
        callbacks.onMouseDrag(onDrag);
        callbacks.onMouseScroll(scrollListener);
    }

    @Override
    public void cleanup() {
        callbacks.removeListener(onDrag);
    }

    /**
     * updates the eye position
     * @param deltaTime not used
     */
    @Override
    public void updatePosition(float deltaTime) {
        eye = getEyePosition();
    }

    private Vector3f getEyePosition() {
        double eyeX = vDist * Math.sin(theta) * Math.cos(phi);
        double eyeY = vDist * Math.sin(theta) * Math.sin(phi);
        double eyeZ = vDist * Math.cos(theta);

        final Vector3f eye = new Vector3f((float) eyeX, (float) eyeY, (float) eyeZ);
        return eye.add(focus, eye);
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

    private class TurnCameraOnDrag extends DragListener {
        TurnCameraOnDrag() {
            super(GLFW_MOUSE_BUTTON_RIGHT);
        }

        @Override
        public void buttonPressed() {

        }

        @Override
        public void buttonReleased() {

        }

        public void mouseDragged(int deltaX, int deltaY) {
            int s = 1;

            theta += deltaY * DRAG_PIXEL_TO_RADIAN * s;
            phi += deltaX * DRAG_PIXEL_TO_RADIAN * s;

            theta = Math.max(THETA_MIN, Math.min(THETA_MAX, theta));
            phi = phi % PHI_MAX;
        }
    }
}
