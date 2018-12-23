package NG.Tracks;

import NG.Engine.Game;
import org.joml.Vector2fc;

/**
 * @author Geert van Ieperen created on 23-12-2018.
 */
public class ConnectionNode extends NetworkNode {
    /*
     * Representation Invariants
     * if aNode == null then bNode == null
     * if aNode == null then aTrack == null
     * if bNode == null then bTrack == null
     * getArity() == 2
     * (isConnected == true) iff (aNode != null && bNode != null)
     * ((this.position to aNode.position) dot this.direction) < 0
     * ((this.position to bNode.position) dot this.direction) >= 0
     */

    private NetworkNode aNode;
    private TrackPiece aTrack;

    private NetworkNode bNode;
    private TrackPiece bTrack;

    private boolean isReplaced = false;

    /**
     * an unconnected node
     * @param position a position on the map
     * @param type     the type of track that this node connects
     */
    protected ConnectionNode(Vector2fc position, TrackMod.TrackType type) {
        super(position, type);
    }

    /**
     * creates a node between the two given nodes
     * @param game     the current game instance
     * @param position the position of this node on the map, such that it lies on the tracks from aNode to bNode
     * @param aNode    one node to connect to
     * @param bNode    another node to connect to
     */
    public ConnectionNode(Game game, Vector2fc position, NetworkNode aNode, NetworkNode bNode) {
        super(position, aNode.type);
        assert aNode.type.equals(bNode.type); // see SwitchNode

        boolean success = removeConnection(game, aNode, bNode);
        assert success : "Connection did not exist";

        addConnection(game, aNode, this);
        addConnection(game, this, bNode);
    }

    /**
     * Creates a new node, connected on one side only
     * @param game     the game instance
     * @param position the position of this new node
     * @param source   the original node
     */
    public ConnectionNode(Game game, Vector2fc position, NetworkNode source) {
        super(position, source.type);
        this.aNode = source;
        addConnection(game, aNode, this);
        this.direction = aTrack.getEndDirection();
    }

    @Override
    protected NetworkNode addNode(NetworkNode newNode, TrackPiece track) {
        assert !isReplaced : "This node has been replaced";
        if (aNode == null) {
            aNode = newNode;
            aTrack = track;
            direction = track.getEndDirection();
            return this;

        } else if (bNode == null) {
            bNode = newNode;
            bTrack = track;
            return this;

        } else {
            SwitchNode replacement = new SwitchNode(this);
            isReplaced = true;
            return replacement.addNode(newNode, track);
        }
    }

    @Override
    protected TrackPiece removeNode(NetworkNode target) {
        assert !isReplaced : "This node has been replaced";
        if (target == aNode) {
            aNode = bNode;
            TrackPiece removed = aTrack;
            aTrack = bTrack;
            bTrack = null;
            bNode = null;
            return removed;
        }
        if (target == bNode) {
            bNode = null;
            TrackPiece removed = bTrack;
            bTrack = null;
            return removed;
        }
        return null;
    }

    @Override
    public NetworkNode getNext(NetworkNode previous) {
        assert !isReplaced : "This node has been replaced";
        if (previous == aNode) {
            return bNode;
        } else {
            assert previous == bNode : "Not connected to node " + previous;
            return aNode;
        }
    }

    @Override
    public boolean isConnected() {
        return bNode == null;
    }

    NetworkNode getANode() {
        return aNode;
    }

    TrackPiece getATrack() {
        return aTrack;
    }

    NetworkNode getBNode() {
        return bNode;
    }

    TrackPiece getBTrack() {
        return bTrack;
    }
}
