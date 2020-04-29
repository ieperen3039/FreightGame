package NG.Entities;

import NG.Core.GameObject;
import NG.Rendering.MatrixStack.SGL;

/**
 * @author Geert van Ieperen created on 29-4-2020.
 */
public interface Station extends GameObject, Entity {
    @Override
    void update();

    float getElevation();

    @Override
    void draw(SGL gl);

    @Override
    UpdateFrequency getUpdateFrequency();

    @Override
    void onClick(int button);
}
