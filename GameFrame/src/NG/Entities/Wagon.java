package NG.Entities;

import NG.DataStructures.Generic.Pair;
import NG.Freight.Cargo;
import NG.Mods.CargoType;
import NG.Rendering.MeshLoading.Mesh;
import NG.Resources.Resource;

import java.util.*;

/**
 * @author Geert van Ieperen created on 20-5-2020.
 */
public class Wagon implements TrainElement {
    public final Properties properties;
    private CargoType currentType;
    private Collection<Cargo> contents;

    public Wagon(Properties properties) {
        this.properties = properties;
        this.contents = new ArrayList<>();
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public Map<CargoType, Integer> getCargoTypes() {
        return properties.capacity;
    }

    private int getCargoAmount() {
        int totalContents = 0;
        for (Cargo cargo : contents) {
            totalContents += cargo.quantity();
        }
        return totalContents;
    }

    @Override
    public Pair<CargoType, Integer> getContents() {
        int totalContents = getCargoAmount();
        return new Pair<>(currentType, totalContents);
    }

    @Override
    public double addContents(Cargo cargo) throws IllegalArgumentException {
        if (!properties.capacity.containsKey(cargo.type)) {
            throw new IllegalArgumentException("Tried adding cargo of wrong type");
        }

        if (!contents.isEmpty() && !cargo.type.equals(currentType)) {
            throw new IllegalArgumentException("Tried mixing cargo types");
        }

        int maximum = properties.capacity.get(cargo.type);
        if (getCargoAmount() + cargo.quantity() > maximum) {
            throw new IllegalArgumentException("Tried adding more than capacity allows");
        }

        currentType = cargo.type;
        contents.add(cargo);

        return ((double) cargo.quantity() / maximum) * properties.loadingTime;
    }

    public static class Properties extends TrainElement.Properties {
        public final Map<CargoType, Integer> capacity;
        public final double loadingTime;

        public Properties(
                String name, float length, float mass, float linearResistance,
                Resource<Mesh> mesh, List<String> trackTypes, float maxSpeed,
                Map<CargoType, Integer> capacity, double loadingTime
        ) {
            super(name, length, mass, linearResistance, 0, maxSpeed, mesh, trackTypes);
            this.capacity = Collections.unmodifiableMap(capacity);
            this.loadingTime = loadingTime;
        }
    }
}
