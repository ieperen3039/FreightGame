package NG.Freight;

import NG.DataStructures.CargoCollection;
import NG.DataStructures.Valuta;
import NG.Entities.Storage;
import NG.Mods.CargoType;

/**
 * Known as 'Goods' or 'Haul' and represents whatever is put on a train. One instance represents a certain quantity of
 * one type. Instances of these are created by stations, while industries use their own implementation of quantity of
 * goods.
 * @author Geert van Ieperen. Created on 12-11-2018.
 * @see CargoCollection
 * @see CargoType
 */
public class Cargo {
    public final CargoType type;

    private final double pickupTime;
    private final Storage pickupPlace;
    private int quantity;

    public Cargo(CargoType type, int quantity, double pickupTime, Storage pickupPlace) {
        assert quantity > 0;
        this.type = type;
        this.quantity = quantity;
        this.pickupTime = pickupTime;
        this.pickupPlace = pickupPlace;
    }

    /** return a new Freight class with the given quantity subtracted from this, or null when impossible */
    public Cargo split(int quantity) {
        assert quantity > 0;
        if (this.quantity <= quantity) return null;

        this.quantity -= quantity;
        return new Cargo(type, quantity, pickupTime, pickupPlace);
    }

    public int quantity() {
        return quantity;
    }

    @Override
    public String toString() {
        return "Cargo{" + type + ":" + quantity + '}';
    }

    public Valuta value(double time, Storage target) {
        double secondsInTransit = time - pickupTime;
        float distanceTravelled = pickupPlace.getPosition().distance(target.getPosition());
        return type.value(secondsInTransit, distanceTravelled).multiply(quantity);
    }
}
