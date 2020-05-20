package NG.Entities;

import NG.Rendering.MatrixStack.SGL;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 19-5-2020.
 */
public interface TrainElement {
    void draw(SGL gl, Vector3fc position, Quaternionfc rotation, Entity sourceEntity);

    float realLength();
}
