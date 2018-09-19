package NG.Tracks;

import NG.Entities.Entity;
import org.joml.Vector2fc;

/**
 * @author Geert van Ieperen. Created on 18-9-2018.
 */
public interface TrackPiece extends Entity {

    /**
     * @return the position of the last end of the track
     */
    Vector2fc getEndPosition();

    /**
     * @return the position of the first part of the track
     */
    Vector2fc getStartPosition();

    /**
     * @return the normalized direction along which the start of the track goes.
     */
    Vector2fc getStartDirection();

    /**
     * @return the normalized direction where the end of the track stops.
     */
    Vector2fc getEndDirection();
}
