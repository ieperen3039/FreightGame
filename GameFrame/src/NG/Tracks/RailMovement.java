package NG.Tracks;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.DataStructures.Generic.AveragingQueue;
import NG.DataStructures.Generic.BlockingTimedArrayQueue;
import NG.DataStructures.Generic.Pair;
import NG.DataStructures.Interpolation.FloatInterpolator;
import NG.DataStructures.Interpolation.LongInterpolator;
import NG.Entities.Train;
import NG.Network.NetworkNode;
import NG.Network.NetworkPosition;
import NG.Network.RailNode;
import NG.Network.Signal;
import NG.Tools.Logger;
import NG.Tools.NetworkPathFinder;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * A
 * @author Geert van Ieperen created on 6-5-2020.
 */
public class RailMovement extends AbstractGameObject {
    private static final float DELTA_TIME = 1f / 128f;
    private static final int METERS_TO_MILLIS = 1000;

    private final Train controller;

    private float speed; // real train speed in direction of facing in meters.
    private float accelerationFraction;
    private double nextUpdateTime;

    private final Deque<TrackPiece> reservedPath = new ArrayDeque<>();

    private long currentTotalMillis; // (millis = 1000 * the real distance) : data type is adequate for almost a lightyear distance
    private TrackPiece currentTrack; // track the train is on
    private boolean isPositiveDirection; // whether the train faces in positive direction on currentTrack
    private long trackEndDistanceMillis;

    private boolean doStop = false;
    private boolean doPlanReverse = false;
    private long stopTotalMillis = Long.MAX_VALUE;

    private RailNode scanEndNode; // track that is scanned latest
    private long scanTrackEndMillis; // total distance to the end of scanTrack
    private boolean scanIsInPathDirection;

    private LongInterpolator totalMillimeters;
    private FloatInterpolator totalToLocalDistance; // maps total distance to track distance
    private BlockingTimedArrayQueue<Pair<TrackPiece, Boolean>> tracks; // maps total distance to track, includes currentTrack

    private float r1 = 1;
    private float r2 = 0;
    private float maxForce = 10;
    private float invMass = 1;
    private float breakForce = 1;
    private float trainLength;

    private AveragingQueue accelerationAverage = new AveragingQueue((int) (0.1f / DELTA_TIME));

    public RailMovement(
            Game game, Train controller, double spawnTime, TrackPiece startPiece, float fraction,
            boolean isPositiveDirection
    ) {
        super(game);
        this.currentTrack = startPiece;
        this.controller = controller;

        this.currentTotalMillis = 0;
        this.totalMillimeters = new LongInterpolator(0, 0L, spawnTime);
        long trackStartDistanceMillis = (long) (-1 * startPiece.getLength() * METERS_TO_MILLIS * fraction);
        this.trackEndDistanceMillis = (long) (trackStartDistanceMillis + startPiece.getLength() * METERS_TO_MILLIS);

        this.scanEndNode = isPositiveDirection ? startPiece.getEndNode() : startPiece.getStartNode();


        this.totalToLocalDistance = new FloatInterpolator(0, 0f, trackStartDistanceMillis, startPiece.getLength(), trackEndDistanceMillis);
        this.tracks = new BlockingTimedArrayQueue<>(0);
        tracks.add(new Pair<>(startPiece, isPositiveDirection), trackStartDistanceMillis);

        this.nextUpdateTime = spawnTime;
        this.isPositiveDirection = isPositiveDirection;
        this.speed = 0;
        this.accelerationFraction = 0;

        initPath();

        Logger.printOnline(() -> String.format(
                "Average acceleration %6.02f | Current acceleration: %6.02f",
                accelerationAverage.average(), accelerationFraction
        ));
    }

