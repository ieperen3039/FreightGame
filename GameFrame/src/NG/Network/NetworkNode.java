package NG.Network;

import NG.Tools.Logger;

import java.util.*;

/**
 * @author Geert van Ieperen created on 1-5-2020.
 */
public class NetworkNode {
    private final RailNode aNode;
    private final RailNode bNode;

    private List<NetworkNode> aReachable;
    private List<NetworkNode> bReachable;
    private final List<RailNode.Direction> aDirections;
    private final List<RailNode.Direction> bDirections;

    /**
     * create a new node using aNode and bNode as end points. For aDirection and bDirection holds, that aNode contains
     * aDirection as one of its entries and bNode contains bDirection as one of its entries.
     * @param aNode      one node
     * @param aDirection a direction form aNode that can be followed to bNode
     * @param bNode      another node
     * @param bDirection a direction from bNode that can be followed to aNode
     */
    private NetworkNode(RailNode aNode, RailNode.Direction aDirection, RailNode bNode, RailNode.Direction bDirection) {
        this.aNode = aNode;
        this.bNode = bNode;

        aDirections = aNode.getNext(aDirection.railNode);
        aReachable = wrapReachable(aDirections);

        bDirections = bNode.getNext(bDirection.railNode);
        bReachable = wrapReachable(bDirections);

        Logger.DEBUG.print("Network:", networkPairs(this));
    }

    private static List<NetworkNode> wrapReachable(List<RailNode.Direction> list) {
        return new AbstractList<NetworkNode>() {
            final List<RailNode.Direction> next = list;

            @Override
            public NetworkNode get(int index) {
                return next.get(index).networkNode;
            }

            @Override
            public int size() {
                return next.size();
            }
        };
    }

    public static NetworkNode createNetworkNode(
            RailNode aNode, RailNode.Direction aDirection, RailNode bNode, RailNode.Direction bDirection
    ) {
        NetworkNode newNode = new NetworkNode(aNode, aDirection, bNode, bDirection);
        aDirection.networkNode = newNode;
        bDirection.networkNode = newNode;
        return newNode;
    }

    public static List<NetworkNode> networkList(NetworkNode start) {
        ArrayList<NetworkNode> list = new ArrayList<>();
        Set<NetworkNode> seen = new HashSet<>();
        Deque<NetworkNode> queue = new ArrayDeque<>();

        list.add(start);
        seen.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            NetworkNode node = queue.remove();
            for (NetworkNode other : node.aReachable) {
                if (!seen.add(other)) {
                    queue.add(other);
                    list.add(other);
                }
            }
            for (NetworkNode other : node.bReachable) {
                if (!seen.add(other)) {
                    queue.add(other);
                    list.add(other);
                }
            }
        }

        return list;
    }

    public static String networkPairs(NetworkNode start) {
        StringBuilder stringer = new StringBuilder();
        List<NetworkNode> queue = new ArrayList<>();

        queue.add(start);

        int i = 0;
        while (i < queue.size()) {
            NetworkNode node = queue.get(i);

            for (NetworkNode other : node.aReachable) {
                int oi = queue.indexOf(other);
                if (oi == -1) {
                    queue.add(other);
                } else if (oi < i) {
                    stringer.append(i).append('-').append(oi);
                }
            }
            for (NetworkNode other : node.bReachable) {
                int oi = queue.indexOf(other);
                if (oi == -1) {
                    queue.add(other);
                } else if (oi < i) {
                    stringer.append(i).append('-').append(oi);
                }
            }

            i++;
        }

        return stringer.toString();
    }
}
