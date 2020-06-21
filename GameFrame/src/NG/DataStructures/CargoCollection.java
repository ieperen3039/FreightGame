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

    /**
     * @return a mapping of type to the amount of units available
     */
    public Map<CargoType, Integer> asMap() {
        Map<CargoType, Integer> contents = new HashMap<>();
        this.addToMap(contents);
        return contents;
    }

    /**
     * adds the amounts of each cargo type of this collection to the types in the given map, creating new entries if
     * necessary.
     * @param contents the contents of this collection are added to this map
     */
    public void addToMap(Map<CargoType, Integer> contents) {
        for (Cargo cargo : storage) {
            contents.merge(cargo.type, cargo.quantity(), Integer::sum);
        }
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
        return remove(type, amount, storage);
    }

    /**
     * @param type    the type of cargo to remove
     * @param amount  the total amount to remove
     * @param storage where to take the cargo from
     * @return a collection of cargo elements of the given type whose quantities together adds up to amount
     * @throws IllegalArgumentException if amount > size() : when this happens, the state of storage is unchanged
     */
    public static Collection<Cargo> remove(CargoType type, int amount, Collection<Cargo> storage) {
        // TODO sort on ???
        Collection<Cargo> batch = new ArrayList<>();
        int remainder = amount;

        for (Cargo cargo : storage) {
            if (cargo.type == type) {
                int elementQuantity = cargo.quantity();
                if (elementQuantity > remainder) {
                    Cargo split = cargo.split(remainder);
                    // the original cargo stays in storage
                    batch.add(split);
                    remainder = 0;
                    break;

                } else if (elementQuantity == remainder) {
                    batch.add(cargo);
                    remainder = 0;
                    break;

                } else {
                    remainder -= elementQuantity;
                    batch.add(cargo);
                }
            }
        }

        if (remainder > 0) { // no cargo has been split
            throw new IllegalArgumentException("Could not meet requested amount: " + amount);
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
