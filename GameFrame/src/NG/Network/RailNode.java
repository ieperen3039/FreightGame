package NG.Network;

import NG.Core.Game;
import NG.DataStructures.Generic.Pair;
import NG.Tools.Vectors;
import NG.Tracks.StraightTrack;
import NG.Tracks.TrackPiece;
import NG.Tracks.TrackType;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.Collection;
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

    private final List<RailNode> aNodes = new ArrayList<>(1);
    private final List<RailNode> bNodes = new ArrayList<>(1);
    private final List<TrackPiece> aTracks = new ArrayList<>(1);
    private final List<TrackPiece> bTracks = new ArrayList<>(1);
    private final Vector3fc direction;

    /** position of this node */
    private final Vector3fc position;
    /** type of tracks that this node connects */
    private final TrackType type;

    public RailNode(Vector3fc nodePoint, TrackType type, Vector3fc direction) {
        this.position = new Vector3f(nodePoint);
        this.type = type;
        this.direction = new Vector3f(direction);
    }

    public RailNode(RailNode target) {
        this(target.position, target.type, target.direction);
    }

    /**
     * @param other another node
     * @return the direction of track leaving this node if it were to connect to other
     */
    public Vector3fc getDirectionTo(RailNode other) {
        return isSameDirection(other) ? direction : new Vector3f(direction).negate();
    }

    private boolean isSameDirection(RailNode other) {
        Vector3f thisToOther = new Vector3f(other.position).sub(position);
        return thisToOther.dot(direction) > 0;
    }

    /**
     * adds a connection to another node, with the given track as connecting track piece
     * @param newNode the new node to add
     * @param track   the track that connects these two nodes
     */
    public void addNode(RailNode newNode, TrackPiece track) {
        if (isSameDirection(newNode)) {
            aNodes.add(newNode);
            aTracks.add(track);

        } else {
            bNodes.add(newNode);
            bTracks.add(track);
        }

    }

    /**
     * removes a connection between this node and the given target node
     * @param target a node this is connected to
     * @return the track connecting the two nodes iff the target was indeed connected to this node, and has now been
     * removed. If there is no such connection, this returns null
     */
    protected TrackPiece removeNode(RailNode target) {
        int i = aNodes.indexOf(target);
        if (i != -1) {
            aNodes.remove(i);
            return aTracks.remove(i);
        }

        int j = bNodes.indexOf(target);
        if (j != -1) {
            bNodes.remove(j);
            return bTracks.remove(j);
        }

        return null;
    }

    /**
     * returns the node that follows from passing this node from the direction of the given previous node, according to
     * the state of this node.
     * @param previous the node you just left
     * @return the logical next node on the track
     */
    public Collection<RailNode> getNext(RailNode previous) {
        if (aNodes.contains(previous)) {
            return bNodes;

        } else {
            assert bNodes.contains(previous) : "Not connected to node " + previous;
            return aNodes;
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

    @Override
    public String toString() {
        return "Node " + Vectors.toString(position) + "";
    }

    /**
     * creates two nodes that are only connected together with a straight piece of track.
     * @param game      the current game instance
     * @param type      the type of track
     * @param aPosition one position of the map
     * @param bPosition another position on the map
     * @return a {@link StraightTrack} piece that connects the
     */
    public static TrackPiece createNewTrack(Game game, TrackType type, Vector3fc aPosition, Vector3fc bPosition) {
        Vector3f AToB = new Vector3f(bPosition).sub(aPosition);
        RailNode A = new RailNode(aPosition, type, AToB);

        TrackPiece trackConnection = TrackPiece.getTrackPiece(
                game, A.type, A, AToB, bPosition
        );
        game.state().addEntity(trackConnection);
        return trackConnection;
    }

    /**
     * connect a NetworkNode to a new node created at the given position
     * @param game        the game instance
     * @param node        the node to connect
     * @param newPosition the position of the new node
     * @return the new node
     */
    public static RailNode createNew(Game game, RailNode node, Vector3fc newPosition) {
        TrackPiece trackConnection = TrackPiece.getTrackPiece(
                game, node.type, node, node.getDirectionTo(newPosition), newPosition
        );
        game.state().addEntity(trackConnection);

        return trackConnection.getEndNode();
    }

    /**
     * connects the given two nodes together by means of a single straight track and a single circle track. The tracks
     * are added to the game state, and the new node inbetween is returned.
     * @param game  the game instance
     * @param aNode one node to connect
     * @param bNode another node to connect
     * @return the node that had to be created to connect the two nodes.
     */
    public static RailNode createConnection(Game game, RailNode aNode, RailNode bNode) {
        Pair<TrackPiece, TrackPiece> trackPieces = TrackPiece.getTrackPiece(
                game, aNode.type, aNode, aNode.getDirectionTo(bNode), bNode, bNode.getDirectionTo(aNode)
        );

        game.state().addEntity(trackPieces.left);
        game.state().addEntity(trackPieces.right);

        return trackPieces.left.getEndNode();
    }

    public static RailNode createSplit(Game game, TrackPiece trackPiece, Vector3f point) {
        RailNode aNode = trackPiece.getStartNode();
        RailNode bNode = trackPiece.getEndNode();

        TrackType type = trackPiece.getType();
        Vector3fc aStartDir = new Vector3f(trackPiece.getStartDirection()).negate();

        removeTrack(trackPiece);

        TrackPiece aConnection = TrackPiece.getTrackPiece(
                game, type, aNode, aStartDir, point
        );
        game.state().addEntity(aConnection);

        RailNode newNode = aConnection.getEndNode();
        TrackPiece bConnection = TrackPiece.getTrackPiece(
                game, type, newNode, aConnection.getEndDirection(), bNode
        );
        game.state().addEntity(bConnection);

        return newNode;
    }

    public static void removeTrack(TrackPiece trackPiece) {
        RailNode aNode = trackPiece.getStartNode();
        RailNode bNode = trackPiece.getEndNode();

        TrackPiece oldPiece = aNode.removeNode(bNode);
        TrackPiece sameOldPiece = bNode.removeNode(aNode);

        assert oldPiece == sameOldPiece :
                "Nodes were mutually connected with a different track piece (" + oldPiece + " and " + sameOldPiece + ")";
        assert oldPiece != null : "Invalid tracks between " + aNode + " and " + bNode;
        assert oldPiece == trackPiece :
                "Nodes were connected with double tracks: " + oldPiece + " and " + trackPiece;

        trackPiece.dispose();
    }
}
