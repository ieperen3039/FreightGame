package NG.Network;

import NG.Tools.Logger;
import NG.Tools.Toolbox;
import NG.Tools.Vectors;
import NG.Tracks.TrackPiece;
import NG.Tracks.TrackType;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.*;

/**
 * A node that connects two track pieces together
 * @author Geert van Ieperen created on 16-12-2018.
 */
public class RailNode {
    /*
     * Representation Invariants
     * for all NetworkNodes n in aNodes {
     *      v := this.position to n.position
     *      (v dot this.direction) < 0
     * }
     * for all NetworkNodes n in bNodes {
     *      v := this.position to n.position
     *      (v dot this.direction) >= 0
     * }
     */

    private final List<Direction> aDirection = new ArrayList<>(1);
    private final List<Direction> bDirection = new ArrayList<>(1);
    private final Vector3fc direction;

    /** position of this node */
    private final Vector3fc position;
    /** type of tracks that this node connects */
    private final TrackType type;

    public RailNode(Vector3fc nodePoint, TrackType type, Vector3fc direction) {
        this.position = new Vector3f(nodePoint);
        this.direction = new Vector3f(direction);
        this.type = type;
    }

    public RailNode(RailNode target) {
        this(target.position, target.type, target.direction);
    }

    /**
     * @param other another node
     * @return the direction of track leaving this node if it were to connect to other
     */
    public Vector3fc getDirectionTo(RailNode other) {
        return isInDirectionOf(other) ? direction : new Vector3f(direction).negate();
    }

    private boolean isInDirectionOf(RailNode other) {
        Vector3f thisToOther = new Vector3f(other.position).sub(position);
        return thisToOther.dot(direction) > 0;
    }

    public TrackType getType() {
        return type;
    }

    /**
     * @return the Direction of the given node in the direction lists of this node, or null if the given node is not
     * connected to this node.
     */
    public Direction getEntryOf(RailNode railNode) {
        int i = getIndexOf(aDirection, railNode);
        if (i != -1) {
            return aDirection.get(i);
        } else {
            int i2 = getIndexOf(bDirection, railNode);
            if (i2 == -1) return null;
            return bDirection.get(i2);
        }
    }

    /**
     * removes a connection between this node and the given target node
     * @param target a node this is connected to
     * @return the track connecting the two nodes iff the target was indeed connected to this node, and has now been
     * removed. If there is no such connection, this returns null
     */
    private TrackPiece removeNode(RailNode target) {
        boolean wasNetwork = this.isNetworkNode();
        Direction removed;

        List<Direction> list = aDirection;
        int i = getIndexOf(list, target);
        if (i != -1) {
            removed = list.remove(i);

        } else {
            list = bDirection;
            int j = getIndexOf(list, target);
            if (j != -1) {
                removed = list.remove(j);

            } else {
                return null;
            }
        }

        if (isEnd()) {
            List<Direction> others = getNext(target);
            for (Direction entry : others) {
                entry.railNode.updateNetwork(this, null, 0);
            }

        } else if (wasNetwork && !isNetworkNode()) {
            assert isStraight() : this; // !isEnd() && !isSwitch() assuming (!isNetworkNode() => !isSwitch())
            List<Direction> otherDirections = getNext(target);

            assert otherDirections.size() == 1 : otherDirections;
            Direction oneDirection = otherDirections.get(0);
            assert list.size() == 1 : list;
            Direction twoDirection = list.get(0);

            oneDirection.railNode.updateNetwork(this, twoDirection.networkNode, twoDirection.distanceToNetworkNode);
            twoDirection.railNode.updateNetwork(this, oneDirection.networkNode, oneDirection.distanceToNetworkNode);
        }

        return removed.trackPiece;
    }

    public Iterable<Direction> getAllEntries() {
        return Toolbox.combinedList(aDirection, bDirection);
    }

    /**
     * returns the nodes that follows from passing this node from the direction of the given previous node.
     * @param previous the node you just left
     * @return the logical next nodes on the track
     */
    public List<Direction> getNext(RailNode previous) {
        if (getIndexOf(aDirection, previous) != -1) {
            return bDirection;

        } else {
            assert getIndexOf(bDirection, previous) != -1 : "Not connected to node " + previous;
            return aDirection;
        }
    }

    /**
     * returns the nodes that follows from passing this node from the direction of the given leaving track.
     * @param track one track connected to this node
     * @return the logical next nodes on the track
     */
    public List<Direction> getNext(TrackPiece track) {
        return getNext(track.getNot(this));
    }

    public Vector3fc getDirectionTo(Vector3fc point) {
        Vector3f thisToOther = new Vector3f(point).sub(position);
        boolean isSameDirection = thisToOther.dot(direction) > 0;
        return isSameDirection ? direction : new Vector3f(direction).negate();
    }

    public Vector3fc getPosition() {
        return position;
    }

    /** @return true iff this node has no connections on one (or both) side. */
    private boolean isEnd() {
        return aDirection.isEmpty() || bDirection.isEmpty();
    }

