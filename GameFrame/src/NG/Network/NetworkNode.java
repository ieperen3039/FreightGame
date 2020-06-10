package NG.Network;

import NG.Tools.Logger;
import NG.Tools.Toolbox;
import NG.Tracks.TrackPiece;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Function;

public class NetworkNode {
    private final List<Direction> aDirection = new ArrayList<>(1);
    private final List<Direction> bDirection = new ArrayList<>(1);

    public NetworkNode() {
    }

    /**
     * @return the Direction of the given node in the direction lists of this node, or null if the given node is not
     * connected to this node.
     */
    public Direction getEntryOf(NetworkNode railNode) {
        for (Direction dir : aDirection) {
            if (dir.adjacent.equals(railNode)) {
                return dir;
            }
        }
        for (Direction dir : bDirection) {
            if (dir.adjacent.equals(railNode)) {
                return dir;
            }
        }
        return null;
    }

    /**
     * @return the Direction of the given track in the direction lists of this track, or null if the given track is not
     * connected to this node.
     */
    public Direction getEntryOf(TrackPiece currentTrack) {
        for (Direction dir : aDirection) {
            if (dir.trackPiece.equals(currentTrack)) {
                return dir;
            }
        }
        for (Direction dir : bDirection) {
            if (dir.trackPiece.equals(currentTrack)) {
                return dir;
            }
        }
        return null;
    }

    /**
     * @return the Direction of the given network node in the direction lists of this node, or null if the given network
     * node is not connected to this node.
     */
    public Direction getEntryOfNetwork(NetworkNode networkNode) {
        assert networkNode.isNetworkCritical();

        for (Direction dir : aDirection) {
            if (dir.network.equals(networkNode)) {
                return dir;
            }
        }
        for (Direction dir : bDirection) {
            if (dir.network.equals(networkNode)) {
                return dir;
            }
        }
        return null;
    }

    /**
     * @return an immutable view of all connections
     */
    public Collection<Direction> getAllEntries() {
        return Toolbox.combinedList(aDirection, bDirection);
    }

    /**
     * returns the nodes that follows from passing this node from the direction of the given previous node.
     * @param previous the node you just left
     * @return the logical next nodes on the track
     */
    public List<Direction> getNext(NetworkNode previous) {
        return getNext(previous, d -> d.adjacent);
    }

    /**
     * returns the nodes that follows from passing this node from the direction of the given leaving track.
     * @param track one track connected to this node
     * @return the logical next nodes on the track
     */
    public List<Direction> getNext(TrackPiece track) {
        return getNext(track, d -> d.trackPiece);
    }

    /**
     * given a connected network node, returns the direction list opposite to that node. Similar as
     * getNext(NetworkNode), but with network node instead
     * @param networkNode
     * @return the list of directions, or null if networkNode is not a connected network node
     */
    public List<Direction> getNextFromNetwork(NetworkNode networkNode) {
        return getNext(networkNode, d -> d.network);
    }

    public <T> List<Direction> getNext(T networkNode, Function<Direction, T> mapping) {
        if (getIndexOf(aDirection, networkNode, mapping) >= 0) {
            return bDirection;

        } else if (getIndexOf(bDirection, networkNode, mapping) >= 0) {
            return aDirection;
        }

        return null;
    }

    /** @return true iff this node has no connections on one (or both) side. */
    public boolean isEnd() {
        return aDirection.isEmpty() || bDirection.isEmpty();
    }

    /** @return true iff this node connects multiple tracks on at least one side. */
    public boolean isSwitch() {
        return aDirection.size() > 1 || bDirection.size() > 1;
    }

    /** @return true iff this node has exactly one connection on both sides. isStraight <=> !isEnd && !isSwitch */
    public boolean isStraight() {
        return aDirection.size() == 1 && bDirection.size() == 1;
    }

    /** @return true iff this node forms a connection in the network */
    public boolean isNetworkCritical() {
        return isSwitch() || isEnd();
    }

