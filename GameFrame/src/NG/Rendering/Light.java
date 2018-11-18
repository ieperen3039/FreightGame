package NG.Rendering;

import NG.DataStructures.Color4f;
import NG.DataStructures.MatrixStack.SGL;
import org.joml.Vector3f;

/**
 * @author Geert van Ieperen. Created on 18-11-2018.
 */
public class Light {
    private Vector3f position;
    private Color4f color;

    public Light(float x, float y, float z, Color4f color, float brightness) {
        this(new Vector3f(x, y, z), color, brightness);
    }

    public Light(Vector3f position, Color4f color, float brightness) {
        this.position = position;
        this.color = new Color4f(color, color.alpha * brightness);
    }

    public void draw(SGL gl) {
        gl.setLight(position, color);
    }
}