    /** @return true iff this node connects multiple tracks on at least one side. */
    private boolean isSwitch() {
        return aDirection.size() > 1 || bDirection.size() > 1;
    }

    /** @return true iff this node has exactly one connection on both sides. isStraight <=> !isEnd && !isSwitch */
    private boolean isStraight() {
        return aDirection.size() == 1 && bDirection.size() == 1;
    }

    /** @return true iff this node forms a connection in the network */
    protected boolean isNetworkNode() {
        return isSwitch();
    }

    /**
     * set the network node indicator in the direction of (this to source) to the newNetworkNode.
     * @param source         the node in whose direction the new networknode is set.
     * @param newNetworkNode the first network node in direction of source.
     * @param distance       distance between source and newNetworkNode
     */
    public void updateNetwork(RailNode source, RailNode newNetworkNode, float distance) {
        assert newNetworkNode == null || newNetworkNode.isNetworkNode() : newNetworkNode + " | " + source;

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

        // if this is already set correctly, then the remainder is set correctly as well
        if (Objects.equals(newNetworkNode, entry.networkNode)) return;

        // we propagate backwards, hence distance increases
        float newDistance = distance + entry.trackPiece.getLength();
        Logger.WARN.print(this, newNetworkNode, newDistance);
        list.set(i, new Direction(source, entry.trackPiece, newNetworkNode, newDistance));

        // when straight, propagate the change
        if (this.isStraight()) {
            assert otherList.size() == 1;
            RailNode next = otherList.get(0).railNode;
            next.updateNetwork(this, newNetworkNode, newDistance);
        }
    }

    @Override
    public String toString() {
        return "Node{" + aDirection.size() + ":" + bDirection.size() + " @" + Vectors.toString(position) + "}";
    }

