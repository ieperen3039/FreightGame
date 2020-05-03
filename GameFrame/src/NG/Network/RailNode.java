package NG.Network;

import NG.DataStructures.Generic.Pair;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import NG.Tracks.TrackPiece;
import NG.Tracks.TrackType;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

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
     * @return pair (left, right) with left = the first node in (direction) that is a switch, and right = the direction
     * entry of this switch pointing back to this. If no switch exists in this direction (= this is a dead end) returns
     * null.
     */
    private Pair<RailNode, Direction> getFirstSwitchRecursive(RailNode next) {
        if (next.isNetworkNode()) {
            Direction thisAsEntry = next.getEntryOf(this);
            return new Pair<>(next, thisAsEntry);
        }
        List<Direction> options = next.getNext(this);
        if (options.isEmpty()) return null;

        assert options.size() == 1;
        return getFirstSwitchRecursive(options.get(0).railNode);
    }

    /**
     * @return the Direction of the given node in the direction lists of this node.
     */
    private Direction getEntryOf(RailNode railNode) {
        int i = getIndexOf(aDirection, railNode);
        if (i != -1) {
            return aDirection.get(i);
        } else {
            return bDirection.get(getIndexOf(bDirection, railNode));
        }
    }

    /**
     * removes a connection between this node and the given target node
     * @param target a node this is connected to
     * @return the track connecting the two nodes iff the target was indeed connected to this node, and has now been
     * removed. If there is no such connection, this returns null
     */
    private TrackPiece removeNode(RailNode target) {
        int i = getIndexOf(aDirection, target);
        if (i != -1) {
            Direction removed = aDirection.remove(i);
            return removed.trackPiece;
        }

        int j = getIndexOf(bDirection, target);
        if (j != -1) {
            Direction removed = bDirection.remove(j);
            return removed.trackPiece;
        }

        return null;
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
     * returns the node that follows from passing this node from the direction of the given previous node, according to
     * the state of this node. If {@link #isSwitch()} returns true, this is guaranteed to be at most one element
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

    public Vector3fc getDirectionTo(Vector3fc point) {
        Vector3f thisToOther = new Vector3f(point).sub(position);
        boolean isSameDirection = thisToOther.dot(direction) > 0;
        return isSameDirection ? direction : new Vector3f(direction).negate();
    }

    public Vector3fc getPosition() {
        return position;
    }

    /** @return true iff this node has no connections on one (or both) side */
    private boolean isEnd() {
        return aDirection.isEmpty() || bDirection.isEmpty();
    }

    /** @return true iff this node connects multiple tracks on at least one side */
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

    @Override
    public String toString() {
        return "Node " + aDirection.size() + ":" + bDirection.size() + " " + Vectors.toString(position) + "";
    }

    /**
     * connects the two end nodes together
     */
    public static void addConnection(TrackPiece track) {
        addConnection(track, track.getStartNode(), track.getEndNode());
    }

    /**
     * connects oneNode with twoNode using the given track.
     */
    public static void addConnection(TrackPiece track, RailNode oneNode, RailNode twoNode) {
        List<Direction> oneTwoDirection;
        List<Direction> oneAntiDirection;
        List<Direction> twoOneDirection;
        List<Direction> twoAntiDirection;

        if (oneNode.isInDirectionOf(twoNode)) {
            oneTwoDirection = oneNode.aDirection;
            oneAntiDirection = oneNode.bDirection;
        } else {
            oneTwoDirection = oneNode.bDirection;
            oneAntiDirection = oneNode.aDirection;
        }
        if (twoNode.isInDirectionOf(oneNode)) {
            twoOneDirection = twoNode.aDirection;
            twoAntiDirection = twoNode.bDirection;
        } else {
            twoOneDirection = twoNode.bDirection;
            twoAntiDirection = twoNode.aDirection;
        }

        Direction oneDirection = new Direction(twoNode, track);
        Direction twoDirection = new Direction(oneNode, track);
        oneTwoDirection.add(oneDirection);
        twoOneDirection.add(twoDirection);

        Pair<RailNode, Direction> oneSideSwitch = getSwitch(oneNode, oneDirection, oneAntiDirection);
        Pair<RailNode, Direction> twoSideSwitch = getSwitch(twoNode, twoDirection, twoAntiDirection);

        if (!twoNode.isEnd()) {
            Object oneside = oneSideSwitch == null ? "null" : oneSideSwitch.left;
            Object twoside = twoSideSwitch == null ? "null" : twoSideSwitch.left;
            Logger.WARN.print(oneside, twoside);
        }

        if (oneSideSwitch != null && twoSideSwitch != null) {
            NetworkNode.createNetworkNode(oneSideSwitch.left, oneSideSwitch.right, twoSideSwitch.left, twoSideSwitch.right);
        }
    }

    private static Pair<RailNode, Direction> getSwitch(
            RailNode node, Direction direction, List<Direction> antiDirections
    ) {
        if (node.isNetworkNode() || node.isStraight()) {
            return new Pair<>(node, direction);

        } else if (antiDirections.isEmpty()) {
            // the node is an unconnected node
            return null;

        } else {
            Direction oneBDir = antiDirections.get(0);
            return node.getFirstSwitchRecursive(oneBDir.railNode);
        }
    }

    /**
     * removes the connection between the nodes, and returns the track piece to be removed
     * @param aNode
     * @param bNode
     * @return an unmodified track piece. This should be disposed, but could potentially be re-inserted with {@link
     * #addConnection(TrackPiece, RailNode, RailNode)}
     */
    static TrackPiece removeConnection(RailNode aNode, RailNode bNode) {
        // remove the networknode describing this connection (if any)
        Pair<RailNode, Direction> bSideSwitch = bNode.getFirstSwitchRecursive(aNode);
        if (bSideSwitch != null) bSideSwitch.right.networkNode = null;

        Pair<RailNode, Direction> aSideSwitch = aNode.getFirstSwitchRecursive(bNode);
        if (aSideSwitch != null) aSideSwitch.right.networkNode = null;

        // now remove the connection itself
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
        replaceNode(oneNode, twoNode, newNode, oneTrack);
        replaceNode(twoNode, oneNode, newNode, twoTrack);
    }

    /** replaces twoNode with newNode in the direction list of oneNode, using the given newTrack */
    private static void replaceNode(RailNode oneNode, RailNode twoNode, RailNode newNode, TrackPiece newTrack) {
        int twoIndex;
        List<Direction> oneList = oneNode.aDirection;

        twoIndex = getIndexOf(oneList, twoNode);
        if (twoIndex == -1) {
            oneList = oneNode.bDirection;
            twoIndex = getIndexOf(oneList, twoNode);

            assert twoIndex != -1 : "nodes are not connected";
        }

        oneList.set(twoIndex, new Direction(newNode, newTrack));

        if (newNode.isInDirectionOf(oneNode)) {
            newNode.aDirection.add(new Direction(oneNode, newTrack));
        } else {
            newNode.bDirection.add(new Direction(oneNode, newTrack));
        }
    }

    public static class Direction {
        public final RailNode railNode;
        public final TrackPiece trackPiece;
        public NetworkNode networkNode = null;

        public Direction(RailNode railNode, TrackPiece trackPiece) {
            this.railNode = railNode;
            this.trackPiece = trackPiece;
        }
    }
}
