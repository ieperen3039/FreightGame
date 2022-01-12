package NG.Tracks;

import NG.Core.AbstractGameObject;
import NG.Core.Coloring;
import NG.Core.Game;
import NG.DataStructures.Collision.ColliderEntity;
import NG.DataStructures.Generic.Color4f;

/**
 * Any entity that is connected to a specific type of track. This excludes entities that accept arbitrary track types.
 * @author Geert van Ieperen created on 26-4-2021.
 */
public abstract class TrackElement extends AbstractGameObject implements ColliderEntity {
    protected final String typeName;
    protected final Coloring coloring = new Coloring(Color4f.WHITE);
    protected transient TrackType type;
    protected double spawnTime = Double.NEGATIVE_INFINITY;
    protected double despawnTime = Double.POSITIVE_INFINITY;

    public TrackElement(
            Game game, TrackType type
    ) {
        super(game);
        this.type = type;
        this.typeName = type.toString();
    }

    @Override
    public void restoreFields(Game game) {
        type = game.objectTypes().getTrackByName(typeName);
        assert type != null;
    }

    @Override
    public UpdateFrequency getUpdateFrequency() {
        return UpdateFrequency.NEVER;
    }

    public TrackType getType() {
        return type;
    }

    public void setMarking(Coloring.Marking marking) {
        this.coloring.addMark(marking);
    }

    @Override
    public void despawn(double gameTime) {
        despawnTime = gameTime;
    }

    @Override
    public double getSpawnTime() {
        return spawnTime;
    }

    @Override
    public double getDespawnTime() {
        return despawnTime;
    }
}
