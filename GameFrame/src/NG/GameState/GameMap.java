package NG.GameState;

import NG.Core.GameAspect;
import NG.InputHandling.MouseTools.MouseToolListener;
import NG.Rendering.MatrixStack.SGL;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * An object that represents the world where all other entities stand on. This includes both the graphical and the
 * physical representation.
 * @author Geert van Ieperen. Created on 29-9-2018.
 */
public interface GameMap extends GameAspect, MouseToolListener {
    void setMapGenerator(HeightMapGenerator mapGenerator);

    /**
     * generate a map using the provided generator. This method should be run in a separate thread
     */
    void generateNew();

    /**
     * @param position a position in (x, y) coordinates
     * @return the height of the ground above z=0 on that position, such that vector (x, y, z) lies on the map
     */
    default float getHeightAt(Vector2fc position) {
        return getHeightAt(position.x(), position.y());
    }

    /**
     * maps a 2D map coordinate to a 3D position. Returns a vector with z == 0 if no map is loaded.
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

    /**
     * returns a vector on the map that results from raytracing the given ray.
     * @param origin    the origin of the ray
     * @param direction the (un-normalized) direction of the ray
     * @return a vector p such that {@code p = origin + t * direction} for minimal t such that p lies on the map.
     */
    Vector3f intersectWithRay(Vector3fc origin, Vector3fc direction);

    /**
     * allows objects to listen for when this map is changed, as a result of {@link #generateNew(HeightMapGenerator)} or
     * possibly an internal reason. Any call to {@link #draw(SGL)}, {@link #getHeightAt(float, float)} etc. will
     * represent the new values as soon as this callback is activated.
     * @param listener the object to notify
     */
    void addChangeListener(ChangeListener listener);

    interface ChangeListener {
        /** is called when the map is changed */
        void onMapChange();
    }
}
