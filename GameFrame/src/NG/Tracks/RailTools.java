package NG.Tracks;

import NG.Core.Game;
import NG.DataStructures.Generic.Pair;
import NG.Network.RailNode;
import NG.Tools.Logger;
import org.joml.Math;
import org.joml.*;

/**
 * a number of utility methods to create or modify rail pieces
 * @author Geert van Ieperen created on 1-5-2020.
 */
public final class RailTools {
    private static final float STRAIGHT_MAX_ANGLE_DEG = 1f;
    private static final float STRAIGHT_DOT_LIMIT = Math.cos(Math.toRadians(STRAIGHT_MAX_ANGLE_DEG));

    /**
     * creates two nodes that are only connected together with a straight piece of track.
     * @param game      the current game instance
     * @param type      the type of track
     * @param aPosition one position of the map
     * @param bPosition another position on the map
     * @return a {@link StraightTrack} piece that connects the
     */
    public static TrackPiece createNew(Game game, TrackType type, Vector3fc aPosition, Vector3fc bPosition) {
        Vector3f AToB = new Vector3f(bPosition).sub(aPosition);
        RailNode A = new RailNode(aPosition, type, AToB);

        TrackPiece trackConnection = new StraightTrack(game, type, A, bPosition, true);
        RailNode.addConnection(trackConnection);
        game.state().addEntity(trackConnection);

        assert trackConnection.isValid() : trackConnection;

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
        TrackPiece trackConnection = getTrackPiece(
                game, node.getType(), node, node.getDirectionTo(newPosition), newPosition
        );
        RailNode.addConnection(trackConnection);
        game.state().addEntity(trackConnection);

        assert trackConnection.isValid() : trackConnection;

        return trackConnection.getNot(node);
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
        Pair<TrackPiece, TrackPiece> trackPieces = getTrackPiece(
                game, aNode.getType(), aNode, bNode
        );

        RailNode.addConnection(trackPieces.left);
        game.state().addEntity(trackPieces.left);
        assert trackPieces.left.isValid() : trackPieces.left;

        if (trackPieces.right != null) {
            RailNode.addConnection(trackPieces.right);
            game.state().addEntity(trackPieces.right);
            assert trackPieces.right.isValid() : trackPieces.right;
        }

        return trackPieces.left.getNot(aNode);
    }

    public static RailNode createSplit(Game game, TrackPiece trackPiece, float fraction) {
        RailNode aNode = trackPiece.getStartNode();
        RailNode bNode = trackPiece.getEndNode();

        TrackType type = trackPiece.getType();
        Vector3fc aStartDir = aNode.getDirectionTo(bNode);

        Vector3f point = trackPiece.getPositionFromFraction(fraction);
        Vector3f direction = trackPiece.getDirectionFromFraction(fraction);
        RailNode newNode = new RailNode(point, type, direction);

        TrackPiece aConnection = getTrackPiece(
                game, type, aNode, aStartDir, newNode
        );
        // DONT RailNode.addConnection(trackConnection);
        game.state().addEntity(aConnection);

        TrackPiece bConnection = getTrackPiece(
                game, type, newNode, direction, bNode
        );
        game.state().addEntity(bConnection);

        // this replaces the addConnection
        RailNode.insertNode(aNode, bNode, newNode, aConnection, bConnection);

        assert aConnection.isValid() : aConnection;
        assert bConnection.isValid() : bConnection;

        trackPiece.dispose();
        return newNode;
    }

