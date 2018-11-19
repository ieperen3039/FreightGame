package NG.Rendering;

import NG.DataStructures.Color4f;
import NG.DataStructures.MatrixStack.SGL;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * a light object, which can be either a point-light or an infinitely far light
 * @author Geert van Ieperen. Created on 18-11-2018.
 */
public class Light {
    private Vector4f position;
    private Color4f color;

    public Light(float x, float y, float z, Color4f color, float brightness) {
        this(new Vector3f(x, y, z), color, brightness);
    }

    public Light(Vector3f position, Color4f color, float brightness) {
        this(position, color, brightness, false);
    }

    public Light(Vector3f toLight, Color4f color, float brightness, boolean isInfinite) {
        this.position = new Vector4f(toLight, isInfinite ? 0.0f : 1.0f);
        this.color = new Color4f(color, color.alpha * brightness);
    }

    public boolean isInfinite() {
        return position.w == 0.0f;
    }

    public void draw(SGL gl) {
        gl.setLight(color, position);
    }
}
