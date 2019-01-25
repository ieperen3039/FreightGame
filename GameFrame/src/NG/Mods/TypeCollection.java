package NG.Mods;

import NG.Tracks.TrackMod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * a collection of all different types available.
 * @author Geert van Ieperen. Created on 22-11-2018.
 */
public class TypeCollection {
    private List<TrackMod.TrackType> trackTypes = new ArrayList<>();
    private List<FreightMod.FreightType> freightsTypes = new ArrayList<>();

    public void addTrackTypes(TrackMod mod) {
        trackTypes.addAll(mod.getTypes());
    }

    public List<TrackMod.TrackType> getTrackTypes() {
        return Collections.unmodifiableList(trackTypes);
    }

    public void addFreightTypes(FreightMod mod) {
        freightsTypes.addAll(mod.getTypes());
    }

    public List<FreightMod.FreightType> getFreightsTypes() {
        return Collections.unmodifiableList(freightsTypes);
    }
}
