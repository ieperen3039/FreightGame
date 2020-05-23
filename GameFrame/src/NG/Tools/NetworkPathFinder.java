package NG.Tools;

import NG.Network.NetworkPosition;
import NG.Network.RailNode;
import NG.Tracks.TrackPiece;

import java.util.*;
import java.util.concurrent.Callable;

/**
 * Implements Dijkstra to find the shortest path to any node adhering to the given target.
 * @author Geert van Ieperen created on 22-5-2020.
 */
public class NetworkPathFinder implements Callable<List<RailNode>> {
    private final Set<RailNode> targets;
    private final RailNode startPredecessor;
    private final RailNode startNode;

    public NetworkPathFinder(TrackPiece currentTrack, RailNode nextNode, NetworkPosition target) {
        assert nextNode.isNetworkNode();

        this.targets = target.getNodes();
        this.startNode = nextNode;
        this.startPredecessor = nextNode.getEntryOf(currentTrack).networkNode;
    }

    @Override
    public List<RailNode> call() {
        Map<RailNode, RailNode> predecessors = new HashMap<>();
        Map<RailNode, Float> distanceMap = new HashMap<>();
        PriorityQueue<RailNode> open = new PriorityQueue<>(Comparator.comparing(distanceMap::get));

        predecessors.put(startNode, startPredecessor);
        distanceMap.put(startNode, 0f);
        open.add(startNode);

        RailNode endNode = dijkstra(predecessors, distanceMap, open);

        if (endNode == null) {
            return null;
        }

        return new Path(startNode, predecessors, endNode);
    }

    /**
     * computes all nearest predecessors
     * @param predecessors A map that contains a predecessor of each node in open. Upon returning, maps each node to its
     *                     predecessor on the shortest path to that node.
     * @param distanceMap  A map that contains the distance to each node in open. Upon returning, contains the distance
     *                     of the shortest path to that node.
     * @param open         a collection of nodes to start searching from. Usually, contains only the starting node. Upon
     *                     returning, its contents is undefined.
     * @return the nearest node in {@code targets} found.
     */
    private RailNode dijkstra(
            Map<RailNode, RailNode> predecessors, Map<RailNode, Float> distanceMap, PriorityQueue<RailNode> open
    ) {
        while (!open.isEmpty()) {
            RailNode node = open.remove();
            if (targets.contains(node)) return node;

            RailNode predecessor = predecessors.get(node);
            assert predecessor != null : node + " has not been added to predecessors";

            List<RailNode.Direction> next = node.getNextFromNetwork(predecessor);
            assert next != null : "one-directional connection from " + predecessor + " to " + node;

            float nodeDistance = distanceMap.get(node);

            for (RailNode.Direction entry : next) {
                RailNode nextNode = entry.networkNode;
                if (nextNode == null) continue; // empty dead end
                if (nextNode == node) continue; // self-loop

                float distance = nodeDistance + entry.distanceToNetworkNode;
                Float knownDistance = distanceMap.getOrDefault(nextNode, Float.POSITIVE_INFINITY);
                if (knownDistance < distance) continue; // shorter path is known

                predecessors.put(nextNode, node);
                distanceMap.put(nextNode, distance);
                open.add(nextNode);
            }
        }

        return null;
    }

    private static class Path extends ArrayList<RailNode> {
        public Path(RailNode startNode, Map<RailNode, RailNode> predecessors, RailNode endNode) {
            super();

            RailNode currentNode = endNode;

            while (currentNode != startNode) {
                add(currentNode);
                currentNode = predecessors.get(currentNode);
                assert currentNode != null : predecessors;
            }

            int size = this.size();
            for (int i = 0, mid = size >> 1, j = size - 1; i < mid; i++, j--) {
                Collections.swap(this, i, j);
            }
        }
    }
}