    /**
     * sets the parameters describing the speed change of this train
     * @param TE          tractive effort: force applied by the train
     * @param mass        mass of the object
     * @param R1          linear resistance factor
     * @param R2          quadratic resistance factor
     * @param breakForce  force added to the TE when breaking
     * @param trainLength length of the train in units
     */
    public void setProperties(float TE, float mass, float R1, float R2, float breakForce, float trainLength) {
        this.maxForce = TE;
        this.invMass = 1f / mass;
        this.r1 = R1;
        this.r2 = R2;
        this.breakForce = breakForce;
        this.trainLength = trainLength;
    }

    public void update() {
        double gametime = game.timer().getGameTime();
        update(gametime);
    }

    public void update(double gameTime) {
        float speed = this.speed;

        while (nextUpdateTime < gameTime) {
            float resistance = (speed * r1) + (speed * speed * r2);
            if (accelerationFraction < 0) {
                // s = vt + at^2 // movement in meters
                speed += (breakForce + maxForce + resistance) * invMass * DELTA_TIME * accelerationFraction;

            } else {
                // s = vt + at^2 // movement in meters
                speed += (maxForce - resistance) * invMass * DELTA_TIME * accelerationFraction;
            }

            float movement = speed * DELTA_TIME;

            // case: the train stops
            if (movement < 0) {
                if (doPlanReverse) {
                    reverse(trainLength);
                    doPlanReverse = false;

                } else { // do not reverse
                    speed = 0;
                    totalMillimeters.add(currentTotalMillis, nextUpdateTime);
                    nextUpdateTime += DELTA_TIME;
                    continue;
                }
            }

            int movementMillis = (int) (movement * METERS_TO_MILLIS);
            currentTotalMillis += movementMillis;
            totalMillimeters.add(currentTotalMillis, nextUpdateTime);

            // look ahead the projected best-effort stop distance
            long scanTargetMillis = currentTotalMillis + getStopDistanceMillis(speed) + movementMillis;
            while (scanTargetMillis > scanTrackEndMillis) {
                // reserve the next part of the plan
                assert scanEndNode.hasSignal();

                Signal signal = scanEndNode.getSignal();
                NetworkPosition target = controller.getTarget(scanEndNode.getNetworkNode());
                Deque<TrackPiece> path = signal.reservePath(target, scanIsInPathDirection);

                if (path.isEmpty()) {
                    doStop = true;
                    stopTotalMillis = scanTrackEndMillis;
                    break;
                }

                reservedPath.addAll(path);

                for (TrackPiece track : path) {
                    scanTrackEndMillis += track.getLength() * METERS_TO_MILLIS;
                    scanEndNode = track.getNot(scanEndNode);
                }

                scanIsInPathDirection = !scanEndNode.isInDirectionOf(path.getLast());
            }

            // case: the train is at the end of the track, and no new tracks are planned
            if (reservedPath.isEmpty() && currentTotalMillis > trackEndDistanceMillis) {
                Logger.ASSERT.print("Hit end of track");
                currentTotalMillis = trackEndDistanceMillis;
                stopTotalMillis = currentTotalMillis;
                doStop = true;
                speed = 0; // full stop
            }

            // case: train leaves current track
            while (currentTotalMillis > trackEndDistanceMillis) {
                TrackPiece trackPiece = reservedPath.remove();
                RailNode node = isPositiveDirection ? currentTrack.getEndNode() : currentTrack.getStartNode();
                boolean positiveDirection = node.equals(trackPiece.getStartNode());
                commitTrack(trackPiece, positiveDirection);
            }

            // case: train is told to stop
            if (doStop || doPlanReverse) {
                accelerationFraction = 0;

                // only break when necessary
                int stoppingDistance = (int) (stopTotalMillis - currentTotalMillis);
                int stopDistanceNextCycle = getStopDistanceMillis(speed) + movementMillis;

                if (stopDistanceNextCycle > stoppingDistance) {
                    accelerationFraction = -1;
                }
            }

            // loop end update
            nextUpdateTime += DELTA_TIME;
            accelerationAverage.add(accelerationFraction);
        }

        this.speed = Math.abs(speed);
    }

