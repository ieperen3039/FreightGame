package NG.GameState;

import NG.DataStructures.MatrixStack.SGL;
import NG.Engine.GameAspect;
import org.joml.Vector2fc;
import org.joml.Vector3f;

/**
 * @author Geert van Ieperen. Created on 29-9-2018.
 */
public interface GameMap extends GameAspect {
    /**
     * generate a map using the provided generator. This method should be run in a separate thread
     * @param mapGenerator the generator to use for this map.
     */
    void generateNew(MapGeneratorMod mapGenerator);

    /**
     * @param position a position in (x, y) coordinates
     * @return the height of the ground above z=0 on that position, such that vector (x, y, z) lies on the map
     */
    float getHeightAt(Vector2fc position);

    /**
     * maps a 2D map coordinate to a 3D position. \result.x == mapCoord.x && result.y == mapCoord.y
     * @param mapCoord a 2D map coordinate
     * @return the 2D coordinate mapped to the surface of the inital map (or with z == 0 if no map is loaded)
     */
    default Vector3f getPosition(Vector2fc mapCoord) {
        return new Vector3f(mapCoord, getHeightAt(mapCoord));
    }

    /**
     * draws the map on the screen.
     * @param gl the gl object to draw with
     */
    void draw(SGL gl);
}
