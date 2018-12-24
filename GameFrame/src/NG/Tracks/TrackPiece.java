package NG.Tracks;

import NG.Engine.Game;
import NG.Entities.Entity;
import NG.Tools.Logger;
import org.joml.Vector2f;
import org.joml.Vector2fc;

/**
 * @author Geert van Ieperen. Created on 18-9-2018.
 */
public interface TrackPiece extends Entity {

    NetworkNodePoint getStartNodePoint();

    NetworkNodePoint getEndNodePoint();

    /**
     * gives the direction of the track at the endNodePoint. This should be equal to {@link #getEndNodePoint()} . {@link
     * NetworkNodePoint#getNode()} . {@link NetworkNode#getDirection()}
     * @return the direction of the last point on the track.
     */
    Vector2fc getEndDirection();

    /**
     * gives the direction of the track at the startNodePoint. This should be equal to {@link #getStartNodePoint()} .
     * {@link NetworkNodePoint#getNode()} . {@link NetworkNode#getDirection()}
     * @return the direction of the first point on the track.
     */
    Vector2fc getStartDirection();

    /**
     * @param nodePoint on of the two node points on this track
     * @return the associated direction of the track on that point
     */
    default Vector2fc getDirectionOf(NetworkNodePoint nodePoint) {
        if (nodePoint.equals(getStartNodePoint())) {
            return getStartDirection();
        } else {
            assert nodePoint.equals(getEndNodePoint());
            return getEndDirection();
        }
    }

    /**
     * factory method for creating an arbitrary track between two points
     * @param game       the game instance
     * @param type       the track type
     * @param aPoint     a position coordinate A on the map
     * @param aDirection the direction D of the track in point A, pointing towards B
     * @param bPoint     a position coordinate B on the map
     * @return a track that describes a track from A to B with direction D in point A
     */
    static TrackPiece getTrackPiece(
            Game game, TrackMod.TrackType type, NetworkNodePoint aPoint, Vector2fc aDirection, NetworkNodePoint bPoint
    ) {
        Vector2f relPosB = aPoint.vectorTo(bPoint);
        if (relPosB.lengthSquared() < 1e-6) {
            Logger.ASSERT.print("Created track of length 0");
            return new StraightTrack(game, type, aPoint, bPoint);
        }

        Vector2f direction = new Vector2f(aDirection).normalize();
        Vector2f vecToB = relPosB.normalize(); // overwrites relPosB
        float dot = vecToB.dot(direction);
//        assert dot >= 0 : "Direction must point toward the end point. Circle parts longer than a half are not accepted";
        if (dot < 0) direction.negate();

        if (dot > 0.99f) {
            Logger.DEBUG.print("Creating straight track", "dot = " + dot);
            return new StraightTrack(game, type, aPoint, bPoint);

        } else {
            return new CircleTrack(game, type, aPoint, direction, bPoint);
        }
    }

}
