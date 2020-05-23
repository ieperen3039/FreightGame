package NG.Network;

import NG.Tracks.TrackType;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 2-5-2020.
 */
public class SpecialRailNode extends RailNode {
    private final NetworkPosition source;

    public SpecialRailNode(Vector3fc nodePoint, TrackType type, Vector3fc direction, NetworkPosition source) {
        super(nodePoint, type, direction);
        this.source = source;
    }

    public SpecialRailNode(RailNode target) {
        super(target);
        source = null;
    }

    @Override
    public boolean isNetworkNode() {
        // this is the point of being special
        return true;
    }

    @Override
    public String toString() {
        return "SpecialNode{" + source + '}';
    }
}