    /** returns the index of the element containing the given node, or -1 if no such element exists */
    private static int getIndexOf(List<Direction> aDirection, RailNode target) {
        for (int i = 0; i < aDirection.size(); i++) {
            Direction d = aDirection.get(i);
            if (d.railNode.equals(target)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * connects the two end nodes together
     */
    public static void addConnection(TrackPiece track) {
        addConnection(track, track.getStartNode(), track.getEndNode());
    }

    /**
     * connects oneNode with twoNode using the given track. Updates the network node entries on both nodes
     */
    public static void addConnection(TrackPiece track, RailNode oneNode, RailNode twoNode) {
        boolean oneWasNetwork = oneNode.isNetworkNode();
        boolean twoWasNetwork = twoNode.isNetworkNode();

        List<Direction> oneToTwo;
        List<Direction> oneToOther;
        List<Direction> twoToOne;
        List<Direction> twoToOther;

        if (oneNode.isInDirectionOf(twoNode)) {
            oneToTwo = oneNode.aDirection;
            oneToOther = oneNode.bDirection;

        } else {
            oneToTwo = oneNode.bDirection;
            oneToOther = oneNode.aDirection;
        }

        if (twoNode.isInDirectionOf(oneNode)) {
            twoToOne = twoNode.aDirection;
            twoToOther = twoNode.bDirection;

        } else {
            twoToOne = twoNode.bDirection;
            twoToOther = twoNode.aDirection;
        }

        // these must be measured before adding
        boolean oneBecomesStraight = oneToTwo.isEmpty() && oneToOther.size() == 1;
        boolean twoBecomesStraight = twoToOne.isEmpty() && twoToOther.size() == 1;

        addEntry(oneToTwo, twoToOther, twoNode, twoBecomesStraight, track);
        addEntry(twoToOne, oneToOther, oneNode, oneBecomesStraight, track);

        // these must occur after adding
        updateNetwork(oneNode, twoNode, oneWasNetwork);
        updateNetwork(twoNode, oneNode, twoWasNetwork);

        Logger.WARN.print("From   ", oneNode, oneWasNetwork, oneNode.isNetworkNode(), oneBecomesStraight);
        Logger.WARN.print("To     ", twoNode, twoWasNetwork, twoNode.isNetworkNode(), twoBecomesStraight);
        Logger.WARN.print("Network:");
        System.err.println(RailNode.getNetworkAsString(twoNode));
    }

    private static void addEntry(
            List<Direction> list, List<Direction> otherList, RailNode twoNode, boolean twoBecomesStraight,
            TrackPiece track
    ) {
        if (twoBecomesStraight) {
            assert otherList.size() == 1;
            // copy the network node of oneToOther into twoToOne.
            Direction entryOfOther = otherList.get(0);
            float distanceToNetwork = entryOfOther.distanceToNetworkNode + track.getLength();
            list.add(new Direction(twoNode, track, entryOfOther.networkNode, distanceToNetwork));

        } else {
            list.add(new Direction(twoNode, track, null, 0));
        }
    }

    /** checks whether oneNode became a network node and updates all connected nodes accordingly. */
    private static void updateNetwork(RailNode oneNode, RailNode twoNode, boolean oneWasNetwork) {
        if (oneNode.isNetworkNode()) {
            if (oneWasNetwork) {
                twoNode.updateNetwork(oneNode, oneNode, 0);

            } else {
                for (Direction entry : oneNode.getAllEntries()) {
                    entry.railNode.updateNetwork(oneNode, oneNode, 0);
                }
            }
        }
    }

    /**
     * removes the connection between the nodes, and returns the track piece to be removed
     * @param aNode
     * @param bNode
     * @return an unmodified track piece. This should be disposed, but could potentially be re-inserted with {@link
     * #addConnection(TrackPiece, RailNode, RailNode)}
     */
    public static TrackPiece removeConnection(RailNode aNode, RailNode bNode) {
        assert aNode.getEntryOf(bNode) != null && bNode.getEntryOf(aNode) != null;

        TrackPiece oldPiece = aNode.removeNode(bNode);
        TrackPiece sameOldPiece = bNode.removeNode(aNode);

        assert oldPiece == sameOldPiece :
                "Nodes were mutually connected with a different track piece (" + oldPiece + " and " + sameOldPiece + ")";
        assert oldPiece != null : "Invalid tracks between " + aNode + " and " + bNode;

        return oldPiece;
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
            RailNode oneNode, RailNode twoNode, RailNode newNode, TrackPiece oneTrack, TrackPiece twoTrack
    ) {
        assert newNode.aDirection.isEmpty() && newNode.bDirection.isEmpty() : "newNode should be empty | " + newNode;

        Direction entryToTwo = oneNode.getEntryOf(twoNode);
        Direction entryToOne = twoNode.getEntryOf(oneNode);

        replaceNode(oneNode, twoNode, newNode, oneTrack);
        replaceNode(twoNode, oneNode, newNode, twoTrack);
    }

    private static void replaceNode(
            RailNode oneNode, RailNode twoNode, RailNode newNode, TrackPiece track
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
        oneList.set(twoIndex, new Direction(newNode, track, entryOneToTwo.networkNode, entryOneToTwo.distanceToNetworkNode));

        RailNode networkNode;
        float distanceToNetworkOfNewToOne;

        if (oneNode.isNetworkNode()) {
            networkNode = oneNode;
            distanceToNetworkOfNewToOne = 0;
        } else if (oneNode.isEnd()) {
            networkNode = null;
            distanceToNetworkOfNewToOne = 0;
        } else {
            Direction direction = otherList.get(0);
            networkNode = direction.networkNode;
            distanceToNetworkOfNewToOne = direction.distanceToNetworkNode + track.getLength();
        }

        Direction newEntry = new Direction(oneNode, track, networkNode, distanceToNetworkOfNewToOne);

        if (newNode.isInDirectionOf(newEntry.railNode)) {
            newNode.aDirection.add(newEntry);
        } else {
            newNode.bDirection.add(newEntry);
        }
    }

    /**
     * translates the top-level pathing network to a string.
     * @param startNode A starting node on the network, which will be id 0.
     * @return a comma separated list of pairs "a-b" with numbers a and b representing arbitrary ID values of nodes.
     * pairs may occur the other way around. dead ends are marked by 'x'
     */
    public static String getNetworkAsString(RailNode startNode) {
        List<Edge> pairs = new ArrayList<>();
        Map<RailNode, Integer> seen = new HashMap<>();
        Deque<RailNode> open = new ArrayDeque<>();
        open.add(startNode);
        seen.put(startNode, 0);

        while (!open.isEmpty()) {
            RailNode current = open.remove();
            Integer currentID = seen.get(current);

            for (Direction entry : current.getAllEntries()) {
                RailNode other = entry.networkNode;

                if (other != null) {
                    if (!seen.containsKey(other)) {
                        int id = seen.size();
                        seen.put(other, id);
                        open.add(other);
                    }

                    Integer otherID = seen.get(other);
                    pairs.add(new Edge(currentID, otherID, entry.distanceToNetworkNode));
                }
                // else this is an endpoint
            }
        }

        if (pairs.isEmpty()) return "";

        // +1 for 1-indexing
        StringBuilder output = new StringBuilder("matrix(c(");
        pairs.forEach(e -> output.append(e.a + 1).append(", ")
                .append(e.b + 1).append(", ")
                .append(e.dist).append(", ")
        );

        output.setLength(output.length() - 2);
        output.append("), nc = 3, byrow = TRUE").append(")");
        return output.toString();
    }

    private static class Edge {
        int a;
        int b;
        float dist;

        public Edge(Integer thisID, Integer otherID, float distanceToNetwork) {
            this.a = thisID;
            this.b = otherID;
            this.dist = distanceToNetwork;
        }
    }

    public static class Direction {
        public final RailNode railNode;
        public final TrackPiece trackPiece;
        public RailNode networkNode;
        public float distanceToNetworkNode;

        private Direction(RailNode railNode, TrackPiece trackPiece, RailNode networkNode, float distanceToNetworkNode) {
            this.railNode = railNode;
            this.trackPiece = trackPiece;
            this.networkNode = networkNode;
            this.distanceToNetworkNode = distanceToNetworkNode;
        }

        @Override
        public String toString() {
            return "{" + railNode + ", " + trackPiece + "," + networkNode + "}";
        }

    }
}
