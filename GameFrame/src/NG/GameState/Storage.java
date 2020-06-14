package NG.GameState;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.DataStructures.CargoCollection;
import NG.Entities.Entity;
import NG.Entities.Train;
import NG.Freight.Cargo;
import NG.Mods.CargoType;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A storage is any building that stores {@link Cargo}.
 * @author Geert van Ieperen. Created on 12-11-2018.
 */
public abstract class Storage extends AbstractGameObject implements Entity {
    protected final CargoCollection contents;
    protected final Vector3f position;

    protected double spawnTime;
    protected double despawnTime = Double.POSITIVE_INFINITY;

    public Storage(Vector3fc position, Game game, double spawnTime) {
        super(game);
        this.spawnTime = spawnTime;
        this.position = new Vector3f(position);
        this.contents = new CargoCollection();
    }

    public CargoCollection contents() {
        return contents;
    }

    public void loadAvailable(Train train) {
        Map<CargoType, Integer> open = getTransferableCargo(this, train);

        while (!open.isEmpty()) {
            load(train, open);
            open = getTransferableCargo(this, train);
        }
    }

    public void load(Train train, Map<CargoType, Integer> open) {
        CargoType loadableType = open.keySet().iterator().next();
        // the total amount of these cargos may not add up to the total open space
        int amount = open.get(loadableType);
        assert amount > 0;
        Collection<Cargo> cargos = contents.take(loadableType, amount);

        for (Cargo cargo : cargos) {
            boolean complete = train.store(cargo);
            assert complete : "getTransferableCargo returned too much, leftover = " + cargo;
        }
    }

    public static Map<CargoType, Integer> getTransferableCargo(Storage storage, Train train) {
        Map<CargoType, Integer> available = storage.contents.asMap();
        Map<CargoType, Integer> freeSpace = train.getFreeSpace();
        Map<CargoType, Integer> intersection = new HashMap<>();

        for (CargoType type : freeSpace.keySet()) {
            if (available.containsKey(type)) {
                int amount = Math.min(freeSpace.get(type), available.get(type));
                if (amount > 0) {
                    intersection.put(type, amount);
                }
            }
        }

        return intersection;
    }

    protected void setPosition(Vector3fc position) {
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
