package NG.Entities;

import NG.Core.Game;
import NG.DataStructures.Generic.Pair;
import NG.Freight.Cargo;
import NG.Mods.CargoType;
import NG.Rendering.MeshLoading.Mesh;
import NG.AssetHandling.Asset;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Geert van Ieperen created on 5-5-2020.
 */
public class Locomotive implements TrainElement {
    private transient Properties properties;
    private final String typeName;

    public Locomotive(Properties properties) {
        this.properties = properties;
        this.typeName = properties.name;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public Map<CargoType, Integer> getCargoTypes() {
        return Collections.emptyMap();
    }

    @Override
    public CargoType getCurrentCargoType() {
        return CargoType.NO_CARGO;
    }

    @Override
    public Pair<CargoType, Integer> getContents() {
        return new Pair<>(CargoType.NO_CARGO, 0);
    }

    @Override
    public Collection<Cargo> getContentElements() {
        return Collections.emptySet();
    }

    @Override
    public Collection<Cargo> take(int amount) {
        throw new IllegalArgumentException("This train doesn't have any cargo");
//        return Collections.emptyList();
    }

    @Override
    public void addContents(Cargo cargo) throws IllegalArgumentException {
        throw new IllegalArgumentException("This train doesn't accept any cargo");
    }

    @Override
    public int getStorableAmount(CargoType type) {
        return 0;
    }

    @Override
    public double getLoadTime(Cargo cargo) {
        return 0;
    }

    @Override
    public Collection<Cargo> takeAll() {
        return Collections.emptyList();
    }

    @Override
    public void restore(Game game) {
        properties = game.objectTypes().getLocoByName(typeName);
    }

    public static class Properties extends TrainElement.Properties {
        public final float tractiveEffort;

        public Properties(
                String name, float length, float mass, float linearResistance, float quadraticResistance,
                Asset<Mesh> mesh, List<String> trackTypes, int buildCost, float maintenancePerSec,
                float tractiveEffort
        ) {
            super(name, length, mass, linearResistance, quadraticResistance, 100, mesh, trackTypes, buildCost, maintenancePerSec);
            this.tractiveEffort = tractiveEffort;
        }
    }
}
