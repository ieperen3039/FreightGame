package NG.GameState;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.DataStructures.FreightStorage;
import NG.Entities.Entity;
import NG.Freight.Freight;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A storage is any building that stores {@link Freight}. This includes stations, industries and possibly a rearranging
 * terrain, but not a train.
 * @author Geert van Ieperen. Created on 12-11-2018.
 */
public abstract class Storage extends AbstractGameObject implements Entity {
    private final FreightStorage contents;
    private final Vector3f position;

    protected double spawnTime;
    protected double despawnTime = Double.POSITIVE_INFINITY;

    public Storage(Vector3fc position, Game game) {
        super(game);
        this.position = new Vector3f(position);
        this.contents = new FreightStorage();
    }

    protected FreightStorage contents() {
        return contents;
    }

    public void setPosition(Vector3fc position) {
        this.position.set(position);
    }

    public Vector3fc getPosition() {
        return position;
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
