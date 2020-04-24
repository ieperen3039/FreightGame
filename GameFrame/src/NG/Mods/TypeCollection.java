package NG.Mods;

import NG.Tracks.TrackType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * a collection of all different types available.
 * @author Geert van Ieperen. Created on 22-11-2018.
 */
public class TypeCollection {
    private final List<TrackType> trackTypes = new ArrayList<>();
    private final List<FreightType> freightsTypes = new ArrayList<>();

    public void addTrackTypes(TrackType type) {
        trackTypes.add(type);
    }

    public List<TrackType> getTrackTypes() {
        return Collections.unmodifiableList(trackTypes);
    }

    public void addFreightTypes(FreightType type) {
        freightsTypes.add(type);
    }

    public List<FreightType> getFreightsTypes() {
        return Collections.unmodifiableList(freightsTypes);
    }
}
