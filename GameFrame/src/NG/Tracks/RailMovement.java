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

import java.util.*;

/**
 * A
 * @author Geert van Ieperen created on 6-5-2020.
 */
public class RailMovement extends AbstractGameObject {
    private static final float DELTA_TIME = 1f / 128f;
    private static final int METERS_TO_MILLIS = 1000;

    private final Train controller;

    private float speed; // real train speed in direction of facing in meters.
    private double nextUpdateTime;

    private final Deque<TrackPiece> reservedPath = new ArrayDeque<>();
    private final PriorityQueue<SpeedTarget> futureSpeedTargets = new PriorityQueue<>();
    private final List<SpeedTarget> activeSpeedTargets = new ArrayList<>();

    private long currentTotalMillis; // (millis = 1000 * the real distance) : data type is adequate for almost a lightyear distance
    private TrackPiece currentTrack; // track the train is on
    private boolean isPositiveDirection; // whether the train faces in positive direction on currentTrack
    private long trackEndDistanceMillis;

    private boolean doStop = true;
    private boolean doReverse = false;

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

    private AveragingQueue accelerationAverage = new AveragingQueue((int) (0.1f / DELTA_TIME));
    private float trainLengthMillis = 0;
    private float maxSpeed = 0;

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

        initPath();

        Logger.printOnline(() -> String.format(
                "Speed : %6.02f | Max : %6.02f | Average acceleration : %6.02f",
                speed, maxSpeed, accelerationAverage.average())
        );
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
        this.trainLengthMillis = trainLength * METERS_TO_MILLIS;
    }

    public void update() {
        double gametime = game.timer().getGameTime();
        update(gametime);
    }

    public void reverse() {
        doReverse = true;
    }

    public void stop() {
        doStop = true;
    }

    public void start() {
        doStop = false;
        doReverse = false;
    }

    public void update(double gameTime) {
        float speed = this.speed;

        while (nextUpdateTime < gameTime) {
            float accelerationFraction = 1f;

            if (speed == 0) { // case: the train is stopped
                if (doReverse) {
                    executeReversal();
                    doReverse = false;
                    accelerationFraction = 1;

                }
                if (doStop) {
                    accelerationFraction = 0;
                }

            } else if (doStop || doReverse) { // case: the train should stop
                accelerationFraction = -1;

            } else { // search for the next speed target

                // first remove all non-applicable speed targets
                activeSpeedTargets.removeIf(t -> t.endMillis < currentTotalMillis - trainLengthMillis);

                SpeedTarget nextSpeedTarget = null;
                // move speed targets that have passed to active
                Iterator<SpeedTarget> futureTargetIterator = futureSpeedTargets.iterator();
                while (futureTargetIterator.hasNext()) {
                    nextSpeedTarget = futureTargetIterator.next();

                    if (nextSpeedTarget.startMillis < currentTotalMillis) { // has passed
                        futureTargetIterator.remove();

                        // only move if involuntary and applicable
                        float trainEndMillis = currentTotalMillis - trainLengthMillis;
                        if (!nextSpeedTarget.isVoluntary && nextSpeedTarget.endMillis > trainEndMillis) {
                            activeSpeedTargets.add(nextSpeedTarget);
                        }

                    } else {
                        break;
                    }
                }

                // calculate maximum speed
                float maxSpeed = Float.POSITIVE_INFINITY;
                for (SpeedTarget active : activeSpeedTargets) {
                    if (active.speed < maxSpeed) {
                        maxSpeed = active.speed;
                    }
                }
                this.maxSpeed = maxSpeed;

                if (speed > maxSpeed) {
                    speed = maxSpeed;
                    accelerationFraction = 0;
                }

                if (nextSpeedTarget != null && nextSpeedTarget.speed <= speed) {
                    // calculate distance required vs distance available
                    long speedTargetSpaceNext = nextSpeedTarget.startMillis - currentTotalMillis + (int) (speed * DELTA_TIME * METERS_TO_MILLIS);
                    int speedTargetDistance = getBreakDistanceMillis(speed, nextSpeedTarget.speed);

                    // only break when necessary
                    if (speedTargetSpaceNext < speedTargetDistance) {
                        accelerationFraction = -1;
                    }
                }
            }

            // update speed
            float resistance = (speed * r1) + (speed * speed * r2);
            if (accelerationFraction < 0) {
                speed += (breakForce + maxForce + resistance) * invMass * DELTA_TIME * accelerationFraction;

            } else {
                speed += (maxForce - resistance) * invMass * DELTA_TIME * accelerationFraction;
            }

            if (speed < 0) {
                speed = 0;
                accelerationFraction = 0; // approximately
            }
            accelerationAverage.add(accelerationFraction);

            // case: speed is higher than allowed
            TrackPiece prePiece = tracks.getPrevious(currentTotalMillis - trainLengthMillis).left;

            // update position
            // s = vt + at^2 // movement in meters
            int movementMillis = (int) (speed * DELTA_TIME * METERS_TO_MILLIS);
            currentTotalMillis += movementMillis;
            totalMillimeters.add(currentTotalMillis, nextUpdateTime);

            // if a track is left, free it
            TrackPiece postPiece = tracks.getPrevious(currentTotalMillis - trainLengthMillis).left;
            if (prePiece != postPiece) {
                prePiece.setOccupied(false);
            }

            // look ahead the projected best-effort stop distance
            long scanTargetMillis = currentTotalMillis + getBreakDistanceMillis(speed, 0) + movementMillis;
            while (scanTargetMillis > scanTrackEndMillis) {
                // reserve the next part of the plan
                assert scanEndNode.hasSignal();

                Signal signal = scanEndNode.getSignal();
                NetworkPosition target = controller.getTarget(scanEndNode.getNetworkNode());
                Deque<TrackPiece> path = signal.reservePath(target, scanIsInPathDirection);

                if (path.isEmpty()) {
                    futureSpeedTargets.add(new SpeedTarget(scanTrackEndMillis, scanTrackEndMillis, 0f, true));
                    break;
                }

                for (TrackPiece track : path) {
                    appendToPath(track);
                }

                scanIsInPathDirection = !scanEndNode.isInDirectionOf(path.getLast());
            }

            // case: the train is at the end of the track, and no new tracks are planned
            if (reservedPath.isEmpty() && currentTotalMillis > trackEndDistanceMillis) {
                Logger.ASSERT.print("Hit end of track");
                currentTotalMillis = trackEndDistanceMillis;
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

            // loop end update
            nextUpdateTime += DELTA_TIME;

            this.speed = speed;
        }
    }

    private void executeReversal() {
        isPositiveDirection = !isPositiveDirection;

        long passedDistance = (long) (currentTrack.getLength() * METERS_TO_MILLIS - (trackEndDistanceMillis - currentTotalMillis));
        trackEndDistanceMillis = currentTotalMillis + passedDistance;
        double startDistanceMillis = tracks.timeOfNext(currentTotalMillis);

        float localDistance = totalToLocalDistance.getInterpolated(currentTotalMillis);
        totalToLocalDistance.add(localDistance, currentTotalMillis); // overrides later elements
        totalToLocalDistance.add(isPositiveDirection ? currentTrack.getLength() : 0f, trackEndDistanceMillis);

        tracks.add(new Pair<>(currentTrack, isPositiveDirection), currentTotalMillis);

        currentTotalMillis += trainLengthMillis;
        activeSpeedTargets.clear();
        clearPath();

        while (currentTotalMillis > trackEndDistanceMillis) {
            Pair<TrackPiece, Boolean> previous = tracks.getPrevious(startDistanceMillis);
            startDistanceMillis = tracks.timeOfPrevious(startDistanceMillis);

            TrackPiece track = previous.left;
            commitTrack(track, !previous.right);

            long trackStart = trackEndDistanceMillis - (long) (track.getLength() * METERS_TO_MILLIS);
            futureSpeedTargets.add(new SpeedTarget(trackStart, trackEndDistanceMillis, track.getMaximumSpeed(), false));
        }

        initPath();
    }

    // initializes the reservedPath towards the next signal
    private void initPath() {
        scanEndNode = isPositiveDirection ? currentTrack.getEndNode() : currentTrack.getStartNode();
        TrackPiece nextTrack = currentTrack;
        scanTrackEndMillis = trackEndDistanceMillis;

        NetworkPosition target = controller.getTarget(scanEndNode.getNetworkNode());
        NetworkPathFinder.Path path = null; // lazy init

        while (!scanEndNode.hasSignal()) {
            NetworkNode networkNode = scanEndNode.getNetworkNode();
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

            nextTrack.setOccupied(true);
            appendToPath(nextTrack);
        }
        scanEndNode.getSignal().validateConnections(); // optional

        scanIsInPathDirection = !scanEndNode.isInDirectionOf(nextTrack);
    }

    private void appendToPath(TrackPiece nextTrack) {
        reservedPath.add(nextTrack);

        long newScanTrackEndMillis = scanTrackEndMillis + (long) (nextTrack.getLength() * METERS_TO_MILLIS);
        futureSpeedTargets.add(new SpeedTarget(scanTrackEndMillis, newScanTrackEndMillis, nextTrack.getMaximumSpeed(), false));

        scanEndNode = nextTrack.getNot(scanEndNode);
        scanTrackEndMillis = newScanTrackEndMillis;
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
        futureSpeedTargets.clear();
    }

    private int getBreakDistanceMillis(float currentSpeed, float targetSpeed) {
        if (targetSpeed > currentSpeed) return 0;
        // dv = -1 * (breakForce + maxForce + (v * r1) + (v * v * r2)) * 1/mass;

        // linear solution: ignore resistances
        // a = dv = -1 * (breakForce + maxForce) * 1/mass;
        // https://www.johannes-strommer.com/english/formulas/velocity-acceleration-time-distance/
        // s = (v*v - u*u) / 2a
        float a = -(breakForce + maxForce) * invMass;
        float stopDistance = (targetSpeed * targetSpeed - currentSpeed * currentSpeed) / (2 * a);
        return (int) (stopDistance * METERS_TO_MILLIS);
    }

    /**
     * @return reals speed in meters per second
     */
    public float getSpeed() {
        return speed;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public boolean isStopping() {
        return doStop || doReverse;
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

    private class SpeedTarget implements Comparable<SpeedTarget> {
        public final long startMillis;
        public final long endMillis;
        public final float speed;
        public final boolean isVoluntary;

        public SpeedTarget(long startMillis, long endMillis, float speed, boolean isVoluntary) {
            this.startMillis = startMillis;
            this.endMillis = endMillis;
            this.speed = speed;
            this.isVoluntary = isVoluntary;
            assert isVoluntary || speed > 0 : "Impassable target";
        }

        /** computes which speed target must be reacted on first */
        @Override
        public int compareTo(SpeedTarget other) {
            if (this.startMillis < other.startMillis) {
                return compareSorted(this, other);
            } else {
                return -compareSorted(other, this);
            }
        }

        private int compareSorted(SpeedTarget first, SpeedTarget last) {
            int breakDistance = getBreakDistanceMillis(first.speed, last.speed);
            return Float.compare(first.startMillis + breakDistance, last.startMillis);
        }

        @Override
        public String toString() {
            return "SpeedTarget{" +
                    "startMillis=" + startMillis +
                    ", speed=" + speed +
                    '}';
        }
    }
}
