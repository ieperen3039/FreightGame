package NG.Tools;

import org.joml.Math;
import org.joml.*;

import java.util.Locale;

/**
 * creates new copies of some standard vectors
 *
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class Vectors {
    private static final float PI = (float) Math.PI;

    public static Vector3f zeroVector() {
        return new Vector3f(0, 0, 0);
    }

    public static Vector3f zVector() {
        return new Vector3f(0, 0, 1);
    }

    public static Vector3f xVector() {
        return new Vector3f(1, 0, 0);
    }

    public static String toString(Vector3fc v) {
        return String.format(Locale.US, "(%1.3f, %1.3f, %1.3f)", v.x(), v.y(), v.z());
    }

    public static String toString(Vector2fc v) {
        return String.format(Locale.US, "(%1.3f, %1.3f)", v.x(), v.y());
    }

    public static String toString(Vector3ic v) {
        return String.format("(%d, %d, %d)", v.x(), v.y(), v.z());
    }

    public static String toString(Vector2ic v) {
        return String.format("(%d, %d)", v.x(), v.y());
    }

    public static String toString(Vector4fc v) {
        return String.format(Locale.US, "(%1.3f, %1.3f, %1.3f, %1.3f)", v.x(), v.y(), v.z(), v.w());
    }

    public static String stringAsVector(float x, float y) {
        return String.format("(%1.3f, %1.3f)", x, y);
    }

    /**
     * Rotates a two-dimensional vector on the z-axis in clockwise direction //TODO not counterclock?
     * @param target the vector to rotate
     * @param angle  the angle of rotation
     * @param dest   the vector to store the result
     * @return dest
     */
    public static Vector2f rotate(Vector2fc target, float angle, Vector2f dest) {
        float sin = sin(angle), cos = cos(angle);
        float tx = target.x();
        float ty = target.y();
        dest.x = tx * cos - ty * sin;
        dest.y = tx * sin + ty * cos;
        return dest;
    }

    /**
     * Rotates a two-dimensional vector on the z-axis
     * @param target the vector to rotate
     * @param angle  the angle of rotation
     * @return the target vector holding the result
     */
    public static Vector2f rotate(Vector2f target, int angle) {
        float sin = sin(angle), cos = cos(angle);
        float tx = target.x;
        float ty = target.y;
        target.x = tx * cos - ty * sin;
        target.y = tx * sin + ty * cos;
        return target;
    }

    // a few mathematical shortcuts
    public static float cos(float theta) {
        return (float) Math.cos(theta);
    }

    public static float sin(float theta) {
        return (float) Math.sin(theta);
    }

    /**
     * @param vector any vector
     * @return theta such that a vector with {@code x = {@link #cos(float)}} and {@code y = {@link #sin(float)}} gives
     * {@code vector}, normalized.
     */
    public static float arcTan(Vector2fc vector) {
        return (float) Math.atan2(vector.y(), vector.x());
    }

    /**
     * high(er)-precision calculation of the angle between two vectors.
     * @param v1 a vector
     * @param v2 another vector
     * @return the angle in randians between v1 and v2
     */
    public static float angle(Vector2fc v1, Vector2fc v2) {
        float x1 = v1.x();
        float y1 = v1.y();
        float x2 = v2.x();
        float y2 = v2.y();
        double length1Sqared = x1 * x1 + y1 * y1;
        double length2Sqared = x2 * x2 + y2 * y2;
        double dot = x1 * x2 + y1 * y2;
        double cos = dot / (java.lang.Math.sqrt(length1Sqared * length2Sqared));

        // Cull because sometimes cos goes above 1 or below -1 because of lost precision
        cos = cos < 1 ? cos : 1;
        cos = cos > -1 ? cos : -1;
        return (float) java.lang.Math.acos(cos);
    }

    /**
     * @param theta an angle in radians
     * @return the vector given by (cos(theta), sin(theta))
     */
    public static Vector2fc unitVector(float theta) {
        return new Vector2f(cos(theta), sin(theta));
    }
}
