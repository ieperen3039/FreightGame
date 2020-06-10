package NG.Tracks;

import NG.Core.Game;
import NG.Network.NetworkNode;
import NG.Network.RailNode;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import org.joml.Math;
import org.joml.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.lang.StrictMath.PI;

/**
 * a number of utility methods to create or modify rail pieces.
 * @author Geert van Ieperen created on 1-5-2020.
 */
public final class RailTools {
    private static final float STRAIGHT_MAX_ANGLE_DEG = 2f;
    private static final float STRAIGHT_DOT_LIMIT = Math.cos(Math.toRadians(STRAIGHT_MAX_ANGLE_DEG));

    private static final float MAX_CIRCLE_ANGLE_RAD = Math.toRadians(90);
    private static final float MINIMUM_CONNECT_ANGLE = 1 / 64f;
    private static final float MINIMUM_TRACK_LENGTH = 1 / 4f;

    /**
     * creates two nodes that are only connected together with a straight piece of track.
     * @param game           the current game instance
     * @param type           the type of track
     * @param aPosition      one position of the map
     * @param bPosition      another position on the map
     * @param signalDistance distance between signals, or Float.POSITIVE_INFINITY if no signals should be placed
     * @return a {@link StraightTrack} piece that connects the
     */
    public static List<TrackPiece> createNew(
            Game game, TrackType type, Vector3fc aPosition, Vector3fc bPosition, float signalDistance
    ) {
        Vector3f sub = new Vector3f(bPosition).sub(aPosition);
        if (sub.length() < MINIMUM_TRACK_LENGTH) return Collections.emptyList();

        return getStraightPieces(game, new RailNode(aPosition, type, sub), bPosition, null, signalDistance, 0);
    }

    /**
     * connect a NetworkNode to a new node created at the given position
     * @param game           the game instance
     * @param node           the node to connect
     * @param newPosition    the position of the new node
     * @param signalDistance distance between signals, or Float.POSITIVE_INFINITY if no signals should be placed
     * @return the new node
     */
    public static List<TrackPiece> createNew(Game game, RailNode node, Vector3fc newPosition, float signalDistance) {
        Vector3fc nodePosition = node.getPosition();
        Vector3f toNew = new Vector3f(newPosition).sub(nodePosition);

        float length = toNew.length();
        if (length < MINIMUM_TRACK_LENGTH) return Collections.emptyList();
        toNew.div(length); // normalize toNew

        NetworkNode networkNode = node.getNetworkNode();
        Vector3fc nodeDirection = networkNode.isEnd() ? node.getOpenDirection() : node.getDirectionTo(newPosition);
        Vector3f nodeDirNorm = new Vector3f(nodeDirection).normalize();
        float dot = toNew.x * nodeDirNorm.x() + toNew.y * nodeDirNorm.y();

        float offset = getOffset(node, nodeDirection, signalDistance);
        // distanceToSignal > 0 hence 0 <= offset <= signalDistance

        if (Math.abs(dot) > STRAIGHT_DOT_LIMIT) {
            Vector2f flatDir = new Vector2f(nodeDirection.x(), nodeDirection.y()).normalize(length);
            Vector3fc modifiedEnd = new Vector3f(
                    nodePosition.x() + flatDir.x, nodePosition.y() + flatDir.y, newPosition.z()
            );

            return getStraightPieces(game, node, modifiedEnd, null, signalDistance, offset);
        } else {
            return getCirclePieces(game, node, nodeDirection, newPosition, null, signalDistance, offset);
        }
    }

