package NG.Tracks;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.DataStructures.Generic.AveragingQueue;
import NG.DataStructures.Generic.BlockingTimedArrayQueue;
import NG.DataStructures.Generic.Pair;
import NG.DataStructures.Interpolation.FloatInterpolator;
import NG.DataStructures.Interpolation.LongInterpolator;
import NG.Entities.Train;
import NG.Network.*;
import NG.Tools.Logger;
import NG.Tools.NetworkPathFinder;
import NG.Tools.Toolbox;
import NG.Tools.Vectors;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.BooleanSupplier;

/**
 * A
 * @author Geert van Ieperen created on 6-5-2020.
 */
public class RailMovement extends AbstractGameObject implements Schedule.UpdateListener {
    private static final float DELTA_TIME = 1f / 128f;
    private static final int METERS_TO_MILLIS = 1000;
    private static final float SPEED_RESOLUTION = 1 / (DELTA_TIME * METERS_TO_MILLIS);
    public static final int SCAN_BUFFER_MILLIS = 10;
    private static final double SIGNAL_PATHING_TIMEOUT = 1.0 / 8;
    private static final int STOP_TARGET_SNAP_DISTANCE_MILLIS = 10;

    public static final BooleanSupplier ALWAYS = () -> true;

    private final Train controller;

    private float speed; // real train speed in direction of facing in meters, ignoring SPEED_RESOLUTION
    private double updateTime;

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
    private boolean scanIsInPathDirection; // TODO can we use NetworkPosition#getNodes()?
    private int scanTargetsAhead = 0;
    private double signalPathTimeout = Double.NEGATIVE_INFINITY;
    private SpeedTarget endOfTrackBrakeTarget;

    private float maxSpeed = 0;

    private LongInterpolator totalMillimeters; // maps time to total distance millimeters
    private FloatInterpolator totalToLocalDistance; // maps total distance millimeters to track local distance
    private BlockingTimedArrayQueue<Pair<TrackPiece, Boolean>> tracks; // maps total distance millimeters to track, includes currentTrack

    private float r1 = 1;
    private float r2 = 0;
    private float maxForce = 1;
    private float invMass = 1;
    private float breakForce = 1;

    private long trainLengthMillis = 0;
    private float trainMaxSpeed = 1;

    private AveragingQueue accelerationAverage = new AveragingQueue((int) (0.1f / DELTA_TIME));

    public RailMovement(
            Game game, Train controller, double spawnTime, TrackPiece startPiece, boolean isPositiveDirection
    ) {
        super(game);
        this.currentTrack = startPiece;
        this.controller = controller;

        this.currentTotalMillis = 0;
        this.totalMillimeters = new LongInterpolator(0, 0L, spawnTime);
        long trackStartDistanceMillis = (long) (-1 * startPiece.getLength() * METERS_TO_MILLIS * 1);
        this.trackEndDistanceMillis = (long) (trackStartDistanceMillis + startPiece.getLength() * METERS_TO_MILLIS);

        this.scanEndNode = isPositiveDirection ? startPiece.getEndNode() : startPiece.getStartNode();

        this.totalToLocalDistance = new FloatInterpolator(0, 0f, trackStartDistanceMillis, startPiece.getLength(), trackEndDistanceMillis);
        this.tracks = new BlockingTimedArrayQueue<>(0);
        tracks.add(new Pair<>(startPiece, isPositiveDirection), trackStartDistanceMillis);

        this.updateTime = spawnTime;
        this.isPositiveDirection = isPositiveDirection;
        this.speed = 0;

        initPath();
    }

