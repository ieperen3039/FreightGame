package NG.Tracks;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.DataStructures.Generic.BlockingTimedArrayQueue;
import NG.DataStructures.Generic.Pair;
import NG.DataStructures.Interpolation.FloatInterpolator;
import NG.DataStructures.Interpolation.LongInterpolator;
import NG.Entities.Train;
import NG.Network.RailNode;
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
    private float speed = 0;
    private float acceleration = 0;

    private double nextUpdateTime;
    private long currentTotalMillis; // (1000 * the real distance) : data type is adequate for almost a lightyear distance
    private long trackEndDistanceMillis;
    private TrackPiece currentTrack; // track the train is currently on

    private boolean positiveDirection; // whether the train faces in positive track direction

    private LongInterpolator totalMillimeters;
    private FloatInterpolator totalToLocalDistance; // maps total distance to track distance
    private BlockingTimedArrayQueue<Pair<TrackPiece, Boolean>> tracks; // maps total distance to track, includes currentTrack
    private long trackStartDistanceMillis;

    public RailMovement(
            Game game, Train controller, double spawnTime, TrackPiece startPiece, float fraction,
            boolean positiveDirection, float initialSpeed
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
        this.speed = initialSpeed;
    }

    public void update() {
        double gametime = game.timer().getGameTime();
        update(gametime);
    }

    public void update(double gameTime) {
        while (nextUpdateTime < gameTime) {

            // s = vt + at^2 // movement in meters
            float movement = speed * DELTA_TIME + acceleration * DELTA_TIME * DELTA_TIME;
            if (movement < 0) { // when the train reverses
                totalMillimeters.add(currentTotalMillis, nextUpdateTime);
                speed = acceleration * DELTA_TIME; // only progresses when acceleration is positive
                nextUpdateTime += DELTA_TIME;
                continue;
            }

            currentTotalMillis += movement * METERS_TO_MILLIS;
            totalMillimeters.add(currentTotalMillis, nextUpdateTime);
            // speed update after movement update
            speed += acceleration * DELTA_TIME;

            while (currentTotalMillis > trackEndDistanceMillis) {
                progressTrack();
            }

            nextUpdateTime += DELTA_TIME;
        }
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

        while (currentTotalMillis > trackEndDistanceMillis) {
            Pair<TrackPiece, Boolean> previous = tracks.getPrevious(oldTrackStartMillis);
            oldTrackStartMillis -= tracks.timeSincePrevious(oldTrackStartMillis);
            progressTrack(previous.left, !previous.right);
        }
    }

    private void progressTrack() {
        RailNode node = positiveDirection ? currentTrack.getEndNode() : currentTrack.getStartNode();

        RailNode.Direction next = controller.pickNextTrack(currentTrack, node);

        if (next == null) {
            speed = 0;
            acceleration = 0;
            currentTotalMillis = trackEndDistanceMillis;
            return; // full stop
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

    public void setAcceleration(float a) {
        acceleration = a;
    }

    /**
     * @return reals speed in meters per second
     */
    public float getSpeed() {
        return Math.abs(speed);
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
