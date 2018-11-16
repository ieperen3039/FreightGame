package NG.Entities;

import NG.DataStructures.MatrixStack.SGL;

/**
 * @author Geert van Ieperen. Created on 14-9-2018.
 */
public interface Entity {
    void update();

    void draw(SGL gl);

    void onClick(int button);
}
