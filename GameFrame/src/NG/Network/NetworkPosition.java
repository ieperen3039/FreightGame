package NG.Network;

import java.util.Set;

/**
 * @author Geert van Ieperen created on 22-5-2020.
 */
public interface NetworkPosition {
    Set<NetworkNode> getNodes();

    NetworkNode getStopNode(NetworkNode node);
}
