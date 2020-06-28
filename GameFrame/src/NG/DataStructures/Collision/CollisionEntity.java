package NG.DataStructures.Collision;

import org.joml.AABBf;

/**
 * @author Geert van Ieperen created on 21-2-2020.
 */
public class CollisionEntity {
    private final ColliderEntity entity;
    private int id;
    public AABBf hitbox; // combined of both states

    public CollisionEntity(ColliderEntity source) {
        this.entity = source;
        hitbox = entity.getHitbox();
    }

    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public ColliderEntity entity() {
        return entity;
    }

    public float xUpper() {
        return hitbox.maxX;
    }

    public float yUpper() {
        return hitbox.maxY;
    }

    public float zUpper() {
        return hitbox.maxZ;
    }

    public float xLower() {
        return hitbox.minX;
    }

    public float yLower() {
        return hitbox.minY;
    }

    public float zLower() {
        return hitbox.minZ;
    }

    @Override
    public String toString() {
        return entity.toString();
    }
}
