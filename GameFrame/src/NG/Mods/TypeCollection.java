package NG.Mods;

import NG.Entities.Tracks.TrackMod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Geert van Ieperen. Created on 22-11-2018.
 */
public class TypeCollection {
    private List<TrackMod.TrackType> trackTypes = new ArrayList<>();

    public void addTrackTypes(TrackMod mod) {
        trackTypes.addAll(mod.getTypes());
    }

    public List<TrackMod.TrackType> getTrackTypes() {
        return Collections.unmodifiableList(trackTypes);
    }
}
