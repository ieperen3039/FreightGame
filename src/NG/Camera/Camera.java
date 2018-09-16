package NG.Camera;

import NG.Engine.Game;
import NG.Engine.GameModule;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 29-10-2017.
 */
public interface Camera extends GameModule {

    /**
     * initialize the camera and its fields
     */
    void init(Game game);

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
    Vector3fc getEye();

    /** a copy of the point in space where the camera looks to */
    Vector3fc getFocus();

    /** a copy of the direction of up, the length of this vector is undetermined. */
    Vector3fc getUpVector();

}
