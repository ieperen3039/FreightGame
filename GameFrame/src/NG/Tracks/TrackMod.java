package NG.Tracks;

import NG.DataStructures.MatrixStack.SGL;
import NG.GameState.GameMap;
import NG.Mods.Mod;
import org.joml.Vector2fc;

import java.util.Collection;

/**
 * @author Geert van Ieperen. Created on 19-9-2018.
 */
public interface TrackMod extends Mod {

    /**
     * @return a list of all types of tracks to be created
     * @implNote The list should not be modified. Advised is to cache the list and make it unmodifiable
     */
    Collection<TrackType> getTypes();

    interface TrackType {

        /** @return a name for this track */
        String getTypeName();

        /**
         * draw a partially circular track on the given parameters.
         *
         * @param gl         the object for drawing
         * @param center     the center of the circle, given as a (x, y) coordinate
         * @param radius     the radius of the circle where the track lies on
         * @param lowerTheta the angle in radian where the circle part starts
         * @param angle      the total angle in radians that this circle describes.
         * @param map        the map that provides information about the terrain, e.g. with {@link
         *                   GameMap#getHeightAt(Vector2fc)}
         */
        void drawCircle(SGL gl, Vector2fc center, float radius, float lowerTheta, float angle, GameMap map);

        /**
         * draw a partially straight track on the given parameters.
         *
         * @param gl         the object for drawing
         * @param startCoord the coordinate where this track origins from
         * @param direction  the normalized direction of where the track goes to
         * @param length     the length of the track, such that (startCoord + (direction * length) == endCoord) where
         *                   endCoord is the end of this line of tracks
         * @param map        the map that provides information about the terrain, e.g. with {@link
         *                   GameMap#getHeightAt(Vector2fc)}
         */
        void drawStraight(SGL gl, Vector2fc startCoord, Vector2fc direction, float length, GameMap map);
    }
}
