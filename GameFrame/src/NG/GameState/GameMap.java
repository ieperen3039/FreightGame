package NG.GameState;

import NG.ActionHandling.MouseTools.MouseToolListener;
import NG.Engine.GameAspect;
import NG.Rendering.MatrixStack.SGL;
import org.joml.Vector2fc;
import org.joml.Vector3f;

/**
 * @author Geert van Ieperen. Created on 29-9-2018.
 */
public interface GameMap extends GameAspect, MouseToolListener {
    /**
     * generate a map using the provided generator. This method should be run in a separate thread
     * @param mapGenerator the generator to use for this map.
     */
    void generateNew(MapGeneratorMod mapGenerator);

    /**
     * @param position a position in (x, y) coordinates
     * @return the height of the ground above z=0 on that position, such that vector (x, y, z) lies on the map
     */
    default float getHeightAt(Vector2fc position) {
        return getHeightAt(position.x(), position.y());
    }

    /**
     * maps a 2D map coordinate to a 3D position.
     * Returns a vector with z == 0 if no map is loaded.
     * <p>{@code \result.x == mapCoord.x && result.y == mapCoord.y}</p>
     * @param mapCoord a 2D map coordinate
     * @return the 2D coordinate mapped to the surface of the inital map.
     */
    default Vector3f getPosition(Vector2fc mapCoord) {
        return new Vector3f(mapCoord, getHeightAt(mapCoord));
    }

    /**
     * draws the map on the screen.
     * @param gl the gl object to draw with
     */
    void draw(SGL gl);

    /**
     * @param x an exact x position on the map
     * @param y an exact y position on the map
     * @return the height at position (x, y) on the map
     */
    float getHeightAt(float x, float y);
}