    public static void removeTrackPiece(TrackPiece trackPiece) {
        assert trackPiece.isValid();
        RailNode aNode = trackPiece.getStartNode();
        RailNode bNode = trackPiece.getEndNode();

        TrackPiece oldPiece = RailNode.removeConnection(aNode, bNode);
        assert oldPiece == trackPiece :
                "Nodes were connected with double tracks: " + oldPiece + " and " + trackPiece;

        trackPiece.dispose();
    }

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
    public static TrackPiece getTrackPiece(
            Game game, TrackType type, RailNode aNode, Vector3fc aDirection, Vector3fc endPosition
    ) {
        Vector3fc aPos = aNode.getPosition();
        Vector2f relPosB = new Vector2f(endPosition.x() - aPos.x(), endPosition.y() - aPos.y());
        if (relPosB.lengthSquared() < 1 / 256f) {
            Logger.ASSERT.print("Created track of length 0");
            return new StraightTrack(game, type, aNode, endPosition, true);
        }

        Vector3f direction = new Vector3f(aDirection).normalize();
        Vector2f vecToB = new Vector2f(relPosB).normalize();

        float dot = vecToB.x * direction.x + vecToB.y * direction.y;
        if (Math.abs(dot) > STRAIGHT_DOT_LIMIT) {
            return new StraightTrack(game, type, aNode, endPosition, true);

        } else {
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
    public static TrackPiece getTrackPiece(
            Game game, TrackType type, RailNode aNode, Vector3fc aDirection, RailNode bNode
    ) {
        assert aDirection.lengthSquared() > 0;

        Vector3fc bPos = bNode.getPosition();
        Vector3fc aPos = aNode.getPosition();
        Vector2f relPosB = new Vector2f(bPos.x() - aPos.x(), bPos.y() - aPos.y());
        if (relPosB.lengthSquared() < 1 / 256f) {
            Logger.ASSERT.print("Created track of length 0");
            return new StraightTrack(game, type, aNode, bNode, true);
        }

        Vector3f direction = new Vector3f(aDirection).normalize();
        Vector2f vecToB = new Vector2f(relPosB).normalize();

        float dot = vecToB.x * direction.x + vecToB.y * direction.y;
        if ((dot * dot) > 127 / 128f) {
            Logger.DEBUG.print("Creating straight track", aPos, bPos, "dot = " + dot);
            return new StraightTrack(game, type, aNode, bNode, true);

        } else {
            Logger.DEBUG.print("Creating circle track", aPos, bPos, "dot = " + dot);
            return new CircleTrack(game, type, aNode, direction, bNode);
        }
    }

    /**
     * factory method for creating two track pieces connecting two directional nodes
     * @param game  the game instance
     * @param type  the track type
     * @param aNode an existing node A
     * @param bNode an existing node B
     * @return two track pieces: Left starts in A, right starts in B. If one piece can connect A and B, then left is
     * that track and B is null.
     * @see StraightTrack
     * @see CircleTrack
     */
    public static Pair<TrackPiece, TrackPiece> getTrackPiece(
            Game game, TrackType type, RailNode aNode, RailNode bNode
    ) {
        Vector3fc bPos = bNode.getPosition();
        Vector3fc aPos = aNode.getPosition();
        Vector3fc aDirection = aNode.getDirectionTo(bPos);
        Vector3fc bDirection = bNode.getDirectionTo(aPos);

        Vector2f intersect = new Vector2f();
        boolean doesIntersect = Intersectionf.intersectLineLine(
                aPos.x(), aPos.y(), aPos.x() + aDirection.x(), aPos.y() + aDirection.y(),
                bPos.x(), bPos.y(), bPos.x() + bDirection.x(), bPos.y() + bDirection.y(),
                intersect
        );

        // (intersect - aPos) dot aDirection
        float aDot = (intersect.x - aPos.x()) * aDirection.x() + (intersect.y - aPos.y()) * aDirection.y();
        // make aDirection point towards the intersection
        if (aDot < 0) aDirection = new Vector3f(aDirection).negate();

        // (intersect - bPos) dot bDirection
        float bDot = (intersect.x - bPos.x()) * bDirection.x() + (intersect.y - bPos.y()) * bDirection.y();
        // make bDirection point towards the intersection
        if (bDot < 0) bDirection = new Vector3f(bDirection).negate();

        float bDistance = intersect.distance(bPos.x(), bPos.y());
        float aDistance = intersect.distance(aPos.x(), aPos.y());

        if (!doesIntersect || aDistance == bDistance) {
            return new Pair<>(
                    new CircleTrack(game, type, aNode, aNode.getDirectionTo(bPos), bNode),
                    null
            );
        } else if (aDistance > bDistance) {
            // situation: connect straight to A and circle to B
            Vector3f middle = getMiddlePosition(aDirection, bDirection, aPos, bPos, aDistance, bDistance);
            return new Pair<>(
                    new StraightTrack(game, type, aNode, middle, true),
                    new CircleTrack(game, type, bNode, bDirection, middle)
            );

        } else {
            Vector3f middle = getMiddlePosition(bDirection, aDirection, bPos, aPos, bDistance, aDistance);
            return new Pair<>(
                    new CircleTrack(game, type, aNode, aDirection, middle),
                    new StraightTrack(game, type, bNode, middle, true)
            );
        }
    }

    private static Vector3f getMiddlePosition(
            Vector3fc straightDir, Vector3fc circleDir, Vector3fc straightPos, Vector3fc circlePos,
            float longDist, float shortDist
    ) {
        float straightLength = longDist - shortDist;
        Vector2f straightVector = new Vector2f(straightDir.x(), straightDir.y()).normalize(straightLength);
        Vector2f circleVector = new Vector2f(circleDir.x(), circleDir.y());
        Vector2f middlePointXY = new Vector2f(straightVector).add(straightPos.x(), straightPos.y());

        // remainder is purely to calculate circle length to compute the height of the middle
        Vector2f startToCenter = new Vector2f(circleDir.y(), -circleDir.x());
        Vector2f startToEnd = new Vector2f(middlePointXY.x() - circlePos.x(), middlePointXY.y() - circlePos.y());
        float dot = startToEnd.dot(startToCenter);

        float radius = startToEnd.lengthSquared() / (2 * org.joml.Math.abs(dot));
        float angle = straightVector.angle(circleVector);
        float circleLength = Math.abs(radius * angle);

        float totalLength = straightLength + circleLength;

        float hDiff = circlePos.z() - straightPos.z();
        float middlePointZ = straightPos.z() + (hDiff / totalLength) * straightLength;

        return new Vector3f(middlePointXY, middlePointZ);
    }
}
