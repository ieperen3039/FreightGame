package NG.Mods;

import NG.Tracks.TrackPiece;
import org.joml.Vector2fc;

import java.util.List;

/**
 * @author Geert van Ieperen. Created on 19-9-2018.
 */
public interface TrackMod extends Mod {

    /**
     * @return a list of all types of tracks to be created
     * @implNote The list is not modified. Advised is to cache the list and make it unmodifiable
     */
    List<TrackType> getTypes();

    interface TrackType {
        /**
         * @return a name that represents the track created by this class's {@link #createNew(Vector2fc, Vector2fc,
         *         Vector2fc)} method
         */
        String getTypeName();

        /**
         * creates a piece of track of this type, with the guarantee that the returned object is no longer modified by
         * this object
         * @param startCoord     the map coordinate where this track piece starts
         * @param startDirection the direction in map coordinates where this piece should start with
         * @param endCoord       the endpoint that has to be met by this trackpiece, regardless of direction.
         * @return a new TrackPiece object
         */
        TrackPiece createNew(Vector2fc startCoord, Vector2fc startDirection, Vector2fc endCoord);

        /**
         * creates a piece of track of this type, possibly reusing a cached version of the trackpiece
         * @param startPosition
         * @param startDirection
         * @param endPoint
         * @return
         */
        TrackPiece concept(Vector2fc startPosition, Vector2fc startDirection, Vector2fc endPoint);
    }
}
