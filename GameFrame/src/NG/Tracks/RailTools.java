package NG.Tracks;

import NG.Core.Game;
import NG.DataStructures.Generic.Pair;
import NG.Network.NetworkNode;
import NG.Network.RailNode;
import NG.Tools.Vectors;
import org.joml.Math;
import org.joml.*;

import java.util.ArrayList;
import java.util.List;

/**
 * a number of utility methods to create or modify rail pieces
 * @author Geert van Ieperen created on 1-5-2020.
 */
public final class RailTools {
    private static final float STRAIGHT_MAX_ANGLE_DEG = 1f;
    private static final float STRAIGHT_DOT_LIMIT = Math.cos(Math.toRadians(STRAIGHT_MAX_ANGLE_DEG));

    private static final float MAX_TRACK_LENGTH = 10f;
    private static final float MAX_CIRCLE_ANGLE_RAD = Math.toRadians(90);

    /**
     * creates two nodes that are only connected together with a straight piece of track.
     * @param game      the current game instance
     * @param type      the type of track
     * @param aPosition one position of the map
     * @param bPosition another position on the map
     * @return a {@link StraightTrack} piece that connects the
     */
    public static List<TrackPiece> createNew(Game game, TrackType type, Vector3fc aPosition, Vector3fc bPosition) {
        Vector3f sub = new Vector3f(bPosition).sub(aPosition);
        return getStraightPieces(game, new RailNode(aPosition, type, sub), bPosition);
    }

    /**
     * connect a NetworkNode to a new node created at the given position
     * @param game        the game instance
     * @param node        the node to connect
     * @param newPosition the position of the new node
     * @return the new node
     */
    public static List<TrackPiece> createNew(Game game, RailNode node, Vector3fc newPosition) {
        Vector3fc nodePosition = node.getPosition();
        Vector3f toNew = new Vector3f(newPosition).sub(nodePosition).normalize();

        NetworkNode firstNetwork = node.getNetworkNode();
        Vector3fc nodeDirection = firstNetwork.isEnd() ? node.getOpenDirection() : node.getDirectionTo(newPosition);
        Vector3fc nodeDirNorm = new Vector3f(nodeDirection).normalize();
        float dot = toNew.x * nodeDirNorm.x() + toNew.y * nodeDirNorm.y();

        if (Math.abs(dot) > STRAIGHT_DOT_LIMIT) {
            return getStraightPieces(game, node, newPosition);
        } else {
            return getCirclePieces(game, node, nodeDirection, newPosition);
        }
    }

    private static List<TrackPiece> getStraightPieces(Game game, RailNode node, Vector3fc bPosition) {
        Vector3f AToB = new Vector3f(bPosition).sub(node.getPosition());
        float totalLength = AToB.length();
        assert totalLength > 0;
        List<TrackPiece> tracks = new ArrayList<>();
        Vector3f localStartPos = new Vector3f(node.getPosition());
        AToB.normalize(MAX_TRACK_LENGTH);

        int maxItr = (int) (totalLength / MAX_TRACK_LENGTH);
        for (int i = 0; i <= maxItr; i++) {
            if (i == maxItr) {
                AToB.normalize(totalLength - i * MAX_TRACK_LENGTH);
            }

            TrackPiece trackConnection = new StraightTrack(game, node.getType(), node, localStartPos.add(AToB), true);
            node = trackConnection.getEndNode();
            tracks.add(trackConnection);
        }
        return tracks;
    }

    private static List<TrackPiece> getCirclePieces(
            Game game, RailNode node, Vector3fc nodeDirection, Vector3fc position
    ) {
        Vector3fc nodePosition = node.getPosition();
        Vector3f direction = new Vector3f(nodeDirection);
        float baseHeight = nodePosition.z();
        float heightDiff = baseHeight - position.z();

        CircleTrack.Description circle = CircleTrack.getCircleDescription(direction, nodePosition, position);
        float sectionAngle = Math.min(MAX_TRACK_LENGTH / circle.radius, MAX_CIRCLE_ANGLE_RAD);
        Vector2fc vecToStart = new Vector2f(nodePosition.x(), nodePosition.y()).sub(circle.center);
        float arcTan = Vectors.arcTan(vecToStart);
        if (arcTan < 0) arcTan += 2 * Math.PI;
        float startTheta = arcTan;

        Vector2f startToEnd = new Vector2f(position.x() - nodePosition.x(), position.y() - nodePosition.y());
        float dotOfCross = direction.x() * startToEnd.y - direction.y() * startToEnd.x;
        boolean isClockwise = dotOfCross < 0;

        List<TrackPiece> tracks = new ArrayList<>();

        int maxItr = (int) (circle.angle / sectionAngle) + 1;

        float nextAngle = startTheta;

        for (int i = 1; i <= maxItr; i++) {
            float currentAngle = nextAngle;
            float nextAngleDelta = (i == maxItr) ? circle.angle : i * sectionAngle;
            nextAngle = isClockwise ? startTheta - nextAngleDelta : startTheta + nextAngleDelta;

            float vx = Math.cos(nextAngle) * circle.radius;
            float vy = Math.sin(nextAngle) * circle.radius;
            float vz = (nextAngleDelta / circle.angle) * heightDiff;

            Vector3f localEndPos = new Vector3f(circle.center, baseHeight).add(vx, vy, vz);

            float dx = -Math.sin(currentAngle);
            float dy = Math.cos(currentAngle);
            float dz = heightDiff / (circle.radius * circle.angle);
            if (isClockwise) {
                direction.set(-dx, -dy, dz);
            } else {
                direction.set(dx, dy, dz);
            }

            TrackPiece trackConnection = new CircleTrack(game, node.getType(), node, direction, localEndPos);
            node = trackConnection.getEndNode();

            tracks.add(trackConnection);
        }
        return tracks;
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

        NetworkNode.addConnection(trackPieces.left);
        game.state().addEntity(trackPieces.left);
        assert trackPieces.left.isValid() : trackPieces.left;

        if (trackPieces.right != null) {
            NetworkNode.addConnection(trackPieces.right);
            game.state().addEntity(trackPieces.right);
            assert trackPieces.right.isValid() : trackPieces.right;
        }

        return trackPieces.left.getNot(aNode);
    }