    /**
     * set the network node indicator in the direction of (this to source) to the newNetworkNode.
     * @param source           the node in whose direction the new networknode is set.
     * @param newNetworkNode   the first network node in direction of source.
     * @param distanceToSource distance between source and newNetworkNode
     */
    public void updateNetworkTo(NetworkNode source, NetworkNode newNetworkNode, float distanceToSource) {
        assert newNetworkNode == null || newNetworkNode.isNetworkCritical() : newNetworkNode + " | " + source;

        List<Direction> list = aDirection;
        List<Direction> otherList = bDirection;
        int i = getIndexOf(list, source);
        if (i == -1) {
            list = bDirection;
            otherList = aDirection;
            i = getIndexOf(list, source);
        }

        assert i != -1 : "source node " + source + " is not connected to this " + this;

        Direction entry = list.get(i);
        // we propagate backwards, hence distance increases
        float newDistance = distanceToSource + entry.trackPiece.getLength();
        assert !Float.isNaN(newDistance) : distanceToSource + " + " + entry.trackPiece;

        // if this is already set correctly, then everything down the line is set correctly as well
        if (newNetworkNode == null) {
            if (entry.network == null) return;

        } else {
            if (newNetworkNode.equals(entry.network) && newDistance == entry.distanceToNetworkNode) return;
        }

        entry.network = newNetworkNode;
        entry.distanceToNetworkNode = newDistance;

        // unless this is a network node itself, propagate the change
        if (!this.isNetworkCritical() && !this.isEnd()) {
            assert otherList.size() == 1;
            NetworkNode next = otherList.get(0).adjacent;
            next.updateNetworkTo(this, newNetworkNode, newDistance);
        }
    }

    public List<Direction> getEntriesA() {
        return Collections.unmodifiableList(aDirection);
    }

    public List<Direction> getEntriesB() {
        return Collections.unmodifiableList(bDirection);
    }

    private static int getIndexOf(List<Direction> list, NetworkNode target) {
        return getIndexOf(list, target, d -> d.adjacent);
    }

    /**
     * removes a connection between this node and the given target node
     * @param target a node this is connected to
     * @return the track connecting the two nodes iff the target was indeed connected to this node, and has now been
     * removed. If there is no such connection, this returns null
     */
    public TrackPiece removeNode(NetworkNode target) {
        Direction removed;

        List<Direction> thisToTarget = aDirection;
        int i = getIndexOf(thisToTarget, target);
        if (i != -1) {
            removed = thisToTarget.remove(i);

        } else {
            thisToTarget = bDirection;
            i = getIndexOf(thisToTarget, target);
            if (i != -1) {
                removed = thisToTarget.remove(i);

            } else {
                return null;
            }
        }

        return removed.trackPiece;
    }

    private void postRemoveCheck(boolean wasCritical, List<Direction> thisToOther) {
        if (isEnd() && !this.isNetworkCritical()) {
            for (Direction entry : thisToOther) {
                entry.adjacent.updateNetworkTo(this, null, 0);
            }

        } else if (wasCritical && !this.isNetworkCritical()) {
            assert isStraight() : this; // !isEnd() && !isSwitch() assuming (!isNetworkCritical() => !isSwitch())
            // doesnt matter which is which
            assert aDirection.size() == 1 : aDirection;
            assert bDirection.size() == 1 : bDirection;

            Direction oneDirection = aDirection.get(0);
            Direction twoDirection = bDirection.get(0);

            oneDirection.adjacent.updateNetworkTo(this, twoDirection.network, twoDirection.distanceToNetworkNode);
            twoDirection.adjacent.updateNetworkTo(this, oneDirection.network, oneDirection.distanceToNetworkNode);

        } else if (this.isNetworkCritical()) {
            for (Direction entry : aDirection) {
                entry.adjacent.updateNetworkTo(this, this, 0);
            }
            for (Direction entry : bDirection) {
                entry.adjacent.updateNetworkTo(this, this, 0);
            }
        }
    }

    @Override
    public String toString() {
        return "NetworkNode (" + aDirection.size() + ":" + bDirection.size() + ')';
    }

