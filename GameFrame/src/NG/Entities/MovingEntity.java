package NG.Entities;

import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen. Created on 14-9-2018.
 */
public interface MovingEntity extends Entity {
    Vector3fc getPosition();

    @Override
    default UpdateFrequency getUpdateFrequency() {
        return UpdateFrequency.EVERY_TICK;
    }
}
