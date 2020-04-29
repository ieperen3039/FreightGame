package NG.Tracks;

import NG.Core.Game;
import NG.DataStructures.Generic.Pair;
import NG.Entities.Entity;
import NG.Network.NetworkNode;
import NG.Tools.Logger;
import org.joml.Math;
import org.joml.*;

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
     * factory method for creating an arbitrary track from a node to a point
     * @param game        the game instance
     * @param type        the track type
     * @param aNode       an existing node A
     * @param endPosition the position B where this track piece should end
     * @param aDirection  the direction D of the track in point A, pointing towards B
     * @return a track that describes a track from A to B, having direction D in point A
     * @see StraightTrack
     * @see CircleTrack
     */
    static TrackPiece getTrackPiece(
            Game game, TrackType type, NetworkNode aNode, Vector3fc aDirection, Vector3fc endPosition
    ) {
        Vector3fc aPos = aNode.getPosition();
        Vector2f relPosB = new Vector2f(endPosition.x() - aPos.x(), endPosition.y() - aPos.y());
        if (relPosB.lengthSquared() < 1 / 256f) {
            Logger.ASSERT.print("Created track of length 0");
            return new StraightTrack(game, type, aNode, endPosition);
        }

        Vector3f direction = new Vector3f(aDirection).normalize();
        Vector2f vecToB = new Vector2f(relPosB).normalize();

        float dot = vecToB.x * direction.x + vecToB.y * direction.y;
        if ((dot * dot) > 127 / 128f) {
            Logger.DEBUG.print("Creating straight track", aNode.getPosition(), endPosition, "dot = " + dot);
            return new StraightTrack(game, type, aNode, endPosition);

        } else {
            Logger.DEBUG.print("Creating circle track", aNode.getPosition(), endPosition, "dot = " + dot);
            return new CircleTrack(game, type, aNode, direction, endPosition);
        }
    }

    /**
     * factory method for creating an arbitrary track between two nodes
     * @param game       the game instance
     * @param type       the track type
     * @param aNode      an existing node A
     * @param bNode      an existing node B
     * @param aDirection the direction D of the track in point A, pointing towards B
     * @return a track that describes a track from A to B, having direction D in point A
     * @see StraightTrack
     * @see CircleTrack
     */
    static TrackPiece getTrackPiece(
            Game game, TrackType type, NetworkNode aNode, Vector3fc aDirection, NetworkNode bNode
    ) {
        assert aDirection.lengthSquared() > 0;

        Vector3fc bPos = bNode.getPosition();
        Vector3fc aPos = aNode.getPosition();
        Vector2f relPosB = new Vector2f(bPos.x() - aPos.x(), bPos.y() - aPos.y());
        if (relPosB.lengthSquared() < 1 / 256f) {
            Logger.ASSERT.print("Created track of length 0");
            return new StraightTrack(game, type, aNode, bNode);
        }

        Vector3f direction = new Vector3f(aDirection).normalize();
        Vector2f vecToB = new Vector2f(relPosB).normalize();

        float dot = vecToB.x * direction.x + vecToB.y * direction.y;
        if ((dot * dot) > 127 / 128f) {
            Logger.DEBUG.print("Creating straight track", aPos, bPos, "dot = " + dot);
            return new StraightTrack(game, type, aNode, bNode);

        } else {
            Logger.DEBUG.print("Creating circle track", aPos, bPos, "dot = " + dot);
            return new CircleTrack(game, type, aNode, direction, bNode);
        }
    }

    /**
     * factory method for creating two track pieces connecting two directional nodes
     * @param game       the game instance
     * @param type       the track type
     * @param aNode      an existing node A
     * @param bNode      an existing node B
     * @param aDirection the direction D of the track in point A, pointing towards B
     * @param bDirection the direction E of the track in point B, pointing towards A
     * @return two track pieces: Left starts in A, with direction D. Right starts in B with direction E.
     * @see StraightTrack
     * @see CircleTrack
     */
    static Pair<TrackPiece, TrackPiece> getTrackPiece(
            Game game, TrackType type, NetworkNode aNode, Vector3fc aDirection,
            NetworkNode bNode, Vector3fc bDirection
    ) {
        Vector3fc bPos = bNode.getPosition();
        Vector3fc aPos = aNode.getPosition();

        Vector2f intersect = new Vector2f();
        Intersectionf.intersectLineLine(
                aPos.x(), aPos.y(), aPos.x() + aDirection.x(), aPos.y() + aDirection.y(),
                bPos.x(), bPos.y(), bPos.x() + bDirection.x(), bPos.y() + bDirection.y(),
                intersect
        );

        // situation: connect straight to A and circle to B
        float bDistance = intersect.distance(bPos.x(), bPos.y());
        float aDistance = intersect.distance(aPos.x(), aPos.y());

        if (aDistance > bDistance) {
            Vector3f middle = getMiddlePosition(aDirection, bDirection, aPos, bPos, aDistance, bDistance);
            return new Pair<>(
                    new StraightTrack(game, type, aNode, middle),
                    new CircleTrack(game, type, bNode, bDirection, middle)
            );

        } else {
            Vector3f middle = getMiddlePosition(bDirection, aDirection, bPos, aPos, bDistance, aDistance);
            return new Pair<>(
                    new CircleTrack(game, type, aNode, aDirection, middle),
                    new StraightTrack(game, type, bNode, middle)
            );
        }
    }

    static Vector3f getMiddlePosition(
            Vector3fc straightDir, Vector3fc circleDir, Vector3fc straightPos, Vector3fc circlePos,
            float longDist, float shortDist
    ) {
        float straightLength = longDist - shortDist;
        Vector2f straightVector = new Vector2f(straightDir.x(), straightDir.y()).normalize(straightLength);
        Vector2f circleVector = new Vector2f(circleDir.x(), circleDir.y());
        Vector2f middlePointXY = new Vector2f(straightVector).add(straightPos.x(), straightPos.y());

        // remainder is pure to calculate circle length to compute the height of the middle
        Vector2f startToCenter = new Vector2f(circleDir.y(), -circleDir.x());
        Vector2f startToEnd = new Vector2f(middlePointXY.x() - circlePos.x(), middlePointXY.y() - circlePos.y());
        float dot = startToEnd.dot(startToCenter);

        float radius = startToEnd.lengthSquared() / (2 * Math.abs(dot));
        float angle = straightVector.angle(circleVector);
        float circleLength = Math.abs(radius * angle);

        float totalLength = straightLength + circleLength;

        float hDiff = circlePos.z() - straightPos.z();
        float middlePointZ = straightPos.z() + (hDiff / totalLength) * straightLength;

        return new Vector3f(middlePointXY, middlePointZ);
    }
}
