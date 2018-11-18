package NG.Tools;

import org.joml.Vector3f;

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
}