    /**
     * @param node        node A
     * @param endNode     node B, may be null
     * @param endPosition position of node B
     * @param spacing     maximum length of the generated track pieces
     * @param offset      minimum length of the first track
     * @return an ordered list of tracks from A to B, where each track is at most {@code spacing} {@link
     * TrackPiece#getLength() length}. If {@code endNode} is not null, the last track has it as {@link
     * TrackPiece#getEndNode() endNode}.
     */
    private static List<TrackPiece> getStraightPieces(
            Game game, RailNode node, Vector3fc endPosition, RailNode endNode, float spacing, float offset
    ) {
        assert offset >= 0;
        assert offset <= spacing; // spacing >= 0
        if (offset == 0) {
            node.addSignal(game);
            offset = spacing; // prevent zero-length tracks
        }
        List<TrackPiece> tracks = new ArrayList<>();

        Vector3fc initialNodePos = node.getPosition();
        Vector3f AToB = new Vector3f(endPosition).sub(initialNodePos);
        float totalLength = AToB.length();
        assert totalLength > 0;

        if (offset < totalLength) {
            AToB.div(totalLength);

            // iterations except the last
            int maxItr = (int) (((totalLength - offset) / spacing) - MINIMUM_TRACK_LENGTH);
            for (int i = 0; i <= maxItr; i++) {
                float distance = offset + i * spacing;

                Vector3f localEndPos = new Vector3f(AToB).mul(distance).add(initialNodePos);
                TrackPiece trackConnection = new StraightTrack(game, node.getType(), node, localEndPos, true);
                node = trackConnection.getEndNode();
                node.addSignal(game);

                tracks.add(trackConnection);
            }
        }

        if (endNode == null) {
            tracks.add(new StraightTrack(game, node.getType(), node, endPosition, true));
        } else {
            tracks.add(new StraightTrack(game, node.getType(), node, endNode, true));
        }

        return tracks;

    }

    /**
     * @param node          node A
     * @param nodeDirection the direction of tracks in node A
     * @param endNode       node B, may be null
     * @param endPosition   position of node B
     * @param spacing       maximum length of the generated track pieces
     * @param offset        minimum length of the first track
     * @return an ordered list of tracks from A to B, where each track has the same radius and is at most {@code
     * spacing} {@link TrackPiece#getLength() length}. If {@code endNode} is not null, the last track has it as {@link
     * TrackPiece#getEndNode() endNode}.
     */
    private static List<TrackPiece> getCirclePieces(
            Game game, RailNode node, Vector3fc nodeDirection, Vector3fc endPosition, RailNode endNode,
            float spacing, float offset
    ) {
        assert offset >= 0;
        assert offset <= spacing;
        if (offset == 0) {
            if (Float.isFinite(spacing)) {
                node.addSignal(game);
            }
            offset = spacing; // prevent zero-length tracks
        }
        List<TrackPiece> tracks = new ArrayList<>();

        Vector3fc nodePosition = node.getPosition();
        CircleTrack.Description circle = CircleTrack.getCircleDescription(nodePosition, nodeDirection, endPosition);

        float baseHeight = nodePosition.z();
        float heightDiff = endPosition.z() - baseHeight;

        Vector2f startToEnd = new Vector2f(endPosition.x() - nodePosition.x(), endPosition.y() - nodePosition.y());
        float dotOfCross = nodeDirection.x() * startToEnd.y - nodeDirection.y() * startToEnd.x;
        boolean isClockwise = dotOfCross < 0;

        Vector3f direction = new Vector3f(nodeDirection);

        int signalDistance = 1;
        float sectionAngle = spacing / circle.radius;
        if (sectionAngle >= (float) PI * 2f) {
            sectionAngle = MAX_CIRCLE_ANGLE_RAD;
            signalDistance = 0;
        }

        // sectionAngle is limited by MAX_CIRCLE_ANGLE_RAD, so we find a common divisor
        while (sectionAngle > MAX_CIRCLE_ANGLE_RAD) {
            sectionAngle /= 2;
            signalDistance *= 2;
        }

        int offsetIndex = 0;
        float offsetAngle = offset / circle.radius;
        if (offsetAngle >= (float) PI * 2f) {
            offsetAngle = sectionAngle;

        } else {
            // offset may not be larger than a section
            while (offsetAngle > sectionAngle + MINIMUM_CONNECT_ANGLE) {
                offsetIndex++;
                offsetAngle -= sectionAngle;
            }
        }

        if (offsetAngle < circle.angle) {
            Vector2fc vecToStart = new Vector2f(nodePosition.x(), nodePosition.y()).sub(circle.center);
            float arcTan = Vectors.arcTan(vecToStart);
            if (arcTan < 0) arcTan += 2 * Math.PI;
            float startTheta = arcTan;

            float dz = heightDiff / (circle.radius * circle.angle);

            int maxItr = (int) (((circle.angle - offsetAngle) / sectionAngle));
            float nextAngle = startTheta;

            for (int i = 0; i <= maxItr; i++) {
                float currentAngle = nextAngle;
                float dx = -Math.sin(currentAngle);
                float dy = Math.cos(currentAngle);
                if (isClockwise) {
                    direction.set(-dx, -dy, dz);
                } else {
                    direction.set(dx, dy, dz);
                }

                float nextAngleOffset = offsetAngle + i * sectionAngle;
                nextAngle = isClockwise ? startTheta - nextAngleOffset : startTheta + nextAngleOffset;

                float vx = Math.cos(nextAngle) * circle.radius;
                float vy = Math.sin(nextAngle) * circle.radius;
                float vz = Math.abs(nextAngleOffset / circle.angle) * heightDiff;

                Vector3f localEndPos = new Vector3f(circle.center, baseHeight).add(vx, vy, vz);
                // rather than computing how MINIMUM_TRACK_LENGTH translates to iterations, we do it this way
                if (localEndPos.distance(endPosition) < MINIMUM_TRACK_LENGTH) {
                    nextAngle = currentAngle; // revert
                    break;
                }

                TrackPiece trackConnection = new CircleTrack(game, node.getType(), node, direction, localEndPos);
                node = trackConnection.getEndNode();

                if (signalDistance > 0 && i % signalDistance == offsetIndex) {
                    node.addSignal(game);
                }

                tracks.add(trackConnection);
            }

            float dx = -Math.sin(nextAngle);
            float dy = Math.cos(nextAngle);
            if (isClockwise) {
                direction.set(-dx, -dy, dz);
            } else {
                direction.set(dx, dy, dz);
            }
        }

        if (node.getPosition().distance(endPosition) < 1 / 128f) {
            Logger.ERROR.print("Circle edge case", startToEnd, circle, sectionAngle);
            if (!tracks.isEmpty()) {
                TrackPiece t = tracks.remove(tracks.size() - 1);
                node = t.getStartNode();
            }
        }

        if (endNode != null) {
            tracks.add(new CircleTrack(game, node.getType(), node, direction, endNode));
        } else {
            tracks.add(new CircleTrack(game, node.getType(), node, direction, endPosition));
        }

        return tracks;
    }

