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
import NG.Network.RailNode;
import NG.Tools.Logger;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A
 * @author Geert van Ieperen created on 6-5-2020.
 */
public class RailMovement extends AbstractGameObject {
    private static final float DELTA_TIME = 1f / 128f;
    private static final int METERS_TO_MILLIS = 1000;
    private final Train controller;

    /**
     * real train speed in direction of facing in meters.
     */
    private float speed;
    private float accelerationFraction;

    private double nextUpdateTime;
    private long currentTotalMillis; // (millis = 1000 * the real distance) : data type is adequate for almost a lightyear distance
    private TrackPiece currentTrack; // track the train is currently on

    private boolean positiveDirection; // whether the train faces in positive track direction

    private LongInterpolator totalMillimeters;
    private FloatInterpolator totalToLocalDistance; // maps total distance to track distance
    private BlockingTimedArrayQueue<Pair<TrackPiece, Boolean>> tracks; // maps total distance to track, includes currentTrack
    private long trackScanTotalMillis;
    private long trackStartDistanceMillis;
    private long trackEndDistanceMillis;

    private float r1 = 1;
    private float r2 = 0;
    private float maxForce = 10;
    private float invMass = 1;
    private float breakForce = 1;
    private boolean doStop = false;

    private AveragingQueue accelerationAverage = new AveragingQueue((int) (0.1f / DELTA_TIME));

    public RailMovement(
            Game game, Train controller, double spawnTime, TrackPiece startPiece, float fraction,
            boolean positiveDirection
    ) {
        super(game);
        this.currentTrack = startPiece;
        this.controller = controller;

        this.currentTotalMillis = 0;
        this.totalMillimeters = new LongInterpolator(0, 0L, spawnTime);
        this.trackStartDistanceMillis = (long) (-1 * startPiece.getLength() * METERS_TO_MILLIS * fraction);
        this.trackEndDistanceMillis = (long) (trackStartDistanceMillis + startPiece.getLength() * METERS_TO_MILLIS);

        this.totalToLocalDistance = new FloatInterpolator(0, 0f, trackStartDistanceMillis, startPiece.getLength(), trackEndDistanceMillis);
        this.tracks = new BlockingTimedArrayQueue<>(0);
        tracks.add(new Pair<>(startPiece, positiveDirection), trackStartDistanceMillis);

        this.nextUpdateTime = spawnTime;
        this.positiveDirection = positiveDirection;
        this.speed = 0;
        this.accelerationFraction = 0;

        Logger.printOnline(() -> String.format(
                "Average acceleration %6.02f | Current acceleration: %6.02f",
                accelerationAverage.average(), accelerationFraction
        ));
    }

    /**
     * sets the parameters describing the speed change of this train
     * @param TE         tractive effort: force applied by the train
     * @param mass       mass of the object
     * @param R1         linear resistance factor
     * @param R2         quadratic resistance factor
     * @param breakForce
     */
    public void setForceFunction(float TE, float mass, float R1, float R2, float breakForce) {
        this.maxForce = TE;
        this.invMass = 1f / mass;
        this.r1 = R1;
        this.r2 = R2;
        this.breakForce = breakForce;
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

            if (movement < 0) { // when the train reverses
                totalMillimeters.add(currentTotalMillis, nextUpdateTime);
                speed = 0;
                nextUpdateTime += DELTA_TIME;
                continue;
            }

            currentTotalMillis += movement * METERS_TO_MILLIS;
            totalMillimeters.add(currentTotalMillis, nextUpdateTime);
            // speed update after movement update

            trackScanTotalMillis = currentTotalMillis + getStopDistanceMillis(speed);
            while (trackScanTotalMillis > trackEndDistanceMillis) {
                progressTrack();
            }

            if (doStop) {
                accelerationFraction = 0;

                // only break when necessary
                int stoppingDistance = (int) (trackEndDistanceMillis - currentTotalMillis);
                int movementNextCycle = (int) (speed * DELTA_TIME * METERS_TO_MILLIS);
                int stopDistanceNextCycle = getStopDistanceMillis(speed) + movementNextCycle;

                if (stopDistanceNextCycle > stoppingDistance) {
                    accelerationFraction = -1;
                }
            }

            if (trackEndDistanceMillis < currentTotalMillis) {
                Logger.ASSERT.print("Hit end of track");
                currentTotalMillis = trackEndDistanceMillis;
                speed = 0; // full stop
            }

            nextUpdateTime += DELTA_TIME;
            accelerationAverage.add(accelerationFraction);
        }

        this.speed = Math.abs(speed);
    }

    public void reverse(float reversalLength) {
        speed = 0; // prevents a couple of edge cases
        positiveDirection = !positiveDirection;
        Pair<TrackPiece, Boolean> track = tracks.getPrevious(currentTotalMillis);

        float oldTrackStartMillis = trackStartDistanceMillis;
        trackStartDistanceMillis = (long) (track.left.getLength() * METERS_TO_MILLIS - (trackEndDistanceMillis - currentTotalMillis));
        trackEndDistanceMillis = currentTotalMillis + trackStartDistanceMillis;

        Float localDistance = totalToLocalDistance.getInterpolated(currentTotalMillis);
        totalToLocalDistance.add(localDistance, currentTotalMillis); // overrides later elements
        totalToLocalDistance.add(positiveDirection ? currentTrack.getLength() : 0f, trackEndDistanceMillis);

        tracks.add(new Pair<>(track.left, positiveDirection), currentTotalMillis);

        currentTotalMillis += reversalLength * METERS_TO_MILLIS;

        trackScanTotalMillis = currentTotalMillis;
        while (trackScanTotalMillis > trackEndDistanceMillis) {
            Pair<TrackPiece, Boolean> previous = tracks.getPrevious(oldTrackStartMillis);
            oldTrackStartMillis -= tracks.timeSincePrevious(oldTrackStartMillis);
            progressTrack(previous.left, !previous.right);
        }
    }

    private void progressTrack() {
        RailNode node = positiveDirection ? currentTrack.getEndNode() : currentTrack.getStartNode();

        NetworkNode.Direction next = controller.pickNextTrack(currentTrack, node);

        if (next == null) {
            trackScanTotalMillis = trackEndDistanceMillis;
            doStop = true;
            return;
        }

        boolean positiveDirection = node.equals(next.trackPiece.getStartNode());
        progressTrack(next.trackPiece, positiveDirection);
    }

    private void progressTrack(TrackPiece next, boolean positiveDirection) {
        // speed doesn't change by design
        this.positiveDirection = positiveDirection;
        this.currentTrack = next;

        float trackLength = next.getLength();
        trackStartDistanceMillis = trackEndDistanceMillis;
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

    private float getAccelerationToStopAfter(int millis, float speed) {
        // s = millis / METERS_TO_MILLIS
        // s = 0.5 * a * t * t
        // s = 0.5 * (breakForce + maxForce) * x * 1/mass * (v * mass) / ((breakForce + maxForce) * x) * (v * mass) / ((breakForce + maxForce) * x)
        // s = (0.5 * v * v * mass) / ((breakForce + maxForce) * x)
        // s * (breakForce + maxForce) * x = (0.5 * v * v * mass)
        // x = (0.5 * v * v * mass) / (s * (breakForce + maxForce))
        return -(0.5f * METERS_TO_MILLIS * speed * speed) / (millis * (breakForce + maxForce) * invMass);
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
}
