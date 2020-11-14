package NG.Network;

import NG.Core.Game;
import NG.Core.GameObject;
import NG.Tools.Vectors;
import NG.Tracks.TrackPiece;
import NG.Tracks.TrackType;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.io.Serializable;
import java.util.List;

/**
 * A node that connects two track pieces together
 * @author Geert van Ieperen created on 16-12-2018.
 */
public class RailNode implements Serializable, GameObject {
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
    private final Signal eolSignal;

    /** position of this node */
    private final Vector3fc position;
    /** direction of this node, normalized */
    private final Vector3fc direction;

    /** type of tracks that this node connects */
    private transient TrackType type;
    private final String typeName;
    private final NetworkNode networkNode;

    /** optional signal on this node */
    private SignalEntity signal = null;

    public RailNode(Vector3fc nodePoint, TrackType type, Vector3fc direction) {
        this(nodePoint, type, direction, new NetworkNode());
    }

    public RailNode(
            Vector3fc nodePoint, TrackType type, Vector3fc direction, NetworkNode networkNode
    ) {
        this.position = new Vector3f(nodePoint);
        this.direction = new Vector3f(direction.x(), direction.y(), 0).normalize();
        this.type = type;
        this.typeName = type.toString();
        this.networkNode = networkNode;
        this.eolSignal = new Signal(this, true, false);
    }

    public RailNode(RailNode source, TrackType newType) {
        this.position = source.position;
        this.direction = source.direction;
        this.type = newType;
        this.typeName = type.toString();
        this.networkNode = source.networkNode;
        this.signal = null;
        this.eolSignal = null;
    }

    public TrackType getType() {
        return type;
    }

    public Vector3fc getPosition() {
        return position;
    }

    /**
     * @return normalized direction. NetworkNode A-side is in this direction
     */
    public Vector3fc getDirection() {
        return direction;
    }

    /**
     * @return normalized direction d of this node, such that {@code point.sub(getPosition()).dot(d) > 0}
     */
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
        return networkNode.isEnd() || signal != null;
    }

    public Signal getSignal() {
        return networkNode.isEnd() ? eolSignal : signal;
    }

    public Signal addSignal(Game game, boolean inNodeDirection) {
        this.signal = new SignalEntity(game, this, inNodeDirection, true);
        game.state().addEntity(signal);
        return signal;
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

    /**
     * returns whether this node is in the direction of the given track. Note that if you are on {@code track} and
     * approach this node in the direction of this node, {@code isInDirectionOf(track)} will return false.
     * @return true iff this node points in the direction of {@code track}.
     */
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

    @Override
    public void restore(Game game) {
        if (type == null) {
            type = game.objectTypes().getTrackByName(typeName);
        }
    }
}