    private static float getOffset(RailNode aNode, Vector3fc aDirection, float signalDistance) {
        if (Float.isInfinite(signalDistance)) return Float.POSITIVE_INFINITY;

        Vector3f antiDirection = new Vector3f(aDirection).negate();
        List<NetworkNode.Direction> predecessors = aNode.getEntriesFromDirection(antiDirection);
        float distanceToSignal = getShortestDistanceToSignal(aNode, predecessors);
        return Math.max(signalDistance - distanceToSignal, 0);
    }

    /**
     * @param list one of the lists of the networknode of {@code node}
     * @return the shortest distance from {@code node} to any signal in {@code list}. If no signal is found, return
     * {@link Float#POSITIVE_INFINITY}
     */
    private static float getShortestDistanceToSignal(RailNode node, List<NetworkNode.Direction> list) {
        float shortest = Float.POSITIVE_INFINITY;
        for (NetworkNode.Direction entry : list) {
            TrackPiece trackPiece = entry.trackPiece;

            RailNode other = trackPiece.getNot(node);
            float length = trackPiece.getLength();

            if (!other.hasSignal()) {
                List<NetworkNode.Direction> next = other.getNetworkNode().getNext(trackPiece);
                length += getShortestDistanceToSignal(other, next);
            }

            if (length < shortest) {
                shortest = length;
            }
        }

        return shortest;
    }

