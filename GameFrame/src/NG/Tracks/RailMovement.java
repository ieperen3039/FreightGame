package NG.Tracks;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.DataStructures.Generic.BlockingTimedArrayQueue;
import NG.DataStructures.Interpolation.FloatInterpolator;
import NG.Entities.Locomotive;
import NG.Network.RailNode;
import NG.Tools.Logger;
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
    private final Locomotive controller;

    /**
     * real train speed in direction of facing. Formally: If positiveDirection, change in trackDistance. If
     * !trackDistance, inverse of change in trackDistance
     */
    private float speed = 0;

    private TrackPiece currentTrack; // track the train is currently on
    private BlockingTimedArrayQueue<TrackPiece> tracks; // includes currentTrack

    private float currentDistance; // distance travelled on the current track, from startNode to endNode
    private FloatInterpolator distances;

    private boolean positiveDirection = true; // whether the train faces in positive track direction
    private float acceleration = 0;

    public RailMovement(Game game, Locomotive controller, float spawnTime, TrackPiece startPiece, float fraction) {
        super(game);
        this.currentTrack = startPiece;
        this.controller = controller;
        this.currentDistance = startPiece.getLength() * fraction;

        this.distances = new FloatInterpolator(0, currentDistance, spawnTime);
        this.tracks = new BlockingTimedArrayQueue<>(0);
        tracks.add(startPiece, spawnTime);
    }

    public void update() {
        float gameTime = game.timer().getGametime();
        float deltaTime = game.timer().getGametimeDifference();

        float movement = speed * deltaTime;
        speed += acceleration * deltaTime; // speed update after movement update
        float newDistance = currentDistance + ((positiveDirection) ? movement : -movement);

        float previousTrackLength = currentTrack.getLength();

        RailNode node;
        float endOfTrackTime;

        if (newDistance < 0) {
            node = currentTrack.getStartNode();

            float fraction = Toolbox.getFraction(currentDistance, newDistance, 0);
            endOfTrackTime = gameTime - deltaTime * (1 - fraction);
            Logger.WARN.print(fraction, gameTime, endOfTrackTime);
            distances.add(0f, endOfTrackTime);

            // set positive for later computations
            newDistance = -newDistance;

        } else if (newDistance > previousTrackLength) {
            node = currentTrack.getEndNode();

            float fraction = Toolbox.getFraction(currentDistance, newDistance, previousTrackLength);
            endOfTrackTime = gameTime - deltaTime * (1 - fraction);
            distances.add(previousTrackLength, endOfTrackTime);

            newDistance = newDistance - previousTrackLength;

        } else {
            currentDistance = newDistance;
            distances.add(currentDistance, gameTime);
            return;
        }

        assert node != null;
        // find next track to enter
        List<RailNode.Direction> options = node.getNext(currentTrack);
        if (options.isEmpty()) {
            speed = 0;
            acceleration = 0;
            return;
        }

        RailNode.Direction next = controller.pickNextTrack(options);

        // speed doesn't change by design
        // when speed is negative, we consider a positive direction when starting from endNode
        positiveDirection = node.equals(next.trackPiece.getStartNode()) == (speed > 0);

        assert newDistance > 0 && newDistance < previousTrackLength;
        if (positiveDirection) {
            currentDistance = newDistance;
            distances.add(0f, endOfTrackTime);

        } else {
            float trackPieceLength = next.trackPiece.getLength();
            currentDistance = trackPieceLength - newDistance;
            distances.add(trackPieceLength, endOfTrackTime);
        }

        currentTrack = next.trackPiece;
        tracks.add(currentTrack, endOfTrackTime);
        distances.add(currentDistance, gameTime);
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
        TrackPiece track = tracks.getActive(time);
        float distance = distances.getInterpolated(time);

        float fractionTravelled = distance / track.getLength();
        return track.getPositionFromFraction(fractionTravelled);
    }

    /**
     * @return the interpolated direction of movement (derivative of position) on the given time
     */
    public Vector3f getDirection(float time) {
        TrackPiece track = tracks.getActive(time);
        float distance = distances.getInterpolated(time);

        float fractionTravelled = distance / track.getLength();
        Vector3f directionOfTrack = track.getDirectionFromFraction(fractionTravelled);

        if (!positiveDirection) directionOfTrack.negate();
        directionOfTrack.normalize(speed);

        return directionOfTrack;
    }

    public Quaternionf getRotation(float time) {
        Vector3f direction = getDirection(time);

        float yawAngle = Math.atan2(direction.y, direction.x);
        float hzMovement = Math.sqrt(direction.x * direction.x + direction.y + direction.y);
        float pitchAngle = Math.atan2(direction.z, hzMovement);

        return new Quaternionf()
                .rotateAxis(pitchAngle, 1, 0, 0)
                .rotateAxis(yawAngle, 0, 0, 1);
    }
}
