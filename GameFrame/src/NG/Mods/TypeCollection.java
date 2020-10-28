package NG.Mods;

import NG.Entities.Industry;
import NG.Entities.Locomotive;
import NG.Entities.Wagon;
import NG.Tracks.TrackType;

import java.util.ArrayList;
import java.util.List;

/**
 * a record of all types of track, freight, locomotives and wagons
 * @author Geert van Ieperen. Created on 22-11-2018.
 */
public class TypeCollection {
    public final List<TrackType> trackTypes = new ArrayList<>();
    public final List<CargoType> cargoTypes = new ArrayList<>();
    public final List<Locomotive.Properties> locomotiveTypes = new ArrayList<>();
    public final List<Wagon.Properties> wagonTypes = new ArrayList<>();
    public final List<Industry.Properties> industryTypes = new ArrayList<>();

    public CargoType getCargoByName(String name) {
        for (CargoType type : cargoTypes) {
            if (type.name().equals(name)) return type;
        }

        return null;
    }

    public Industry.Properties getIndustryByName(String name) {
        for (Industry.Properties type : industryTypes) {
            if (type.name.equals(name)) return type;
        }

        return null;
    }

    public Locomotive.Properties getLocoByName(String name) {
        for (Locomotive.Properties type : locomotiveTypes) {
            if (type.name.equals(name)) return type;
        }

        return null;
    }

    public Wagon.Properties getWagonByName(String name) {
        for (Wagon.Properties type : wagonTypes) {
            if (type.name.equals(name)) return type;
        }

        return null;
    }
}