    /**
     * splits the given track into two tracks on the given fraction. this trackpiece is despawned as if calling {@link
     * TrackPiece#despawn(double) trackPiece.despawn(gameTime)}. When appropriate, this method may decide not to create
     * a new node, but return an existing node.
     * @param trackPiece the trackpiece to split
     * @param fraction   the fraction of track where the split happens, such that the new node is created at {@link
     *                   TrackPiece#getPositionFromFraction(float) trackPiece.getPositionFromFraction(fraction)}
     * @param gameTime   the game time where the old track is removed and the new tracks are added to the game.
     * @return the newly created node, connecting both new tracks together. For this node trivially holds {@link
     * NetworkNode#isStraight()}
     */
    public static RailNode createSplit(Game game, TrackPiece trackPiece, float fraction, double gameTime) {
        RailNode aNode = trackPiece.getStartNode();
        RailNode bNode = trackPiece.getEndNode();

        // if the new node is almost on the same position of an existing node, return that existing node.
        float trackLength = trackPiece.getLength();
        if (trackLength * fraction < MINIMUM_TRACK_LENGTH) {
            return aNode;
        }
        if (trackLength * (1 - fraction) < MINIMUM_TRACK_LENGTH) {
            return bNode;
        }

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

        invalidateSignals(aConnection, aNode, new ArrayList<>(0));
        invalidateSignals(bConnection, bNode, new ArrayList<>(0));

        assert aConnection.isValid() : aConnection;
        assert bConnection.isValid() : bConnection;

        trackPiece.despawn(gameTime);
        return newNode;
    }

    /**
     * removes the given track piece from the game. The trackpiece is despawned at gameTime, and any remaining
     * connectionless nodes are cleaned up.
     * @param trackPiece the track to remove
     * @param gameTime   the moment where the trackpiece should be removed
     */
    public static void removeTrackPiece(TrackPiece trackPiece, double gameTime) {
        assert trackPiece.isValid();
        RailNode aNode = trackPiece.getStartNode();
        RailNode bNode = trackPiece.getEndNode();
        invalidateSignals(trackPiece); // first invalidate all connected signals

        TrackPiece oldPiece = NetworkNode.removeConnection(aNode.getNetworkNode(), bNode.getNetworkNode());
        assert oldPiece == trackPiece :
                "Nodes were connected with double tracks: " + oldPiece + " and " + trackPiece;

        trackPiece.despawn(gameTime);

        if (aNode.isUnconnected() && aNode.hasSignal()) {
            aNode.getSignal().despawn(gameTime);
        }

        if (bNode.isUnconnected() && bNode.hasSignal()) {
            bNode.getSignal().despawn(gameTime);
        }
    }

