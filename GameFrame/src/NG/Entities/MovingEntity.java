package NG.Entities;

import org.joml.Vector2f;

/**
 * @author Geert van Ieperen. Created on 14-9-2018.
 */
public interface MovingEntity extends Entity {
    Vector2f getPosition();

    @Override
    default UpdateFrequency getUpdateFrequency() {
        return UpdateFrequency.ALWAYS;
    }
}
