package NG.Tracks;

import NG.Core.Game;
import NG.Entities.Entity;
import NG.Network.NetworkNode;
import NG.Tools.Logger;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen. Created on 18-9-2018.
 */
public interface TrackPiece extends Entity {

    @Override
    default UpdateFrequency getUpdateFrequency() {
        return UpdateFrequency.NEVER;
    }

    Vector3f closestPointOf(Vector3fc origin, Vector3fc direction);

    TrackType getType();

    NetworkNode getStartNode();

    NetworkNode getEndNode();

    /**
     * gives the normalized direction of the track at the endNodePoint pointing outward.
     * @return the direction of the last point on the track.
     */
    Vector3fc getEndDirection();

    /**
     * gives the normalized direction of the track at the startNodePoint pointing outward.
     * @return the direction of the first point on the track.
     */
    Vector3fc getStartDirection();

    /**
     * @param node on of the two node points on this track
     * @return the associated direction of the track on that point
     */
    default Vector3fc getDirectionOf(NetworkNode node) {
        if (node.equals(getStartNode())) {
            return getStartDirection();
        } else {
            assert node.equals(getEndNode());
            return getEndDirection();
        }
    }

    /**
     * @param distanceFromStart distance traveled (no units)
     * @return the position after traveling {@code distanceFromStart} on this track, measured from the start.
     */
    Vector3f getPositionFromDistance(float distanceFromStart);

    /**
     * @param renderClickBox if true, render the area that can be clicked on. Otherwise, render the track itself.
     */
    void doRenderClickBox(boolean renderClickBox);

    /**
     * factory method for creating an arbitrary track between two points
     * @param game        the game instance
     * @param type        the track type
     * @param aPoint      an existing node A
     * @param endPosition the position B where this track piece should end
     * @param aDirection  the direction D of the track in point A, pointing towards B
     * @return a track that describes a track from A to B, having direction D in point A
     * @see StraightTrack
     * @see CircleTrack
     */
    static TrackPiece getTrackPiece(
            Game game, TrackType type, NetworkNode aPoint, Vector3fc aDirection, Vector3fc endPosition
    ) {
        Vector3fc aPos = aPoint.getPosition();
        Vector2f relPosB = new Vector2f(endPosition.x() - aPos.x(), endPosition.y() - aPos.y());
        if (relPosB.lengthSquared() < 1 / 256f) {
            Logger.ASSERT.print("Created track of length 0");
            return new StraightTrack(game, type, aPoint, endPosition);
        }

        Vector3f direction = new Vector3f(aDirection).normalize();
        Vector2f vecToB = new Vector2f(relPosB).normalize();

        float dot = vecToB.x * direction.x + vecToB.y * direction.y;
        if ((dot * dot) > 255 / 256f) {
            Logger.DEBUG.print("Creating straight track", aPoint.getPosition(), endPosition, "dot = " + dot);
            return new StraightTrack(game, type, aPoint, endPosition);

        } else {
            Logger.DEBUG.print("Creating circle track", aPoint.getPosition(), endPosition, "dot = " + dot);
            return new CircleTrack(game, type, aPoint, direction, endPosition);
        }
    }

    /**
     * factory method for creating an arbitrary track between two points
     * @param game       the game instance
     * @param type       the track type
     * @param aPoint     an existing node A
     * @param bPoint     an existing node B
     * @param aDirection the direction D of the track in point A, pointing towards B
     * @return a track that describes a track from A to B, having direction D in point A
     * @see StraightTrack
     * @see CircleTrack
     */
    static TrackPiece getTrackPiece(
            Game game, TrackType type, NetworkNode aPoint, Vector3fc aDirection, NetworkNode bPoint
    ) {
        assert aDirection.lengthSquared() > 0;

        Vector3fc bPos = bPoint.getPosition();
        Vector3fc aPos = aPoint.getPosition();
        Vector2f relPosB = new Vector2f(bPos.x() - aPos.x(), bPos.y() - aPos.y());
        if (relPosB.lengthSquared() < 1 / 256f) {
            Logger.ASSERT.print("Created track of length 0");
            return new StraightTrack(game, type, aPoint, bPoint);
        }

        Vector3f direction = new Vector3f(aDirection).normalize();
        Vector2f vecToB = new Vector2f(relPosB).normalize();

        float dot = vecToB.x * direction.x + vecToB.y * direction.y;
        if ((dot * dot) > 255 / 256f) {
            Logger.DEBUG.print("Creating straight track", aPos, bPos, "dot = " + dot);
            return new StraightTrack(game, type, aPoint, bPoint);

        } else {
            Logger.DEBUG.print("Creating circle track", aPos, bPos, "dot = " + dot);
            return new CircleTrack(game, type, aPoint, direction, bPoint);
        }
    }
}
