package NG.Network;

import NG.Core.Game;
import NG.Tools.Vectors;
import NG.Tracks.TrackPiece;
import NG.Tracks.TrackType;
import org.joml.Vector3f;
import org.joml.Vector3fc;

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

    /** position of this node */
    private final Vector3fc position;
    private final Vector3fc direction;

    /** type of tracks that this node connects */
    private final TrackType type;
    private final NetworkNode networkNode;

    /** optional signal on this node */
    private Signal signal = null;

    public RailNode(Vector3fc nodePoint, TrackType type, Vector3fc direction) {
        this(nodePoint, type, direction, new NetworkNode());
    }

    public RailNode(
            Vector3fc nodePoint, TrackType type, Vector3fc direction, NetworkNode networkNode
    ) {
        this.position = new Vector3f(nodePoint);
        this.direction = new Vector3f(direction).normalize();
        this.type = type;
        this.networkNode = networkNode;
    }

    public RailNode(RailNode source, TrackType newType) {
        this.position = source.position;
        this.direction = source.direction;
        this.type = newType;
        this.networkNode = source.networkNode;
        this.signal = null;
    }

    public TrackType getType() {
        return type;
    }

    public Vector3fc getPosition() {
        return position;
    }

    public Vector3fc getDirection() {
        return direction;
    }

    public Vector3fc getDirectionTo(Vector3fc point) {
        Vector3f thisToOther = new Vector3f(point).sub(position);
        boolean isSameDirection = thisToOther.dot(direction) > 0;
        return isSameDirection ? direction : new Vector3f(direction).negate();
    }

    /**
     * @return if {@link #getNetworkNode()}{@link NetworkNode#isEnd() .isEnd()}, returns the direction vector in the
     * empty direction. Otherwise return null
     */
    public Vector3fc getOpenDirection() {
        if (!networkNode.isEnd()) return null;
        return networkNode.getEntriesA().isEmpty() ? direction : new Vector3f(direction).negate();
    }

    @Override
    public String toString() {
        return Vectors.toString(position);
    }

    public NetworkNode getNetworkNode() {
        return networkNode;
    }

    public boolean hasSignal() {
        return signal != null;
    }

    public Signal getSignal() {
        return signal;
    }

    public void addSignal(Game game) {
        this.signal = new Signal(game, this, true, true);
        game.state().addEntity(signal);
    }

    public void removeSignal(Game game) {
        signal.despawn(game.timer().getGameTime());
        signal = null;
    }

    public List<NetworkNode.Direction> getEntriesFromDirection(Vector3fc nodeDirection) {
        boolean isInDirection = getDirection().dot(nodeDirection) > 0;
        return isInDirection ? networkNode.getEntriesA() : networkNode.getEntriesB();
    }

    public boolean isUnconnected() {
        return networkNode.getEntriesA().isEmpty() && networkNode.getEntriesB().isEmpty();
    }

    public boolean isInDirectionOf(TrackPiece track) {
        assert track.getStartNode().equals(this) || track.getEndNode().equals(this);

        for (NetworkNode.Direction entry : networkNode.getEntriesA()) {
            if (entry.trackPiece.equals(track)) {
                return true;
            }
        }

        // assuming track is part of bDirection
        return false;
    }
}
