package NG.Entities;

/**
 * @author Geert van Ieperen. Created on 12-11-2018.
 */
public class Goods {
    private final String name;
    private final int pickupTime;
    private final Entity pickupPlace;
    private int quantity;

    public Goods(String name, int quantity, int pickupTime, Entity pickupPlace) {
        this.name = name;
        this.quantity = quantity;
        this.pickupTime = pickupTime;
        this.pickupPlace = pickupPlace;
    }

    public Goods split(int quantity) {
        assert this.quantity > quantity;
        this.quantity -= quantity;
        return new Goods(name, quantity, pickupTime, pickupPlace);
    }

    public int getCurrentPrice() {
        return 0;
    }
}
