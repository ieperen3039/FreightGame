package NG.Camera;


import NG.Engine.FreightGame;
import NG.Settings.KeyBinding;
import org.joml.Vector3f;

/**
 * @author Geert van Ieperen created on 22-12-2017. a camera that doesn't move
 */
public class StaticCamera implements Camera {

    public static final float QUARTER = (float) (Math.PI / 2);
    private Vector3f eye, focus;
    private Vector3f up;
    private FreightGame game;

    public StaticCamera(FreightGame game, Vector3f focus, Vector3f up) {
        this.eye = new Vector3f(20, 20, 20);
        this.focus = focus;
        this.up = up;
        this.game = game;
    }

    @Override
    public void init() {
        game.registerKeyPressListener(k -> {
            switch (KeyBinding.get(k)) {
                case CAMERA_LEFT:
                    eye.rotateZ(QUARTER);
                    break;
                case CAMERA_RIGHT:
                    eye.rotateZ(-QUARTER);
                    break;
            }
        });
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
}
