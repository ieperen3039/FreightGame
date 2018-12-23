package NG.Tracks;

import NG.DataStructures.Pair;
import NG.Engine.Game;
import org.joml.Vector2f;
import org.joml.Vector2fc;

/**
 * @author Geert van Ieperen created on 23-12-2018.
 */
public abstract class NetworkNode {
    /** map coordinates of this node */
    protected Vector2fc position;
    /** direction of the bNode on this position */
    protected Vector2fc direction; // normalized
    /** type of tracks that this node connects */
    protected TrackMod.TrackType type;

    protected NetworkNode(Vector2fc position, TrackMod.TrackType type) {
        this.position = position;
        this.type = type;
    }

    /**
     * adds a connection including tracks between node aNode and node bNode, such that the track connects aNode and
     * bNode
     * @param game  the current game instance
     * @param aNode one node
     * @param bNode another node
     */
    protected static void addConnection(Game game, NetworkNode aNode, NetworkNode bNode) {
        TrackPiece trackConnection = TrackPiece.getTrackPiece(game, aNode.type, aNode.position, aNode.direction, bNode.position);
        game.state().addEntity(trackConnection);
        aNode.addNode(bNode, trackConnection);
        bNode.addNode(aNode, trackConnection);
    }

    /**
     * removes the track and references of both nodes to each other
     * @param game
     * @param aNode one node
     * @param bNode another node connected to aNode
     * @return false iff the two nodes were not connected.
     */
    protected static boolean removeConnection(Game game, NetworkNode aNode, NetworkNode bNode) {
        TrackPiece trackPiece = aNode.removeNode(bNode);
        TrackPiece samePiece = bNode.removeNode(aNode);

        assert trackPiece == samePiece : "Nodes are mutually connected with a different track piece (" +
                trackPiece + " and " + samePiece + ")";

        if (trackPiece == null) return false;
        game.state().removeEntity(trackPiece);
        return true;
    }

    /**
     * creates two nodes that are only connected together with a straight piece of track.
     * @param game      the current game instance
     * @param type      the type of track
     * @param aPosition one position of the map
     * @param bPosition another position on the map
     * @return a pair of nodes, where one of the two is at position {@code aPosition} and the other at {@code
     * bPosition}, and which are connected with an instance of {@link StraightTrack}
     */
    public static Pair<NetworkNode, NetworkNode> getNodePair(
            Game game, TrackMod.TrackType type, Vector2fc aPosition, Vector2fc bPosition
    ) {
        ConnectionNode A = new ConnectionNode(aPosition, type);
        ConnectionNode B = new ConnectionNode(bPosition, type);
        A.direction = new Vector2f(bPosition).sub(aPosition);
        addConnection(game, A, B);

        return new Pair<>(A, B);
    }

    public static NetworkNode connectToNew(Game game, NetworkNode node, Vector2fc position) {
        ConnectionNode newNode = new ConnectionNode(position, node.type);
        addConnection(game, node, newNode);
        return newNode;
    }

    public Vector2fc getPosition() {
        return position;
    }

    public Vector2fc getDirection() {
        return direction;
    }

    /**
     * adds a connection to another node, with the given track as connecting track piece
     * @param newNode the new node to add
     * @param track   the track that connects these two nodes
     * @return the resulting node that is connected to all nodes this is connected to, plus the given newNode.
     */
    protected abstract NetworkNode addNode(NetworkNode newNode, TrackPiece track);

    /**
     * removes a connection between this node and the given target node
     * @param target a node this is connected to
     * @return the track connecting the two nodes iff the target was indeed connected to this node, and has now been
     * removed. If there is no such connection, this returns null
     */
    protected abstract TrackPiece removeNode(NetworkNode target);

    /**
     * returns the node that follows from passing this node from the direction of the given previous node, according to
     * the state of this node.
     * @param previous the node you just left
     * @return the logical next node on the track
     * @see SwitchNode#setSwitchState(NetworkNode)
     */
    public abstract NetworkNode getNext(NetworkNode previous);

    /**
     * @return true iff both sides of this node have a connection to another node
     */
    public abstract boolean isConnected();

}
