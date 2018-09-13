package Camera;

import org.joml.Vector3f;

/**
 * @author Geert van Ieperen created on 29-10-2017.
 */
public interface Camera {

    /**
     * a copy of the direction vector of the eye of the camera to the focus of the camera.
     * @return {@link #getEye()}.to({@link #getFocus()}) The length of this vector may differ by implementation
     */
    Vector3f vectorToFocus();

    /**
     * updates the state of this camera according to the given passed time.
     * @param deltaTime the number of seconds passed since last update. This may be real-time or in-game time
     */
    void updatePosition(float deltaTime);

    /** a copy of the position of the camera itself */
    Vector3f getEye();

    /** a copy of the point in space where the camera looks to */
    Vector3f getFocus();

    /** a copy of the direction of up, the length of this vector is undetermined. */
    Vector3f getUpVector();

    /** @return the velocity of the eye of this camera. This is equal to the eye displacement scaled by 1/deltaTime */
    Vector3f getVelocity();
}
