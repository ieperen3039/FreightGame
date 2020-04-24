package NG.Entities;

import NG.Core.GameTimer;
import NG.DataStructures.Valuta;
import NG.Mods.FreightType;

/**
 * Known as 'Goods' or 'Haul' and represents whatever is put on a train (can be humans). One instance represents a
 * certain quantity of one type. Instances of these are created by stations, while industries use their own
 * implementation of quantity of goods.
 * @author Geert van Ieperen. Created on 12-11-2018.
 * @see NG.DataStructures.FreightStorage
 * @see FreightType
 */
public class Freight {
    public final FreightType type;

    private final int pickupTime;
    private final Entity pickupPlace;
    private int quantity;

    public Freight(FreightType type, int quantity, int pickupTime, Entity pickupPlace) {
        this.type = type;
        this.quantity = quantity;
        this.pickupTime = pickupTime;
        this.pickupPlace = pickupPlace;
    }

    public Freight split(int quantity) {
        if (this.quantity < quantity) return null;
        this.quantity -= quantity;
        return new Freight(type, quantity, pickupTime, pickupPlace);
    }

    public Valuta getCurrentPrice(GameTimer time) {
        return type.valueOverTime(pickupTime - time.getGametime());
    }

    public int quantity() {
        return quantity;
    }
}
