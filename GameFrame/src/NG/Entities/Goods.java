package NG.Entities;

import NG.Engine.GameTimer;

/**
 * @author Geert van Ieperen. Created on 12-11-2018.
 */
public class Goods {
    private final GoodsType type;
    private final int pickupTime;
    private final Entity pickupPlace;
    private int quantity;

    public Goods(GoodsType type, int quantity, int pickupTime, Entity pickupPlace) {
        this.type = type;
        this.quantity = quantity;
        this.pickupTime = pickupTime;
        this.pickupPlace = pickupPlace;
    }

    public Goods split(int quantity) {
        assert this.quantity > quantity;
        this.quantity -= quantity;
        return new Goods(type, quantity, pickupTime, pickupPlace);
    }

    public float getCurrentPrice(GameTimer time) {
        return type.valueOverTime(pickupTime - time.getGametime());
    }
}
