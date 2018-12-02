package NG.Tools;

import org.joml.*;

import java.util.Locale;

/**
 * creates new copies of some standard vectors
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class Vectors {
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
}