    public static RailNode createSplit(Game game, TrackPiece trackPiece, float fraction, double gameTime) {
        RailNode aNode = trackPiece.getStartNode();
        RailNode bNode = trackPiece.getEndNode();

        TrackType type = trackPiece.getType();
        Vector3f point = trackPiece.getPositionFromFraction(fraction);
        Vector3f direction = trackPiece.getDirectionFromFraction(fraction);
        RailNode newNode = new RailNode(point, type, direction);

        TrackPiece aConnection;
        TrackPiece bConnection;

        if (trackPiece instanceof StraightTrack) {
            aConnection = new StraightTrack(game, type, aNode, newNode, true);
            bConnection = new StraightTrack(game, type, newNode, bNode, true);

        } else {
            Vector3fc aStartDir = trackPiece.getDirectionFromFraction(0);
            Vector3f aDirNorm = new Vector3f(aStartDir).normalize();
            Vector3f newDirNorm = new Vector3f(direction).normalize();

            aConnection = new CircleTrack(game, type, aNode, aDirNorm, newNode);
            bConnection = new CircleTrack(game, type, newNode, newDirNorm, bNode);
        }

        // DONT RailNode.addConnection(trackConnection);
        game.state().addEntity(aConnection);
        game.state().addEntity(bConnection);

        // this replaces the addConnection
        NetworkNode.insertNode(
                aNode.getNetworkNode(), bNode.getNetworkNode(), newNode.getNetworkNode(),
                aConnection, bConnection
        );

        assert aConnection.isValid() : aConnection;
        assert bConnection.isValid() : bConnection;

        trackPiece.despawn(gameTime);
        return newNode;
    }

    public static void removeTrackPiece(TrackPiece trackPiece, double gameTime) {
        assert trackPiece.isValid();
        RailNode aNode = trackPiece.getStartNode();
        RailNode bNode = trackPiece.getEndNode();

        TrackPiece oldPiece = NetworkNode.removeConnection(aNode.getNetworkNode(), bNode.getNetworkNode());
        assert oldPiece == trackPiece :
                "Nodes were connected with double tracks: " + oldPiece + " and " + trackPiece;

        trackPiece.despawn(gameTime);
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
            StraightTrack straightTrack = new StraightTrack(game, type, aNode, middle, true);
            CircleTrack circleTrack = new CircleTrack(game, type, bNode, bDirection, straightTrack.getNot(aNode));
            return new Pair<>(straightTrack, circleTrack);

        } else {
            Vector3f middle = getMiddlePosition(bDirection, aDirection, bPos, aPos, bDistance, aDistance);
            StraightTrack straightTrack = new StraightTrack(game, type, bNode, middle, true);
            CircleTrack circleTrack = new CircleTrack(game, type, aNode, aDirection, straightTrack.getNot(bNode));
            return new Pair<>(circleTrack, straightTrack);
        }
    }

    private static Vector3f getMiddlePosition(
            Vector3fc straightDir, Vector3fc circleDir, Vector3fc straightPos, Vector3fc circlePos,
            float longDist, float shortDist
    ) {
        float straightLength = longDist - shortDist;
        Vector2f straightVector = new Vector2f(straightDir.x(), straightDir.y()).normalize(straightLength);
        Vector2f middlePoint = new Vector2f(straightVector).add(straightPos.x(), straightPos.y());

        // remainder is purely to calculate circle length to compute the height of the middle
        CircleTrack.Description circle = CircleTrack.getCircleDescription(
                new Vector2f(circleDir.x(), circleDir.y()),
                new Vector2f(circlePos.x(), circlePos.y()),
                middlePoint
        );

        float circleLength = Math.abs(circle.radius * circle.angle);

        float totalLength = straightLength + circleLength;

        float hDiff = circlePos.z() - straightPos.z();
        float middleA = straightPos.z() + (hDiff / totalLength) * straightLength;

        return new Vector3f(middlePoint, middleA);
    }

}
