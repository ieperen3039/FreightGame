package NG.Entities;

import NG.Core.Game;
import NG.DataStructures.CargoCollection;
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
    private transient Properties properties;
    private final String typeName;
    private Collection<Cargo> contents;
    private transient CargoType currentType = CargoType.NO_CARGO;

    public Wagon(Properties properties) {
        this.properties = properties;
        this.typeName = properties.name;
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

    @Override
    public CargoType getCurrentCargoType() {
        if (contents.isEmpty()) return CargoType.NO_CARGO;
        return currentType;
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
        return new Pair<>(currentType, getCargoAmount());
    }

    @Override
    public Collection<Cargo> getContentElements() {
        return contents;
    }

    @Override
    public Collection<Cargo> take(int amount) {
        return CargoCollection.remove(currentType, amount, contents);
    }

    @Override
    public Collection<Cargo> takeAll() {
        Collection<Cargo> returnValue = new ArrayList<>(contents);
        contents.clear();
        return returnValue;
    }

    @Override
    public void addContents(Cargo cargo) throws IllegalArgumentException {
        if (!properties.capacity.containsKey(cargo.getType())) {
            throw new IllegalArgumentException("Tried adding cargo of wrong type");
        }

        if (!contents.isEmpty() && !cargo.getType().equals(currentType)) {
            throw new IllegalArgumentException("Tried mixing cargo types");
        }

        int maximum = properties.capacity.get(cargo.getType());
        if (getCargoAmount() + cargo.quantity() > maximum) {
            throw new IllegalArgumentException("Tried adding more than capacity allows");
        }

        currentType = cargo.getType();
        contents.add(cargo);
    }

    @Override
    public double getLoadTime(Cargo cargo) {
        int maximum = properties.capacity.get(cargo.getType());
        return ((double) cargo.quantity() / maximum) * properties.loadingTime;
    }

    @Override
    public void restore(Game game) {
        properties = game.objectTypes().getWagonByName(typeName);

        for (Cargo cargo : contents) {
            cargo.restore(game);
            currentType = cargo.getType();
        }
    }

    public static class Properties extends TrainElement.Properties {
        public final Map<CargoType, Integer> capacity;
        public final double loadingTime;

        public Properties(
                String name, float length, float mass, float linearResistance,
                float maxSpeed, Resource<Mesh> mesh, List<String> trackTypes, int buildCost, float maintenance,
                Map<CargoType, Integer> capacity, double loadingTime
        ) {
            super(name, length, mass, linearResistance, 0, maxSpeed, mesh, trackTypes, buildCost, maintenance);
            this.capacity = Collections.unmodifiableMap(capacity);
            this.loadingTime = loadingTime;
        }
    }
}