    public void reverse(float reversalLength) {
        speed = 0; // prevents a couple of edge cases
        isPositiveDirection = !isPositiveDirection;

        long passedDistance = (long) (currentTrack.getLength() * METERS_TO_MILLIS - (trackEndDistanceMillis - currentTotalMillis));
        trackEndDistanceMillis = currentTotalMillis + passedDistance;
        float startDistanceMillis = currentTotalMillis - passedDistance;

        float localDistance = totalToLocalDistance.getInterpolated(currentTotalMillis);
        totalToLocalDistance.add(localDistance, currentTotalMillis); // overrides later elements
        totalToLocalDistance.add(isPositiveDirection ? currentTrack.getLength() : 0f, trackEndDistanceMillis);

        tracks.add(new Pair<>(currentTrack, isPositiveDirection), currentTotalMillis);

        currentTotalMillis += reversalLength * METERS_TO_MILLIS;

        while (currentTotalMillis > trackEndDistanceMillis) {
            Pair<TrackPiece, Boolean> previous = tracks.getPrevious(startDistanceMillis);
            startDistanceMillis -= tracks.timeSincePrevious(startDistanceMillis);
            commitTrack(previous.left, !previous.right);
        }

        clearPath();
        initPath();

        Logger.DEBUG.print(totalToLocalDistance, tracks);
    }

    // initializes the reservedPath towards the next signal
    private void initPath() {
        RailNode node = isPositiveDirection ? currentTrack.getEndNode() : currentTrack.getStartNode();
        TrackPiece nextTrack = currentTrack;
        scanTrackEndMillis = trackEndDistanceMillis;

        NetworkPosition target = controller.getTarget(node.getNetworkNode());
        NetworkPathFinder.Path path = null; // lazy init

        while (!node.hasSignal()) {
            NetworkNode networkNode = node.getNetworkNode();
            List<NetworkNode.Direction> next = networkNode.getNext(nextTrack);
            if (next.isEmpty()) return; // EOL

            if (!networkNode.isNetworkCritical()) { // straight w/o signal
                assert next.size() == 1;
                nextTrack = next.get(0).trackPiece;

            } else {
                if (path == null) {
                    // we have to remove all network-critical nodes off the path
                    // hence even if it is straight, we have to do path finding.
                    path = new NetworkPathFinder(nextTrack, networkNode, target).call();
                }

                NetworkNode targetNode = path.remove();

                for (NetworkNode.Direction entry : next) {
                    if (entry.network.equals(targetNode)) {
                        nextTrack = entry.trackPiece;
                        break;
                    }
                }
            }

            node = nextTrack.getNot(node);
            nextTrack.setOccupied(true);
            reservedPath.add(nextTrack);
            scanTrackEndMillis += nextTrack.getLength() * METERS_TO_MILLIS;
        }

        scanEndNode = node;
        scanIsInPathDirection = !scanEndNode.isInDirectionOf(nextTrack);
    }

    private void commitTrack(TrackPiece next, boolean positiveDirection) {
        this.isPositiveDirection = positiveDirection;
        this.currentTrack = next;

        float trackLength = next.getLength();
        long trackStartDistanceMillis = trackEndDistanceMillis;
        trackEndDistanceMillis = (long) (trackStartDistanceMillis + trackLength * METERS_TO_MILLIS);
        tracks.add(new Pair<>(next, positiveDirection), trackStartDistanceMillis);

        if (positiveDirection) {
            totalToLocalDistance.add(0f, trackStartDistanceMillis);
            totalToLocalDistance.add(trackLength, trackEndDistanceMillis);

        } else {
            totalToLocalDistance.add(trackLength, trackStartDistanceMillis);
            totalToLocalDistance.add(0f, trackEndDistanceMillis);
        }
    }

    private void clearPath() {
        for (TrackPiece piece : reservedPath) {
            piece.setOccupied(false);
        }
        reservedPath.clear();
    }

