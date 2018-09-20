package NG.Camera;

import NG.ActionHandling.GLFWListener;
import NG.ActionHandling.KeyPressListener;
import NG.Engine.Game;
import NG.Settings.KeyBinding;
import org.joml.Vector3f;

/**
 * @author Geert van Ieperen created on 22-12-2017. a camera that doesn't move
 */
public class StaticCamera implements Camera, KeyPressListener {

    public static final float EIGHTST = (float) (Math.PI / 4);
    private Vector3f eye, focus;
    private Vector3f up;
    private GLFWListener callbacks;

    public StaticCamera(Vector3f focus, Vector3f up) {
        this.eye = new Vector3f(20, 20, 20);
        this.focus = focus;
        this.up = up;
    }

    @Override
    public void init(Game game) {
        callbacks = game.callbacks();

        callbacks.onKeyPress(this);
    }

    @Override
    public void cleanup() {
        callbacks.removeListener(this);
    }

    @Override
    public Vector3f vectorToFocus() {
        return new Vector3f(focus).sub(eye);
    }

    @Override
    public void updatePosition(float deltaTime) {
    }

    @Override
    public Vector3f getEye() {
        return eye;
    }

    @Override
    public Vector3f getFocus() {
        return focus;
    }

    @Override
    public Vector3f getUpVector() {
        return up;
    }

    @Override
    public void keyPressed(int k) {
        switch (KeyBinding.get(k)) {
            case CAMERA_LEFT:
                eye.rotateZ(EIGHTST);
                break;
            case CAMERA_RIGHT:
                eye.rotateZ(-EIGHTST);
                break;
        }
    }
}
