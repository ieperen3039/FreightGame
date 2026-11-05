package io.github.ieperen3039.trainsinspace.Tools;

import org.joml.Vector3f;
import org.joml.primitives.Rayf;

public class Rays {
    public static Vector3f toPosition(Rayf ray, float t) {
        return new Vector3f(
                ray.oX + ray.dX * t,
                ray.oY + ray.dY * t,
                ray.oZ + ray.dZ * t
        );
    }
}
