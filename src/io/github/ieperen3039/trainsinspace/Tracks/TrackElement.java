package io.github.ieperen3039.trainsinspace.Tracks;

import io.github.ieperen3039.ngn.DataStructures.Generic.Color4f;
import io.github.ieperen3039.trainsinspace.Core.Coloring;
import io.github.ieperen3039.trainsinspace.DataStructures.Collision.ColliderEntity;
import io.github.ieperen3039.trainsinspace.Entities.GameEntity;

/**
 * Any entity that is connected to a specific type of track. This excludes entities that accept arbitrary track types.
 *
 * @author Geert van Ieperen created on 26-4-2021.
 */
public abstract class TrackElement extends GameEntity implements ColliderEntity {
    protected final String typeName;
    protected final Coloring coloring = new Coloring(Color4f.WHITE);
    protected transient TrackType type;

    public TrackElement(TrackType type) {
        this.type = type;
        this.typeName = type.toString();
    }

    @Override
    public GameEntity.UpdateFrequency getUpdateFrequency() {
        return GameEntity.UpdateFrequency.NEVER;
    }

    public TrackType getType() {
        return type;
    }

    public void setMarking(Coloring.Marking marking) {
        this.coloring.addMark(marking);
    }
}
