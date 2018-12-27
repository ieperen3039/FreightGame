package NG.Tracks;

import NG.Tools.Vectors;
import org.joml.Vector2f;

/**
 * @author Geert van Ieperen created on 23-12-2018.
 */
public class ConnectionNode extends NetworkNode {
    /*
     * Representation Invariants
     * isReplaced == false (whenever a method is used)
     * if aNode == null then bNode == null
     * if aNode == null then aTrack == null
     * if bNode == null then bTrack == null
     * (isConnected() == true) iff (aNode != null && bNode != null)
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
     * @param nodePoint the point in space associated with this node
     * @param type      the type of track that this node connects
     */
    protected ConnectionNode(NetworkNodePoint nodePoint, TrackMod.TrackType type) {
        super(nodePoint, type);
    }

    @Override
    protected NetworkNode addNode(NetworkNode newNode, TrackPiece track) {
        assert !isReplaced : "This node has been replaced";
        if (aNode == null) {
            aNode = newNode;
            aTrack = track;
            direction = track.getDirectionOf(nodePoint);
            return this;

        } else if (bNode == null) {
            bNode = newNode;
            bTrack = track;

            assert direction.angle(track.getDirectionOf(nodePoint)) < 0.01f || direction.angle(track.getDirectionOf(nodePoint)
                    .negate(new Vector2f())) < 0.01f :
                    String.format("New connection is not fluent with existing connection: %s instead of %s",
                            Vectors.toString(track.getDirectionOf(nodePoint)), Vectors.toString(direction));
            return this;

        } else {
            SwitchNode replacement = new SwitchNode(this);
            // let position on the map refer to this new replacement node
            nodePoint.setReference(replacement);
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
        return bNode != null;
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
