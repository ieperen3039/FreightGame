package NG.DataStructures.Collision;

import NG.Entities.Entity;
import org.joml.AABBf;

/**
 * @author Geert van Ieperen created on 24-6-2020.
 */
public interface StaticEntity extends Entity {
    AABBf getHitbox();
}
