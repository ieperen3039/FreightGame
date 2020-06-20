package NG.Network;

import NG.DataStructures.Generic.Pair;
import NG.Tracks.TrackPiece;

import java.util.List;

/**
 * @author Geert van Ieperen created on 22-5-2020.
 */
public interface NetworkPosition {
    List<Pair<NetworkNode, Boolean>> getNodes();

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
