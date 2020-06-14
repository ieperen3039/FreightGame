package NG.Mods;

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
    public final List<CargoType> freightsTypes = new ArrayList<>();
    public final List<Locomotive.Properties> locomotiveTypes = new ArrayList<>();
    public final List<Wagon.Properties> wagonTypes = new ArrayList<>();
}
