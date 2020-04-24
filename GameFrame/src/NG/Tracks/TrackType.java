package NG.Tracks;

import NG.GameState.GameMap;
import NG.Rendering.MatrixStack.SGL;
import org.joml.Vector2fc;

/**
 * A specific type of track where trains can move along. This is implemented as a graphical representation of the
 * track.
 *///TODO extend this
public interface TrackType {
    /** @return a name for this track type */
    String name();

    /**
     * draw a partially circular track on the given parameters.
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
