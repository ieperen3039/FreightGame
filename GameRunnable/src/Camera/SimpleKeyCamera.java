package Camera;

import Tools.Vectors;
import org.joml.Vector3f;

/**
 * @author Geert van Ieperen created on 2-11-2017.
 */
public class SimpleKeyCamera implements Camera {

    /**
     * The up vector.
     */
    public final Vector3f up;
    /**
     * The position of the camera.
     */
    public Vector3f eye;
    /**
     * The point to which the camera is looking.
     */
    public Vector3f focus;

    public SimpleKeyCamera() {
        this(new Vector3f(0, 5, 2), Vectors.zeroVector(), Vectors.zVector());
    }

    public SimpleKeyCamera(Vector3f eye, Vector3f focus, Vector3f up) {
        this.eye = eye;
        this.focus = focus;
        this.up = up;
    }

    @Override
    public void updatePosition(float deltaTime) {
        Vector3f movement = Vectors.zeroVector();
        Vector3f left = Vectors.zVector().cross(eye);

//        if (input.isPressed(VK_LEFT)) {
//            movement.add(left);
//        }
//        if (input.isPressed(VK_RIGHT)) {
//            movement.add(left.mul(-1, new Vector3f()));
//        }
//        if (input.isPressed(VK_UP)) {
//            movement.add(Vectors.zVector());
//        }
//        if (input.isPressed(VK_DOWN)){
//            movement.add(Vectors.zVector().mul(-1));
//        }

        eye.add(movement.mul(deltaTime * 1f, new Vector3f()));
    }

    @Override
    public Vector3f vectorToFocus() {
        return focus.sub(eye, new Vector3f());
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
    public Vector3f getVelocity() {
        return Vectors.zeroVector();
    }
}
