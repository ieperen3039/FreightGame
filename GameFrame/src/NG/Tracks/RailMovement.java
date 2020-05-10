package NG.Tracks;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.DataStructures.Generic.BlockingTimedArrayQueue;
import NG.DataStructures.Generic.Pair;
import NG.DataStructures.Interpolation.FloatInterpolator;
import NG.Entities.Locomotive;
import NG.Network.RailNode;
import NG.Tools.Toolbox;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * A
 * @author Geert van Ieperen created on 6-5-2020.
 */
public class RailMovement extends AbstractGameObject {
    private static final float MAX_DELTA_TIME = 0.05f;
    private final Locomotive controller;

    /**
     * real train speed in direction of facing. Formally: If positiveDirection, change in trackDistance. If
     * !trackDistance, inverse of change in trackDistance
     */
    private float speed = 0;

    private TrackPiece currentTrack; // track the train is currently on
    private BlockingTimedArrayQueue<Pair<TrackPiece, Boolean>> tracks; // includes currentTrack
    private float lastUpdateTime;

    private float currentDistance; // distance travelled on the current track, from startNode to endNode
    private FloatInterpolator distances;

    private boolean positiveDirection = true; // whether the train faces in positive track direction
    private float acceleration = 0;

    public RailMovement(
            Game game, Locomotive controller, float spawnTime, TrackPiece startPiece, float fraction,
            boolean positiveDirection
    ) {
        super(game);
        this.currentTrack = startPiece;
        this.controller = controller;
        this.currentDistance = startPiece.getLength() * fraction;

        this.distances = new FloatInterpolator(0, currentDistance, spawnTime);
        this.tracks = new BlockingTimedArrayQueue<>(0);
        tracks.add(new Pair<>(startPiece, positiveDirection), spawnTime);

        this.lastUpdateTime = spawnTime;
    }

    public void update() {
        update(game.timer().getGametime());
    }

    public void update(float gameTime) {
        if (gameTime < lastUpdateTime) return;

        float time = lastUpdateTime + MAX_DELTA_TIME;
        while (time < gameTime) {
            stepTo(time);
            time += MAX_DELTA_TIME;
        }
        stepTo(gameTime);
    }

    private void stepTo(float gameTime) {
        while (true) {
            float deltaTime = gameTime - lastUpdateTime;
            // s = vt + at^2
            float movement = speed * deltaTime + acceleration * deltaTime * deltaTime;
            float trackLength = currentTrack.getLength();
            float newDistance = currentDistance + ((positiveDirection) ? movement : -movement);

            RailNode node;
            float endOfTrackTime;

            if (newDistance < 0) {
                node = currentTrack.getStartNode();

                float fraction = Toolbox.getFraction(currentDistance, newDistance, 0);
                endOfTrackTime = gameTime - deltaTime * (1 - fraction);
                distances.add(0f, endOfTrackTime);

            } else if (newDistance > trackLength) {
                node = currentTrack.getEndNode();

                float fraction = Toolbox.getFraction(currentDistance, newDistance, trackLength);
                endOfTrackTime = gameTime - deltaTime * (1 - fraction);
                distances.add(trackLength, endOfTrackTime);

            } else {
                currentDistance = newDistance;
                distances.add(currentDistance, gameTime);
                // speed update after movement update
                speed += acceleration * deltaTime;
                lastUpdateTime = gameTime;
                return;
            }

            assert node != null;
            // find next track to enter
            List<RailNode.Direction> options = node.getNext(currentTrack);
            if (options.isEmpty()) {
                speed = 0;
                acceleration = 0;
                return; // full stop
            }

            RailNode.Direction next = controller.pickNextTrack(options);

            // now set everything as if last update was at endOfTrackTime
            // speed doesn't change by design
            // when speed is negative, we consider a positive direction when starting from endNode
            positiveDirection = node.equals(next.trackPiece.getStartNode()) == (speed > 0);

            currentTrack = next.trackPiece;
            trackLength = next.trackPiece.getLength();
            tracks.add(new Pair<>(next.trackPiece, positiveDirection), endOfTrackTime);

            if (positiveDirection) {
                currentDistance = 0;
                distances.add(0f, endOfTrackTime);

            } else {
                currentDistance = trackLength;
                distances.add(trackLength, endOfTrackTime);
            }

            lastUpdateTime = endOfTrackTime;
        }
    }

    public void setAcceleration(float a) {
        acceleration = a;
    }

    public float getSpeed() {
        return speed;
    }

    /**
     * @return the interpolated position on the given time
     */
    public Vector3f getPosition(float time) {
        update(time);

        TrackPiece track = tracks.getActive(time).left;
        float distance = distances.getInterpolated(time);

        float fractionTravelled = distance / track.getLength();
        return track.getPositionFromFraction(fractionTravelled);
    }

    /**
     * @return the interpolated direction of movement (derivative of position) on the given time
     */
    public Vector3f getDirection(float time) {
        update(time);

        Pair<TrackPiece, Boolean> activeTrack = tracks.getActive(time);
        TrackPiece track = activeTrack.left;
        float distance = distances.getInterpolated(time);

        float fractionTravelled = distance / track.getLength();
        Vector3f directionOfTrack = track.getDirectionFromFraction(fractionTravelled);

        Boolean isPositive = activeTrack.right;
        if (!isPositive) directionOfTrack.negate();
        directionOfTrack.normalize();

        return directionOfTrack;
    }

    public Quaternionf getRotation(float time) {
//        update(time); // included in getDirection(time)
        Vector3f direction = getDirection(time).normalize();

        float yawAngle = Math.atan2(direction.y, direction.x);
        float hzMovement = Math.sqrt(direction.x * direction.x + direction.y * direction.y);
        float pitchAngle = Math.atan2(direction.z, hzMovement);

        return new Quaternionf()
                .rotateAxis(pitchAngle, 1, 0, 0)
                .rotateAxis(yawAngle, 0, 0, 1);
    }
}
