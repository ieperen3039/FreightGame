package NG.Network;

import NG.DataStructures.Generic.Pair;
import NG.Tracks.TrackPiece;

import java.util.List;

/**
 * @author Geert van Ieperen created on 22-5-2020.
 */
public interface NetworkPosition {
    /**
     * @return a list of (node, inDirection) pairs, where node is a node belonging to this NetworkPosition, and
     * inDirection is true iff this this node accepts traffic coming from the bNodes side of node.
     */
    List<Pair<NetworkNode, Boolean>> getNodes();

    /**
     * @return a list of all track pieces that are part of this NetworkPosition
     */
    List<TrackPiece> getTracks();

    /**
     * @param arrivalTrack the track adjacent to node, indicating direction (track -> node)
     * @param node         the node to find
     * @return true iff node is part of the nodes of this position, and the direction is correct.
     */
    default boolean containsNode(TrackPiece arrivalTrack, NetworkNode node) {
        List<Pair<NetworkNode, Boolean>> targetNodes = getNodes();

        for (Pair<NetworkNode, Boolean> target : targetNodes) {
            if (target.left.equals(node)) {
                if (target.left.isInDirectionOf(arrivalTrack) == target.right) {
                    return true;
                }
            }
        }

        return false;
    }
}
