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

    /**
     * Given that this track is based on an initial direction and two positions, returns the direction at the unknown
     * side of the track.
     * @return the direction of the last point on the track.
     */
    Vector2fc getEndDirection();

    /**
     * factory method for creating an arbitrary track between two points
     * @param game       the game instance
     * @param type       the track type
     * @param aPosition  a position coordinate A on the map
     * @param bPosition  a position coordinate B on the map
     * @param aDirection the direction D of the track in point A, pointing towards B
     * @return a track that describes a track from A to B with direction D in point A
     */
    static TrackPiece getTrackPiece(
            Game game, TrackMod.TrackType type, Vector2fc aPosition, Vector2fc aDirection, Vector2fc bPosition
    ) {
        if (aPosition.distanceSquared(bPosition) < 1e-6) {
            // TODO add null-sized track
            Logger.ASSERT.print("Created track of length 0");
            return new StraightTrack(game, type, aPosition, bPosition);
        }

        Vector2f direction = new Vector2f(aDirection).normalize();
        Vector2f relPosB = new Vector2f(bPosition).sub(aPosition);
        float dot = relPosB.dot(direction);
        assert dot >= 0 : "Direction must point toward the end point. Circle parts longer than a half are not accepted";

        if (dot > 0.99f) {
            return new StraightTrack(game, type, aPosition, bPosition);

        } else {
            return new CircleTrack(game, type, aPosition, direction, bPosition);
        }
    }
}
