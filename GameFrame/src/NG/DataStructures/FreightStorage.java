package NG.DataStructures;

import NG.Entities.Freight;
import NG.Entities.FreightType;
import NG.Tools.Logger;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A collection of Freight entities.
 * @author Geert van Ieperen created on 21-1-2019.
 */
public class FreightStorage {
    private Collection<Freight> storage;

    public FreightStorage() {
        // TODO find optimal datastructure and parameters
        this.storage = new ArrayList<>();
    }

    public boolean store(Freight newGood) {
        return storage.add(newGood);
    }

    /**
     * take goods from this storage
     * @param type   the desired goods
     * @param amount the desired total quantity of goods
     * @param newest currently ignored, should be false
     * @return a collection of the required goods, with a summed quantity either equal to amount, or all goods of the
     * desired type available in the storage.
     */
    public Collection<Freight> pull(FreightType type, int amount, boolean newest) {//TODO sort on newest freight first
        if (newest) Logger.ASSERT.print("pull(): parameter newest is ignored");
        Collection<Freight> batch = new ArrayList<>();
        int remainder = amount;

        for (Freight good : storage) {
            if (good.type == type) {
                int quantity = good.quantity();
                if (quantity > remainder) {
                    Freight split = good.split(remainder);
                    batch.add(split);
                } else {
                    remainder -= quantity;
                    batch.add(good);
                }
            }
        }

        storage.removeAll(batch);

        return batch;
    }
}
