package NG.DataStructures;

import NG.Freight.Cargo;
import NG.Mods.CargoType;

import java.util.*;

/**
 * A collection of Freight entities.
 * @author Geert van Ieperen created on 21-1-2019.
 */
public class CargoCollection extends AbstractCollection<Cargo> {
    private final Collection<Cargo> storage;

    public CargoCollection() {
        this.storage = new ArrayList<>();
    }

    @Override
    public Iterator<Cargo> iterator() {
        return storage.iterator();
    }

    @Override
    public int size() {
        int sum = 0;
        for (Cargo cargo : storage) {
            int quantity = cargo.quantity();
            sum += quantity;
        }
        return sum;
    }

    public Map<CargoType, Integer> asMap() {
        Map<CargoType, Integer> contents = new HashMap<>();
        for (Cargo cargo : storage) {
            contents.merge(cargo.type, cargo.quantity(), Integer::sum);
        }
        return contents;
    }

    public boolean add(Cargo newGood) {
        assert newGood.quantity() > 0 : newGood;
        return storage.add(newGood);
    }

    /**
     * take goods from this storage
     * @param type   the desired goods
     * @param amount the desired total quantity of goods
     * @return a collection of the required goods, with a summed quantity either equal to amount, or all goods of the
     * desired type available in the storage.
     */
    public Collection<Cargo> take(CargoType type, int amount) {
        // TODO sort on ???
        Collection<Cargo> batch = new ArrayList<>();
        int remainder = amount;

        for (Cargo cargo : storage) {
            if (cargo.type == type) {
                int elementQuantity = cargo.quantity();
                if (elementQuantity > remainder) {
                    Cargo split = cargo.split(remainder);
                    batch.add(split);
                    break;

                } else if (elementQuantity == remainder) {
                    batch.add(cargo);
                    break;

                } else {
                    remainder -= elementQuantity;
                    batch.add(cargo);
                }
            }
        }

        storage.removeAll(batch);

        return batch;
    }

    @Override
    public String toString() {
        Map<String, Integer> contents = new HashMap<>();
        for (Cargo cargo : storage) {
            String typeName = cargo.type.name();

            if (contents.containsKey(typeName)) {
                contents.put(typeName, contents.get(typeName) + cargo.quantity());
            } else {
                contents.put(typeName, cargo.quantity());
            }
        }
        return "FreightStorage" + contents;
    }
}
