package NG.Tracks;

import NG.DataStructures.MatrixStack.SGL;
import NG.GameState.GameState;
import NG.Mods.Mod;
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

        void drawCircle(SGL gl, Vector2fc center, float radius, float startRadian, float endRadian, GameState gameState);

        void drawStraight(SGL gl, Vector2fc startCoord, float length, Vector2fc direction, GameState gameState);
    }
}