    /** returns the index of the element containing the given node, or -1 if no such element exists */
    public static <T> int getIndexOf(List<Direction> aDirection, T target, Function<Direction, T> mapping) {
        assert target != null && mapping != null;

        for (int i = 0; i < aDirection.size(); i++) {
            Direction d = aDirection.get(i);
            T element = mapping.apply(d);
            if (target.equals(element)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * connects the two end nodes together
     */
    public static void addConnection(TrackPiece track) {
        RailNode oneRailNode = track.getStartNode();
        RailNode twoRailNode = track.getEndNode();
        NetworkNode oneNode = oneRailNode.getNetworkNode();
        NetworkNode twoNode = twoRailNode.getNetworkNode();

        check(oneNode);
        check(twoNode);

        List<Direction> oneToTwo;
        List<Direction> twoToOne;

        Vector3f trackStartDirection = track.getDirectionFromFraction(0);
        if (trackStartDirection.dot(oneRailNode.getDirection()) > 0) {
            oneToTwo = oneNode.aDirection;
        } else {
            oneToTwo = oneNode.bDirection;
        }

        Vector3f trackEndDirection = track.getDirectionFromFraction(1);
        if (trackEndDirection.dot(twoRailNode.getDirection()) < 0) { // other direction
            twoToOne = twoNode.aDirection;
        } else {
            twoToOne = twoNode.bDirection;
        }

        oneToTwo.add(new Direction(twoNode, track, null, 0));
        twoToOne.add(new Direction(oneNode, track, null, 0));

        // these must occur after adding
        updateNetwork(oneNode, twoNode);
        updateNetwork(twoNode, oneNode);

        check(oneNode);
        check(twoNode);
    }

    private static void updateNetwork(NetworkNode thisNode, NetworkNode targetNode) {
        if (thisNode.isNetworkCritical()) {
            // one of these is targetNode
            for (Direction entry : thisNode.aDirection) {
                entry.adjacent.updateNetworkTo(thisNode, thisNode, 0);
            }
            for (Direction entry : thisNode.bDirection) {
                entry.adjacent.updateNetworkTo(thisNode, thisNode, 0);
            }

        } else if (thisNode.isStraight()) {
            List<Direction> otherDirections = thisNode.getNext(targetNode);
            assert otherDirections.size() == 1;
            Direction entryOfOther = otherDirections.get(0);

            NetworkNode network = entryOfOther.network;
            if (network == null || !network.isNetworkCritical()) {
                // edge case of a loop
                Logger.DEBUG.print("Loop detected");
                targetNode.updateNetworkTo(thisNode, null, 0);
                entryOfOther.adjacent.updateNetworkTo(thisNode, null, 0);
            } else {

                targetNode.updateNetworkTo(thisNode, network, entryOfOther.distanceToNetworkNode);
            }
        }
    }

    /**
     * removes the connection between the nodes, and returns the track piece to be removed
     * @param aNode
     * @param bNode
     * @return an unmodified track piece. This should be disposed, but could potentially be re-inserted with {@link
     * #addConnection(TrackPiece)}
     */
    public static TrackPiece removeConnection(NetworkNode aNode, NetworkNode bNode) {
        assert aNode.getEntryOf(bNode) != null && bNode.getEntryOf(aNode) != null;

        boolean aWasCritical = aNode.isNetworkCritical();
        boolean bWasCritical = bNode.isNetworkCritical();
        List<Direction> aToOther = aNode.getNext(bNode);
        List<Direction> bToOther = bNode.getNext(aNode);

        TrackPiece oldPiece = aNode.removeNode(bNode);
        TrackPiece sameOldPiece = bNode.removeNode(aNode);

        aNode.postRemoveCheck(aWasCritical, aToOther);
        bNode.postRemoveCheck(bWasCritical, bToOther);

        check(aNode);
        check(bNode);

        assert oldPiece == sameOldPiece :
                "Nodes were mutually connected with a different track piece (" + oldPiece + " and " + sameOldPiece + ")";
        assert oldPiece != null : "Invalid tracks between " + aNode + " and " + bNode;

        return oldPiece;
    }

    public static void check(NetworkNode aNode) {
        assert aNode.isNetworkCritical() || !aNode.isSwitch() : aNode;

        Collection<Direction> entries = aNode.getAllEntries();
        // all network nodes are network critical
        assert entries.stream()
                .map(e -> e.network)
                .filter(Objects::nonNull)
                .allMatch(NetworkNode::isNetworkCritical)
                : entries;
        // if this is a network node, all distances are reflective
        assert !aNode.isNetworkCritical() || entries.stream()
                .filter(e -> e.network != null)
                .allMatch(a -> a.network.getAllEntries().stream()
                        .filter(b -> b.network == aNode)
                        .anyMatch(b -> Math.abs(a.distanceToNetworkNode - b.distanceToNetworkNode) < 0.001)
                ) : entries;
    }

    /**
     * Replaces the connection between oneNode and twoNode with a connection via newNode. inserts the given newNode
     * between the two given connected nodes, using the two given tracks. newNode should be not connected to anything.
     * The direction of the tracks does not matter.
     * @param oneNode  a node connected to twoNode
     * @param twoNode  a node connected to oneNode
     * @param newNode  a new, unconnected node
     * @param oneTrack the new track between oneNode and newNode
     * @param twoTrack the new track between newNode and twoNode
     */
    public static void insertNode(
            NetworkNode oneNode, NetworkNode twoNode, NetworkNode newNode, TrackPiece oneTrack, TrackPiece twoTrack
    ) {
        assert newNode.aDirection.isEmpty() && newNode.bDirection.isEmpty() : "newNode should be empty | " + newNode;

        check(oneNode);
        check(twoNode);

        replaceEntry(oneNode, twoNode, newNode, oneTrack);
        replaceEntry(twoNode, oneNode, newNode, twoTrack);

        check(oneNode);
        check(twoNode);
        check(newNode);
    }

    public static void replaceEntry(
            NetworkNode oneNode, NetworkNode twoNode, NetworkNode newNode, TrackPiece track
    ) {
        int twoIndex;
        List<Direction> oneList = oneNode.aDirection;
        List<Direction> otherList = oneNode.bDirection;

        twoIndex = getIndexOf(oneList, twoNode);
        if (twoIndex == -1) {
            oneList = oneNode.bDirection;
            otherList = oneNode.aDirection;
            twoIndex = getIndexOf(oneList, twoNode);

            assert twoIndex != -1 : "nodes are not connected";
        }

        Direction entryOneToTwo = oneList.get(twoIndex);
        oneList.set(twoIndex, new Direction(newNode, track, entryOneToTwo.network, entryOneToTwo.distanceToNetworkNode));

        NetworkNode networkNode;
        float distanceToNetworkOfNewToOne;

        if (oneNode.isNetworkCritical()) {
            networkNode = oneNode;
            distanceToNetworkOfNewToOne = track.getLength();

        } else if (oneNode.isEnd()) {
            networkNode = null;
            distanceToNetworkOfNewToOne = 0;

        } else {
            assert oneNode.isStraight() : oneNode;  // !isEnd() && !isSwitch() assuming (!isNetworkCritical() => !isSwitch())
            Direction direction = otherList.get(0);
            networkNode = direction.network;
            distanceToNetworkOfNewToOne = direction.distanceToNetworkNode + track.getLength();
        }

        Direction newEntry = new Direction(oneNode, track, networkNode, distanceToNetworkOfNewToOne);

        RailNode node = track.getStartNode();
        boolean trackStartsWithNew = node.getNetworkNode().equals(newNode);
        List<Direction> list;

        if (trackStartsWithNew) {
            Vector3f targetDirection = track.getDirectionFromFraction(0f);
            if (node.getDirection().dot(targetDirection) > 0) {
                list = newNode.aDirection;
            } else {
                list = newNode.bDirection;
            }

        } else {
            node = track.getEndNode();
            Vector3f targetDirection = track.getDirectionFromFraction(1f);
            if (node.getDirection().dot(targetDirection) < 0) {
                list = newNode.aDirection;
            } else {
                list = newNode.bDirection;
            }
        }

        list.add(newEntry);
    }

    public Set<NetworkNode> getAllNodes(NetworkNode startNode) {
        Set<NetworkNode> seen = new HashSet<>();
        Deque<NetworkNode> open = new ArrayDeque<NetworkNode>();
        open.add(startNode);
        seen.add(startNode);

        while (!open.isEmpty()) {
            NetworkNode current = open.remove();

            for (Direction entry : current.getAllEntries()) {
                NetworkNode other = entry.adjacent;

                if (other != null && !seen.contains(other)) {
                    open.add(other);
                    seen.add(other);

                }
            }
        }
        return seen;
    }

    /**
     * translates the top-level pathing network to a string.
     * @param startNode A starting node on the network, which will be id 0.
     * @return a comma separated list of pairs "a-b" with numbers a and b representing arbitrary ID values of nodes.
     * pairs may occur the other way around. dead ends are marked by 'x'
     */
    public static String getNetworkAsString(NetworkNode startNode) {
        List<Edge> edges = getNetworkEdges(startNode);

        if (edges.isEmpty()) return "";

        // +1 for 1-indexing
        StringBuilder output = new StringBuilder("matrix(c(");
        edges.forEach(e -> output.append(e.thisID + 1).append(", ")
                .append(e.otherID + 1).append(", ")
                .append(e.distance).append(", ")
        );

        output.setLength(output.length() - 2);
        output.append("), nc = 3, byrow = TRUE").append(")");
        return output.toString();
    }

    private static List<Edge> getNetworkEdges(NetworkNode startNode) {
        List<Edge> edges = new ArrayList<Edge>();
        Map<NetworkNode, Integer> seen = new HashMap<NetworkNode, Integer>();
        Deque<NetworkNode> open = new ArrayDeque<NetworkNode>();
        open.add(startNode);
        seen.put(startNode, 0);

        while (!open.isEmpty()) {
            NetworkNode current = open.remove();
            Integer currentID = seen.get(current);

            for (Direction entry : current.getAllEntries()) {
                NetworkNode other = entry.network;

                if (other != null) {
                    if (!seen.containsKey(other)) {
                        int id = seen.size();
                        seen.put(other, id);
                        open.add(other);
                    }

                    Integer otherID = seen.get(other);
                    edges.add(new Edge(current, currentID, other, otherID, entry.distanceToNetworkNode));
                }
                // else this is an endpoint
            }
        }
        return edges;
    }

    private static class Edge {
        final NetworkNode thisNode;
        final int thisID;
        final NetworkNode otherNode;
        final int otherID;
        final float distance;

        public Edge(NetworkNode thisNode, int thisID, NetworkNode otherNode, int otherID, float distance) {
            this.thisNode = thisNode;
            this.thisID = thisID;
            this.otherNode = otherNode;
            this.otherID = otherID;
            this.distance = distance;
        }
    }

    public static class Direction {
        public final NetworkNode adjacent;
        public final TrackPiece trackPiece;
        public NetworkNode network;

        /** distance between networkNode and the owner of this Direction */
        public float distanceToNetworkNode;

        private Direction(
                NetworkNode adjacent, TrackPiece trackPiece, NetworkNode network, float distanceToNetworkNode
        ) {
            assert !Float.isNaN(distanceToNetworkNode);
            this.adjacent = adjacent;
            this.trackPiece = trackPiece;
            this.network = network;
            this.distanceToNetworkNode = distanceToNetworkNode;
        }

        @Override
        public String toString() {
            return network == null ?
                    "{" + trackPiece.getClass().getSimpleName() + ", " + adjacent + ", - }" :
                    "{" + trackPiece.getClass().getSimpleName() + ", " + adjacent + ", " + distanceToNetworkNode + "}";
        }
    }
}
