package NG.Network;

import NG.Tracks.TrackType;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 2-5-2020.
 */
public class SpecialRailNode extends RailNode {
    public SpecialRailNode(Vector3fc nodePoint, TrackType type, Vector3fc direction) {
        super(nodePoint, type, direction);
    }

    public SpecialRailNode(RailNode target) {
        super(target);
    }

    @Override
    protected boolean isNetworkNode() {
        // this is the point of being special
        return true;
    }
}