    /**
     * factory method for creating two track pieces connecting two directional nodes
     * @param game           the game instance
     * @param aNode          an existing node A
     * @param bNode          an existing node B
     * @param signalDistance The distance from A to the nearest signal in the direction away from B, or
     *                       Float.POSITIVE_INFINITY of no signals should be made.
     * @return two lists: Left starts in A, right ends in B.
     * @see StraightTrack
     * @see CircleTrack
     */
    public static List<TrackPiece> createConnection(
            Game game, RailNode aNode, RailNode bNode, float signalDistance
    ) {
        assert aNode.getType() == bNode.getType();
        assert signalDistance >= 0;

        Vector3fc bPos = bNode.getPosition();
        Vector3fc aPos = aNode.getPosition();
        Vector3fc aDirection = aNode.getDirectionTo(bPos);
        Vector3fc bDirection = bNode.getDirectionTo(aPos);

        if (aDirection.angle(bDirection) < MINIMUM_CONNECT_ANGLE) {
            Vector3f aToB = new Vector3f(bPos).sub(aPos);
            Vector3f middle = new Vector3f(aToB).mul(0.5f).add(aPos);
            float offset = getOffset(aNode, aDirection, signalDistance);

            if (aToB.normalize().dot(aDirection) > (1 - MINIMUM_CONNECT_ANGLE)) {
                return getStraightPieces(game, aNode, bPos, bNode, signalDistance, offset);

            } else {
                Vector3f middleDirection = new Vector3f(aDirection).reflect(aToB); // quick math
                RailNode middleNode = new RailNode(middle, aNode.getType(), middleDirection);
                List<TrackPiece> circlePieces = getCirclePieces(game, aNode, aDirection, middle, middleNode, signalDistance, offset);

                float secondOffset = getOffset(circlePieces, offset);
                List<TrackPiece> newPieces = getCirclePieces(game, middleNode, middleDirection, bPos, bNode, signalDistance, secondOffset);

                circlePieces.addAll(newPieces);
                return circlePieces;
            }
        }

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

        // get initial offset
        float offset = getOffset(aNode, aDirection, signalDistance);

        if (!doesIntersect || Math.abs(aDistance - bDistance) < MINIMUM_TRACK_LENGTH) {
            return getCirclePieces(game, aNode, aDirection, bPos, bNode, signalDistance, offset);

        } else if (aDistance > bDistance) {
            // situation: connect straight to A and circle to B
            Vector3f middle = getMiddlePosition(aDirection, bDirection, aPos, bPos, aDistance, bDistance);
            RailNode middleNode = new RailNode(middle, aNode.getType(), aDirection);

            List<TrackPiece> straightPieces = getStraightPieces(game, aNode, middle, middleNode, signalDistance, offset);

            float secondOffset = getOffset(straightPieces, offset);
            assert (secondOffset <= signalDistance) : straightPieces;
            List<TrackPiece> circlePieces = getCirclePieces(
                    game, middleNode, aDirection, bPos, bNode, signalDistance, secondOffset
            );

            straightPieces.addAll(circlePieces);
            return straightPieces;

        } else {
            Vector3f middle = getMiddlePosition(bDirection, aDirection, bPos, aPos, bDistance, aDistance);
            RailNode middleNode = new RailNode(middle, aNode.getType(), bDirection);

            List<TrackPiece> circlePieces = getCirclePieces(game, aNode, aDirection, middle, middleNode, signalDistance, offset);

            float secondOffset = getOffset(circlePieces, offset);
            assert (secondOffset <= signalDistance) : circlePieces;

            List<TrackPiece> straightPieces = getStraightPieces(
                    game, middleNode, bPos, bNode, signalDistance, secondOffset
            );

            circlePieces.addAll(straightPieces);
            return circlePieces;
        }
    }

    /**
     * computes the signal offset from of the given list of tracks. These tracks are assumed to be directed
     * (get(i).endNode() == get(i+1).startNode()). If no signals are found, returns the total length of the tracks +
     * offset
     * @return the required signal offset for any next connection
     */
    private static float getOffset(List<TrackPiece> tracks, float offset) {
        if (Float.isInfinite(offset)) return Float.POSITIVE_INFINITY;
        float newOffset = 0;
        int i = tracks.size() - 1;
        while (i-- > 0) {
            TrackPiece track = tracks.get(i);
            if (track.getEndNode().hasSignal()) {
                return newOffset;
            }
            newOffset += track.getLength();
        }
        return newOffset + offset;
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
                new Vector2f(circlePos.x(), circlePos.y()), new Vector2f(circleDir.x(), circleDir.y()),
                middlePoint
        );

        float circleLength = Math.abs(circle.radius * circle.angle);

        float hDiff = circlePos.z() - straightPos.z();
        float totalLength = straightLength + circleLength;
        float middleHeight = straightPos.z() + hDiff * (straightLength / totalLength);

        return new Vector3f(middlePoint, middleHeight);
    }

    /** invalidates the signal connections of all signals remotely connected to track */
    public static void invalidateSignals(TrackPiece track) {
        // two different directions should have two different lists
        invalidateSignals(track, track.getStartNode(), new ArrayList<>(0));
        invalidateSignals(track, track.getEndNode(), new ArrayList<>(0));
    }

    /** invalidates the signal connections of all signals in direction of node */
    private static void invalidateSignals(TrackPiece track, RailNode node, Collection<RailNode> seen) {
        // handle loops without signals
        if (node.getNetworkNode().isNetworkCritical()) {
            if (seen.contains(node)) return;
            seen.add(node);
        }

        if (node.hasSignal()) {
            return;
        }

        // recursively search for signals
        List<NetworkNode.Direction> next = node.getNetworkNode().getNext(track);
        for (NetworkNode.Direction entry : next) {
            TrackPiece nextTrack = entry.trackPiece;
            RailNode nextNode = nextTrack.getNot(node);

            invalidateSignals(nextTrack, nextNode, seen);
        }
    }
}
