package NG.Entities;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.DataStructures.CargoCollection;
import NG.Freight.Cargo;
import NG.Mods.CargoType;
import NG.Tools.Logger;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Collection;

/**
 * A storage is any building that stores {@link Cargo}.
 * @author Geert van Ieperen. Created on 12-11-2018.
 */
public abstract class Storage extends AbstractGameObject implements Entity {
    private final CargoCollection contents;
    private final Vector3f position;

    private double spawnTime;
    private double despawnTime = Double.POSITIVE_INFINITY;

    public Storage(Game game, Vector3fc position, double spawnTime) {
        super(game);
        this.spawnTime = spawnTime;
        this.position = new Vector3f(position);
        this.contents = new CargoCollection();
    }

    public CargoCollection contents() {
        return getContents();
    }

    protected int loadThis(Train train, CargoType cargoType, int remainder) {
        try {
            CargoCollection contents = getContents();
            Collection<Cargo> cargos = contents.take(cargoType, Math.min(contents.size(), remainder));

            for (Cargo cargo : cargos) {
                remainder -= cargo.quantity();

                boolean complete = train.store(cargo);
                if (!complete) {
                    Logger.ASSERT.print("Storage#take(CargoType, int) returned too much cargo " + cargo);
                    remainder += cargo.quantity();
                    contents.add(cargo);
                }
            }

            return remainder;

        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Cant load " + remainder + " of " + cargoType, ex);
        }
    }

    public CargoCollection getContents() {
        return contents;
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