    private int getStopDistanceMillis(float speed) {
        // dv = -1 * (breakForce + maxForce + (v * r1) + (v * v * r2)) * 1/mass;

        // linear solution: ignore resistances
        // v = (breakForce + maxForce) * 1/mass * t;
        // t = (v * mass) / ((breakForce + maxForce));
        // s = 0.5 * a * t * t
        // s = 0.5 * (breakForce + maxForce) * 1/mass * t * t
        // s = 0.5 * (breakForce + maxForce) * 1/mass * (v * mass) / ((breakForce + maxForce)) * (v * mass) / ((breakForce + maxForce))
        // s = 0.5 * (v * v * mass) / ((breakForce + maxForce))
        // TODO accuracy
        float t = (speed) / ((breakForce + maxForce) * invMass);
        float stopDistance = 0.5f * (breakForce + maxForce) * invMass * t * t;

        return (int) (stopDistance * METERS_TO_MILLIS);
    }

    public void setAcceleration(float a) {
        assert a >= -1 && a <= 1 : "Acceleration must be given as a fraction [-1, 1]";
        doStop = false;
        accelerationFraction = a;
    }

    /**
     * @return reals speed in meters per second
     */
    public float getSpeed() {
        return speed;
    }

    public boolean isStopping() {
        return doStop;
    }

    public AveragingQueue getAccelerationAverage() {
        return accelerationAverage;
    }

    /**
     * @return the interpolated position on the given time
     */
    public Vector3f getPosition(double time) {
        return getPosition(time, 0);
    }

    /**
     * Computes the position on the given time, and adds the given displacement to that position. The result is a
     * position exactly {@code displacement} further.
     * @return the interpolated position on the given time
     */
    public Vector3f getPosition(double time, float displacement) {
        update(time);

        double totalMillis = totalMillimeters.getInterpolated(time) + displacement * METERS_TO_MILLIS;
        TrackPiece track = tracks.getPrevious(totalMillis).left;
        float localDistance = totalToLocalDistance.getInterpolated(totalMillis);

        float fractionTravelled = localDistance / track.getLength();
        return track.getPositionFromFraction(fractionTravelled);
    }

    /**
     * @return the interpolated direction of movement (derivative of position) on the given time
     */
    public Vector3f getDirection(double time) {
        return getDirection(time, 0);
    }

    /**
     * @return the interpolated direction of movement (derivative of position) on the given time
     */
    public Vector3f getDirection(double time, float displacement) {
        update(time);

        float distanceMillis = totalMillimeters.getInterpolated(time) + displacement * METERS_TO_MILLIS;
        float localDistance = totalToLocalDistance.getInterpolated(distanceMillis);
        Pair<TrackPiece, Boolean> activeTrack = tracks.getPrevious(distanceMillis);
        TrackPiece track = activeTrack.left;

        float fractionTravelled = localDistance / track.getLength();
        Vector3f directionOfTrack = track.getDirectionFromFraction(fractionTravelled);

        Boolean isPositive = activeTrack.right;
        if (!isPositive) directionOfTrack.negate();
        directionOfTrack.normalize();

        return directionOfTrack;
    }

    public Quaternionf getRotation(double time) {
        return getRotation(time, 0);
    }

    public Quaternionf getRotation(double time, float displacement) {
//        update(time); // included in getDirection(time)
        Vector3f direction = getDirection(time, displacement).normalize();

        float yawAngle = Math.atan2(direction.y, direction.x);
        float hzMovement = Math.sqrt(direction.x * direction.x + direction.y * direction.y);
        float pitchAngle = Math.atan2(direction.z, hzMovement);

        return new Quaternionf()
                .rotateAxis(pitchAngle, -1, 0, 0)
                .rotateAxis(yawAngle, 0, 0, 1);
    }

    public void discardUpTo(float time) {
        float distance = totalMillimeters.getInterpolated(time);
        totalMillimeters.removeUntil(time);
        totalToLocalDistance.removeUntil(distance);
        tracks.removeUntil(distance);
    }

    public boolean hasPath() {
        return !reservedPath.isEmpty();
    }
}
