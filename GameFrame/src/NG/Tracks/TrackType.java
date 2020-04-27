package NG.Tracks;

import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shapes.CustomShape;
import NG.Settings.Settings;
import NG.Tools.Vectors;
import org.joml.Math;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Function;

/**
 * A specific type of track where trains can move along. track.
 */
public interface TrackType {
    /** @return the name for this track type */
    String toString();

    /**
     * create a mesh for a part of a circle. This is executed once for each track that is placed down.
     * @param radius    the radius of the circle
     * @param angle     the angle of the circle to rotate. If positive, rotate counterclockwise. If negative, rotate
     *                  clockwise.
     * @param endHeight the height difference from the start, may be negative.
     * @return a mesh that describes a circle that starts at (radius, 0, 0) and ends at (radius * cos(angle), radius *
     * sin(angle), endHeight)
     */
    Mesh generateCircle(float radius, float angle, float endHeight);

    /**
     * generate a straight piece with the given length
     * @param displacement@return a mesh that describes a line that starts at (0, 0, 0) and ends at (0, length,
     *                            endHeight)
     */
    Mesh generateStraight(Vector3fc displacement);

    /**
     * Given a circle radius, gives the maximum speed of this track.
     * @param radius the rail circle radius
     * @return the maximum speed, or 0 if the radius is too small
     */
    default float getMaximumSpeed(float radius) {
        if (radius < minimumRadius()) return 0;
        return 100 - (100 / radius);
    }

    /**
     * @return the smallest allowed rail circle radius
     */
    float minimumRadius();

    static Mesh clickBoxStraight(Vector3fc displacement) {
        return generateFunctional(
                t -> Vectors.newZeroVector().lerp(displacement, t),
                t -> new Vector3f(displacement).normalize()
        );
    }

    static Mesh clickBoxCircle(float radius, float angle, float endHeight) {
        float hDelta = endHeight / (angle * radius);

        return generateFunctional(
                t -> new Vector3f(radius * Math.cos(angle * t), radius * Math.sin(angle * t), endHeight * t),
                t -> new Vector3f(-Math.sin(angle * t), Math.cos(angle * t), hDelta).normalize()
        );
    }

    static Mesh generateFunctional(Function<Float, Vector3f> function, Function<Float, Vector3f> derivative) {
        Vector3fc startPos = function.apply(0f);
        Vector3f startDir = derivative.apply(0f);
        Vector3f pointSide = new Vector3f(startDir).cross(Vectors.Z).normalize(Settings.CLICK_BOX_WIDTH / 2);
        Vector3f pointUp = new Vector3f(pointSide).cross(startDir).normalize(Settings.CLICK_BOX_HEIGHT / 2);

        CustomShape frame = new CustomShape();

        Vector3f point1;
        Vector3f pp1 = new Vector3f(startPos).add(pointSide).add(pointUp);
        Vector3f pn1 = new Vector3f(startPos).add(pointSide).sub(pointUp);
        Vector3f np1 = new Vector3f(startPos).sub(pointSide).add(pointUp);
        Vector3f nn1 = new Vector3f(startPos).sub(pointSide).sub(pointUp);
        frame.addQuad(pp1, pn1, nn1, np1, new Vector3f(startDir).negate());

        Vector3f dir1 = startDir;
        Vector3f pp2 = new Vector3f();
        Vector3f pn2 = new Vector3f();
        Vector3f np2 = new Vector3f();
        Vector3f nn2 = new Vector3f();

        float delta = 1f / Settings.RESOLUTION;
        for (int i = 1; i <= Settings.RESOLUTION; i++) {
            float t = (i == Settings.RESOLUTION) ? 1f : (i * delta);

            pp2.set(pp1);
            pn2.set(pn1);
            np2.set(np1);
            nn2.set(nn1);

            point1 = function.apply(t);
            dir1 = derivative.apply(t);

            pointSide.set(dir1).cross(Vectors.Z).normalize(Settings.CLICK_BOX_WIDTH / 2);
            pointUp.set(pointSide).cross(dir1).normalize(Settings.CLICK_BOX_HEIGHT / 2);

            pp1.set(point1).add(pointSide).add(pointUp);
            pn1.set(point1).add(pointSide).sub(pointUp);
            np1.set(point1).sub(pointSide).add(pointUp);
            nn1.set(point1).sub(pointSide).sub(pointUp);

            frame.addQuad(np1, pp1, pp2, np2, pointUp);
            frame.addQuad(pp1, pn1, pn2, pp2, pointSide);
            frame.addQuad(pn1, nn1, nn2, pn2, pointUp.negate());
            frame.addQuad(nn1, np1, np2, nn2, pointSide.negate());
        }

        frame.addQuad(pp1, pn1, nn1, np1, dir1);

        return frame.toFlatMesh();
    }
}
