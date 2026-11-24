package io.github.ieperen3039.trainsinspace.Entities;

import io.github.ieperen3039.ngn.Scene.Entity;
import io.github.ieperen3039.trainsinspace.Core.Coloring;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An entity is anything that is both visible in the world, and allows interaction with other entities (including the
 * map). Particles and other visual things are not entities.
 * @author Geert van Ieperen. Created on 14-9-2018.
 */
public abstract class GameEntity implements Serializable, Entity {
    public enum UpdateFrequency {
        EVERY_TICK, ONCE_PER_SECOND, ONCE_UPON_A_TIME, NEVER
    }

    private static AtomicInteger nextEntityId = new AtomicInteger(1);
    public final int entityId;

    protected double despawnTime = Double.POSITIVE_INFINITY;
    protected double spawnTime = Double.NEGATIVE_INFINITY;

    public GameEntity() {
        entityId = nextEntityId.incrementAndGet();
    }

    public GameEntity(double spawnTime) {
        this();
        this.spawnTime = spawnTime;
    }

    public abstract void setMarking(Coloring.Marking mark);

    public double getDespawnTime() {
        return despawnTime;
    }
    /** marks this entity to be removed from the game at the given gameTime. */

    public void despawn(double gameTime) {
        despawnTime = gameTime;
    }

    public double getSpawnTime() {
        return spawnTime;
    }

    /**
     * @return true iff this unit should be removed from the game world.
     */
    public boolean isDespawnedAt(double gameTime) {
        return gameTime >= getDespawnTime();
    }

    /**
     * the {@link #update()} method of this entity will be called according to the given update frequency
     * @return a constant value indicating how often to update this entity
     */
    public UpdateFrequency getUpdateFrequency() {
        return UpdateFrequency.EVERY_TICK;
    }
}
