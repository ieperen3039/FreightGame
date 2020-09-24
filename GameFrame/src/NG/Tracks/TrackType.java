package NG.Tracks;

import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Valuta;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.MaterialShader;
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
    float RADIUS_SPEED_RATIO = 1.5f;
    float MINIMUM_RADIUS = 1f;

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
     * sets the material properties of this track in the shader. for example: {@code shader.setMaterial(Material.ROUGH,
     * Color4f.WHITE);}
     * @param shader the current shader
     * @param track
     * @param color
     */
    void setMaterial(MaterialShader shader, TrackPiece track, Color4f color);

    /** @return the maximum speed of a straight track */
    float getMaximumSpeed();

    /**
     * Given a circle radius, gives the maximum speed of this track.
     * @param radius the rail circle radius
     * @return the maximum speed, or 0 if the radius is too small
     */
    default float getMaximumSpeed(float radius) {
        if (radius < MINIMUM_RADIUS) return 0;

        return Math.min(getMaximumSpeed(), radius * RADIUS_SPEED_RATIO);
    }

    Valuta getCostPerMeter();

    static Mesh clickBoxStraight(Vector3fc displacement) {
        float length = displacement.length();

        return generateFunctional(
                t -> new Vector3f(displacement).mul(t),
                t -> new Vector3f(displacement).div(length),
                Settings.CLICK_BOX_WIDTH, Settings.CLICK_BOX_HEIGHT, 2
        ).toFlatMesh();
    }

    static Mesh clickBoxCircle(float radius, float angle, float endHeight) {
        float hDelta = endHeight / (angle * radius);
        float length = Math.abs(radius * angle);

        int resolution = (int) Math.max(Settings.CLICK_BOX_RESOLUTION * length, Math.abs((8 / Math.PI) * angle));

        return generateFunctional(
                t -> new Vector3f(radius * Math.cos(angle * t), radius * Math.sin(angle * t), endHeight * t),
                t -> new Vector3f(-Math.sin(angle * t), Math.cos(angle * t), hDelta).normalize(),
                Settings.CLICK_BOX_WIDTH, Settings.CLICK_BOX_HEIGHT, resolution
        ).toFlatMesh();
    }

    static CustomShape generateFunctional(
            Function<Float, Vector3f> function, Function<Float, Vector3f> derivative,
            float width, float height, int resolution
    ) {
        resolution = Math.min(10_000, Math.max(1, resolution));

        Vector3fc startPos = function.apply(0f);
        Vector3f startDir = derivative.apply(0f);
        Vector3f pointSide = new Vector3f(startDir).cross(Vectors.Z).normalize(width / 2);
        Vector3f pointUp = new Vector3f(pointSide).cross(startDir).normalize(height);

        CustomShape frame = new CustomShape();

        Vector3f point1;
        Vector3f pp1 = new Vector3f(startPos).add(pointSide);
        Vector3f pn1 = new Vector3f(startPos).add(pointSide).sub(pointUp);
        Vector3f np1 = new Vector3f(startPos).sub(pointSide);
        Vector3f nn1 = new Vector3f(startPos).sub(pointSide).sub(pointUp);
        frame.addQuad(pp1, pn1, nn1, np1, new Vector3f(startDir).negate());

        Vector3f dir1 = startDir;
        Vector3f pp2 = new Vector3f();
        Vector3f pn2 = new Vector3f();
        Vector3f np2 = new Vector3f();
        Vector3f nn2 = new Vector3f();

        float delta = 1f / resolution;
        for (int i = 1; i <= resolution; i++) {
            float t = (i == resolution) ? 1f : (i * delta);

            pp2.set(pp1);
            pn2.set(pn1);
            np2.set(np1);
            nn2.set(nn1);

            point1 = function.apply(t);
            dir1 = derivative.apply(t);

            pointSide.set(dir1).cross(Vectors.Z).normalize(width / 2);
            pointUp.set(pointSide).cross(dir1).normalize(height);

            pp1.set(point1).add(pointSide);
            pn1.set(point1).add(pointSide).sub(pointUp);
            np1.set(point1).sub(pointSide);
            nn1.set(point1).sub(pointSide).sub(pointUp);

            frame.addQuad(np1, pp1, pp2, np2, pointUp);
            frame.addQuad(pp1, pn1, pn2, pp2, pointSide);
            frame.addQuad(pn1, nn1, nn2, pn2, pointUp.negate());
            frame.addQuad(nn1, np1, np2, nn2, pointSide.negate());
        }

        frame.addQuad(pp1, pn1, nn1, np1, dir1);
        return frame;
    }
}
