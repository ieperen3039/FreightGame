package NG.Tracks;

import NG.Entities.Entity;
import NG.Network.NetworkNode;
import NG.Network.RailNode;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen. Created on 18-9-2018.
 */
public interface TrackPiece extends Entity {

    @Override
    default UpdateFrequency getUpdateFrequency() {
        return UpdateFrequency.NEVER;
    }

    TrackType getType();

    /**
     * @return the node at the position of getPositionFromFraction(0)
     */
    RailNode getStartNode();

    /**
     * @return the node at the position of getPositionFromFraction(1)
     */
    RailNode getEndNode();

    default RailNode getNot(RailNode node) {
        RailNode startNode = getStartNode();
        return node == startNode ? getEndNode() : startNode;
    }

    float getFractionOfClosest(Vector3fc origin, Vector3fc direction);

    Vector3f getPositionFromFraction(float fraction);

    Vector3f getDirectionFromFraction(float fraction);

    float getLength();

    /**
     * some special rail pieces can't be split or removed by themself, for instance rails in stations.
     * @return false iff this track may not be removed or split by standard user-controlled rail builders.
     */
    boolean isStatic();

    default boolean isValid() {
        NetworkNode startNNode = getStartNode().getNetworkNode();
        NetworkNode endNNode = getEndNode().getNetworkNode();
        if (startNNode.getEntryOf(endNNode) == null || endNNode.getEntryOf(startNNode) == null) {
            throw new IllegalStateException(String.format("track %s has unconnected nodes %s and %s", this, startNNode, endNNode));
        }
        return true;
    }

    void setOccupied(boolean occupied);

    boolean isOccupied();

    float getMaximumSpeed();

    default RailNode get(NetworkNode targetNode) {
        if (getStartNode().getNetworkNode().equals(targetNode)) return getStartNode();
        if (getEndNode().getNetworkNode().equals(targetNode)) return getEndNode();
        return null;
    }
}
