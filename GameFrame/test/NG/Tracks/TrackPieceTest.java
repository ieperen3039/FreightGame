package NG.Tracks;

import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.junit.Test;

/**
 * @author Geert van Ieperen created on 24-12-2018.
 */
public class TrackPieceTest {

    @Test
    public void circleTest() {
        NetworkNodePoint startPoint = new NetworkNodePoint(new Vector2f(26.2f, 47.5f));
        Vector2fc startDirection = new Vector2f(2426, -1674).normalize();
        NetworkNodePoint endPoint = new NetworkNodePoint(new Vector2f(39.1f, 43.7f));

        CircleTrack trackPiece = new CircleTrack(null, null, startPoint, startDirection, endPoint);

        trackPiece.testAssumptions(startDirection);
    }
}
