package NG.Tracks;

import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * A single node from the train network. All nodes are 1 to 1 or 1 to many (both are implemented as switches)
 * @author Geert van Ieperen created on 16-12-2018.
 */
public class SwitchNode extends NetworkNode {

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

    private List<NetworkNode> aNodes = new ArrayList<>();
    private List<NetworkNode> bNodes = new ArrayList<>();
    private List<TrackPiece> aTracks = new ArrayList<>();
    private List<TrackPiece> bTracks = new ArrayList<>();

    /** what otherNode this is connected to */
    private int switchStateA = 0;
    private int switchStateB = 0;

    public SwitchNode(ConnectionNode node) {
        super(node.position, node.type);
        this.direction = node.direction;

        NetworkNode aNode = node.getANode();
        TrackPiece aTrack = node.getATrack();
        aNode.removeNode(node);
        node.removeNode(aNode);
        aNode.addNode(this, aTrack);
        this.addNode(aNode, aTrack);

        NetworkNode bNode = node.getBNode();
        TrackPiece bTrack = node.getBTrack();
        bNode.addNode(this, bTrack);
        this.addNode(bNode, bTrack);

    }

    @Override
    protected NetworkNode addNode(NetworkNode newNode, TrackPiece track) {
        if (isOnSideA(newNode)) {
            aNodes.add(newNode);
            bNodes.add(newNode);

        } else {
            bNodes.add(newNode);
            bTracks.add(track);
        }

        return this;
    }

    /**
     * checks whether the given node lies on the a-side of this node
     * @param newNode a node not connected to this node
     * @return whether the position of newNode is on the A-side of the internal representation of this class
     */
    private boolean isOnSideA(NetworkNode newNode) {
        Vector2f relative = new Vector2f(newNode.position).sub(position);
        return direction.dot(relative) < 0;
    }

    @Override
    protected TrackPiece removeNode(NetworkNode target) {
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
     * sets this switch to connect to the given node. Upon returning, the {@link #getNext(NetworkNode)} method will
     * return {@code targetNode} when a node from the opposite side of {@code targetNode} is given.
     * @param targetNode the node to switch to
     */
    public void setSwitchState(NetworkNode targetNode) {
        int i = aNodes.indexOf(targetNode);
        if (i != -1) {
            switchStateA = i;

        } else {
            i = bNodes.indexOf(targetNode);
            assert i != -1 : "Not connected to " + targetNode;
            switchStateB = i;
        }
    }

    /**
     * given the node you just left, give the next node w.r.t. the current switch state
     * @param previous the node that has been traversed previously
     * @return the node that logically comes next according to the switch state
     */
    @Override
    public NetworkNode getNext(NetworkNode previous) {
        if (aNodes.contains(previous)) {
            return bNodes.get(switchStateB);

        } else {
            assert bNodes.contains(previous) : "Not connected to node " + previous;
            return aNodes.get(switchStateA);
        }
    }

    /** @return true iff the track is not connected on at least one side. */
    @Override
    public boolean isConnected() {
        return !(aNodes.isEmpty() || bNodes.isEmpty());
    }
}