    /**
     * sets the parameters describing the speed change of this train
     * @param TE            tractive effort: force applied by the train
     * @param mass          mass of the object
     * @param R1            linear resistance factor
     * @param R2            quadratic resistance factor
     * @param breakForce    force added to the TE when breaking
     * @param trainLength   length of the train in units
     * @param trainMaxSpeed
     */
    public void setProperties(
            float TE, float mass, float R1, float R2, float breakForce, float trainLength, float trainMaxSpeed
    ) {
        this.maxForce = TE;
        this.invMass = 1f / mass;
        this.r1 = R1;
        this.r2 = R2;
        this.breakForce = breakForce;
        this.trainLengthMillis = (long) (trainLength * METERS_TO_MILLIS);
        this.trainMaxSpeed = trainMaxSpeed;
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

        while (updateTime < gameTime) {
            float accelerationFraction;

            if (speed == 0) { // case: the train is stopped
                if (doStop || doReverse) {
                    if (doReverse) {
                        executeReversal();
                        doReverse = false;
                    }

                    if (doStop) {
                        accelerationFraction = 0;

                    } else {
                        accelerationFraction = 1;
                    }

                } else {
                    futureSpeedTargets.removeIf(SpeedTarget::isInvalid);

                    if (futureSpeedTargets.isEmpty()) {
                        accelerationFraction = 1;

                    } else {
                        // shortcut for checking whether we should stay stopped
                        SpeedTarget nextSpeedTarget = futureSpeedTargets.peek();
                        if (shouldSnapStopped(nextSpeedTarget)) {
                            currentTotalMillis = nextSpeedTarget.startMillis; // snap
                            accelerationFraction = 0;

                        } else {
                            accelerationFraction = 1;
                        }
                    }
                }

            } else if (doStop || doReverse) { // case: the train should stop
                accelerationFraction = -1;

            } else { // search for the next speed target
                boolean shouldStop = false;
                // first remove all non-applicable speed targets
                futureSpeedTargets.removeIf(SpeedTarget::isInvalid);
                activeSpeedTargets.removeIf(t -> t.isInvalid() || t.endMillis < currentTotalMillis - trainLengthMillis);

                SpeedTarget nextSpeedTarget = null;
                // move speed targets that have passed to active
                Iterator<SpeedTarget> futureTargetIterator = futureSpeedTargets.iterator();
                while (futureTargetIterator.hasNext()) {
                    nextSpeedTarget = futureTargetIterator.next();

                    if (nextSpeedTarget.startMillis < currentTotalMillis) { // has passed
                        futureTargetIterator.remove();

                        // only move if applicable
                        float trainEndMillis = currentTotalMillis - trainLengthMillis;
                        if (nextSpeedTarget.speed > 0 && nextSpeedTarget.endMillis > trainEndMillis) {
                            activeSpeedTargets.add(nextSpeedTarget);
                        }

                    } else if (shouldSnapStopped(nextSpeedTarget)) {
                        currentTotalMillis = nextSpeedTarget.startMillis; // snap
                        shouldStop = true;
                        break;

                    } else {
                        // all targets have been moved
                        break;
                    }
                }

                if (shouldStop) {
                    accelerationFraction = 0;
                    if (speed > 1) Logger.ASSERT.print(speed, nextSpeedTarget);
                    speed = 0;

                } else {
                    // calculate maximum speed
                    float maxSpeed = trainMaxSpeed;
                    for (SpeedTarget active : activeSpeedTargets) {
                        if (active.speed < maxSpeed) {
                            maxSpeed = active.speed;
                        }
                    }
                    this.maxSpeed = maxSpeed;

                    if (speed > maxSpeed) {
                        speed = maxSpeed;
                        accelerationFraction = 0;

                    } else {
                        accelerationFraction = 1;
                    }

                    // calculate whether the next speed target requires breaking
                    if (nextSpeedTarget != null) {
                        if (doRequireBreaking(speed, nextSpeedTarget.speed, nextSpeedTarget.startMillis)) {
                            accelerationFraction = -1;
                        }
                    }
                }
            }

            // update speed
            float resistance = (speed * r1) + (speed * speed * r2);
            if (accelerationFraction < 0) {
                speed += (breakForce + maxForce + resistance) * invMass * DELTA_TIME * accelerationFraction;

            } else if (accelerationFraction > 0 || maxForce < resistance) { // maxForce < resistance => train can't pull
                speed += (maxForce - resistance) * invMass * DELTA_TIME * accelerationFraction;
            }

            if (speed < 0) speed = 0;

            accelerationAverage.add(accelerationFraction);

            // track our tail was on the previous tick (+1 to include equal)
            TrackPiece prePiece = tracks.getPrevious(currentTotalMillis - trainLengthMillis + 1).left;

            // update position
            // s = vt + at^2 // movement in meters
            int movementMillis = (int) Math.ceil(speed * DELTA_TIME * METERS_TO_MILLIS);
            currentTotalMillis += movementMillis;
            totalMillimeters.add(currentTotalMillis, updateTime);


            // track our tail is now. when leaving a track, free it
            TrackPiece postPiece = tracks.getPrevious(currentTotalMillis - trainLengthMillis + 1).left;
            if (prePiece != postPiece) {
                prePiece.setOccupied(false);
            }

            if (!doStop && updateTime > signalPathTimeout) {
                if (scanTargetsAhead == 0) { // <- this line took longer than it should
                    // look ahead the projected best-effort stop distance
                    long scanTargetMillis = currentTotalMillis + getBreakDistanceMillis(speed, 0) + movementMillis + SCAN_BUFFER_MILLIS;
                    while (scanTargetMillis > scanTrackEndMillis) {
                        // reserve the next part of the plan
                        assert scanEndNode.hasSignal();

                        Signal signal = scanEndNode.getSignal();
                        Deque<TrackPiece> path = signal.reservePath(scanIsInPathDirection,
                                depth -> controller.getTarget(depth + scanTargetsAhead)
                        );

                        if (path.isEmpty()) {
                            // stop at the end, retry after a timeout
                            signalPathTimeout = updateTime + SIGNAL_PATHING_TIMEOUT;

                            if (endOfTrackBrakeTarget == null || endOfTrackBrakeTarget.isInvalid()) {
                                endOfTrackBrakeTarget = new SpeedTarget(scanTrackEndMillis, scanTrackEndMillis, 0f, () -> scanTrackEndMillis < scanTargetMillis);
                                futureSpeedTargets.add(endOfTrackBrakeTarget);
                            }
                            break;
                        }

                        for (TrackPiece track : path) {
                            appendToPath(track);
                        }

                        scanIsInPathDirection = !scanEndNode.isInDirectionOf(path.getLast());
                    }
                }
            }

            // case: the train is at the end of the track, and no new tracks are planned
            if (reservedPath.isEmpty() && currentTotalMillis > trackEndDistanceMillis) {
                Logger.ASSERT.print("Hit end of track", speed);
                currentTotalMillis = trackEndDistanceMillis;
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
            updateTime += DELTA_TIME;
            this.speed = speed;
        }
    }

    /** returns true iff we should stop, and we are within snapping distance of this target */
    private boolean shouldSnapStopped(SpeedTarget nextSpeedTarget) {
        return nextSpeedTarget.speed == 0 && currentTotalMillis > nextSpeedTarget.startMillis - STOP_TARGET_SNAP_DISTANCE_MILLIS;
    }

    private boolean doRequireBreaking(float currentSpeed, float targetSpeed, long targetMillis) {
        if (currentSpeed < targetSpeed) return false;

        // calculate distance required vs distance available
        long speedTargetSpaceNext = targetMillis - currentTotalMillis + (int) (currentSpeed * DELTA_TIME * METERS_TO_MILLIS);
        int speedTargetDistance = getBreakDistanceMillis(currentSpeed, targetSpeed);

        // only break when necessary
        return (speedTargetSpaceNext < speedTargetDistance);
    }

    private void executeReversal() {
        isPositiveDirection = !isPositiveDirection;

        // distance from start of track to currentTotalMillis
        long passedDistance = (long) (currentTrack.getLength() * METERS_TO_MILLIS - (trackEndDistanceMillis - currentTotalMillis));
        trackEndDistanceMillis = currentTotalMillis + passedDistance;
        double startDistanceMillis = currentTotalMillis - passedDistance;

        float localDistance = totalToLocalDistance.getInterpolated(currentTotalMillis);
        totalToLocalDistance.add(localDistance, currentTotalMillis); // overrides later elements
        totalToLocalDistance.add(isPositiveDirection ? currentTrack.getLength() : 0f, trackEndDistanceMillis);

        tracks.add(new Pair<>(currentTrack, isPositiveDirection), currentTotalMillis);


        currentTotalMillis += trainLengthMillis;
        activeSpeedTargets.clear();
        clearPath();

        Pair<TrackPiece, Boolean> previous = tracks.getPrevious(startDistanceMillis);
        while (currentTotalMillis > trackEndDistanceMillis) {
            TrackPiece track = previous.left;
            commitTrack(track, !previous.right);

            long trackStart = trackEndDistanceMillis - (long) (track.getLength() * METERS_TO_MILLIS);
            futureSpeedTargets.add(new SpeedTarget(trackStart, trackEndDistanceMillis, track.getMaximumSpeed()));
        }

        initPath();
    }

    // initializes the reservedPath towards the next signal
    private void initPath() {
        scanEndNode = isPositiveDirection ? currentTrack.getEndNode() : currentTrack.getStartNode();
        TrackPiece nextTrack = currentTrack;
        scanTrackEndMillis = trackEndDistanceMillis;

        NetworkPosition target = controller.getTarget(0);
        NetworkPathFinder.Path path = null; // lazy init

        while (!scanEndNode.hasSignal()) {
            NetworkNode networkNode = scanEndNode.getNetworkNode();
            List<NetworkNode.Direction> next = networkNode.getNext(nextTrack);
            if (next.isEmpty()) return; // EOL

            // we have to accept all network-critical nodes in path
            // hence even if it is straight, we have to do path finding.
            if (!networkNode.isNetworkCritical()) { // straight w/o signal
                assert next.size() == 1;
                nextTrack = next.get(0).trackPiece;

            } else {
                if (target == null) {
                    int randIndex = Toolbox.random.nextInt(next.size());
                    nextTrack = next.get(randIndex).trackPiece;

                } else {
                    if (path == null) {
                        path = new NetworkPathFinder(nextTrack, networkNode, target).call();
                        if (path.isEmpty()) return;
                    }

                    NetworkNode targetNode = path.remove();

                    for (NetworkNode.Direction entry : next) {
                        if (entry.network.equals(targetNode)) {
                            nextTrack = entry.trackPiece;
                            break;
                        }
                    }
                }
            }

            nextTrack.setOccupied(true);
            appendToPath(nextTrack);
        }

        scanIsInPathDirection = !scanEndNode.isInDirectionOf(nextTrack);
    }

    private void appendToPath(TrackPiece nextTrack) {
        assert nextTrack != null;
        reservedPath.add(nextTrack);

        long newScanTrackEndMillis = scanTrackEndMillis + (long) (nextTrack.getLength() * METERS_TO_MILLIS);
        futureSpeedTargets.add(new SpeedTarget(scanTrackEndMillis, newScanTrackEndMillis, nextTrack.getMaximumSpeed()));

        scanEndNode = nextTrack.getNot(scanEndNode);
        scanTrackEndMillis = newScanTrackEndMillis;
        NetworkNode scanNode = scanEndNode.getNetworkNode();

        NetworkPosition target = controller.getTarget(scanTargetsAhead);
        if (target != null && target.containsNode(nextTrack, scanNode)) {
            scanTargetsAhead++;

            SpeedTarget speedTarget = new SpeedTarget(
                    scanTrackEndMillis, scanTrackEndMillis, 0,
                    () -> controller.shouldWaitFor(target)
            );
            futureSpeedTargets.add(speedTarget);
        }
    }

    private void commitTrack(TrackPiece next, boolean positiveDirection) {
        this.isPositiveDirection = positiveDirection;
        this.currentTrack = next;

        float trackLength = next.getLength();
        long trackStartDistanceMillis = trackEndDistanceMillis;
        trackEndDistanceMillis = (long) (trackStartDistanceMillis + trackLength * METERS_TO_MILLIS);
        tracks.add(new Pair<>(next, positiveDirection), trackStartDistanceMillis);

        RailNode newNode;
        if (positiveDirection) {
            totalToLocalDistance.add(0f, trackStartDistanceMillis);
            totalToLocalDistance.add(trackLength, trackEndDistanceMillis);
            newNode = next.getStartNode();

        } else {
            totalToLocalDistance.add(trackLength, trackStartDistanceMillis);
            totalToLocalDistance.add(0f, trackEndDistanceMillis);
            newNode = next.getEndNode();
        }

        controller.onArrival(next, newNode);
    }

    @Override
    public void onScheduleUpdate(NetworkPosition element) {
        scanTargetsAhead--;
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
     * @return actual speed in meters per second
     */
    public float getSpeed() {
        int millisPerSecond = (int) (speed * DELTA_TIME * METERS_TO_MILLIS);
        return millisPerSecond / (DELTA_TIME * METERS_TO_MILLIS);
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    /** doStop || doReverse */
    public boolean isStopping() {
        return doStop || doReverse;
    }

    public AveragingQueue getAccelerationAverage() {
        return accelerationAverage;
    }

    public Pair<TrackPiece, Boolean> getTracksAt(double timeStamp) {
        return tracks.getPrevious(totalMillimeters.getInterpolated(timeStamp));
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
        Pair<TrackPiece, Boolean> previous = tracks.getPrevious(totalMillis);
        if (previous == null) previous = tracks.getNext(totalMillis);
        TrackPiece track = previous.left;
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
        Vector3f direction = getDirection(time, displacement);
        return Vectors.xTo(direction);
    }

    public void discardUpTo(double time) {
        long distance = totalMillimeters.getInterpolated(time);
        double totalMillimetersMinimum = totalToLocalDistance.timeOfPrevious(distance - trainLengthMillis);
        distance = (long) totalMillimetersMinimum;

        totalMillimeters.removeUntil(time);
        totalToLocalDistance.removeUntil(distance);
        tracks.removeUntil(distance);
    }

    public boolean hasPath() {
        return reservedPath.isEmpty();
    }

    public void removePath() {
        for (TrackPiece trackPiece : reservedPath) {
            trackPiece.setOccupied(false);
        }
        reservedPath.clear();
    }

    private class SpeedTarget implements Comparable<SpeedTarget> {
        public final long startMillis;
        public final long endMillis;
        public final float speed;
        private final BooleanSupplier condition;

        public SpeedTarget(long startMillis, long endMillis, float speed) {
            this(startMillis, endMillis, speed, ALWAYS);
        }

        public SpeedTarget(
                long startMillis, long endMillis, float speed, BooleanSupplier condition
        ) {
            this.startMillis = startMillis;
            this.endMillis = endMillis;
            this.speed = speed;
            this.condition = condition;
        }

        public boolean isInvalid() {
            return !condition.getAsBoolean();
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
