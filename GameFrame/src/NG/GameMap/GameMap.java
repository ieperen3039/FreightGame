package NG.GameMap;

import NG.Core.Game;
import NG.InputHandling.MouseTools.MouseTool;
import NG.Rendering.MatrixStack.SGL;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.io.Externalizable;

/**
 * @author Geert van Ieperen created on 5-5-2019.
 */
public interface GameMap extends Externalizable {
    float ACCEPTABLE_DIFFERENCE = 1 / 128f;

    /**
     * generate a map using the provided generator. This method can be run in a separate thread
     * @param game
     * @param mapGenerator the generator to use for this map.
     */
    void generateNew(Game game, MapGeneratorMod mapGenerator);

    /**
     * calculates the exact height of a given real position
     * @param x the x position
     * @param y the y position
     * @return the real height at the given position, or 0 when it is out of bounds.
     */
    float getHeightAt(float x, float y);

    /**
     * draws the map on the screen.
     * @param gl the gl object to draw with
     */
    void draw(SGL gl);

    /**
     * allows objects to listen for when this map is changed, as a result of {@link #generateNew(Game, MapGeneratorMod)}
     * or possibly an internal reason. Any call to {@link #draw(SGL)}, {@link #getHeightAt(float, float)} etc. will
     * represent the new values as soon as this callback is activated.
     * @param listener the object to notify
     */
    void addChangeListener(ChangeListener listener);

    /**
     * the number of coordinates in x and y direction. The real (floating-point) size can be completely different.
     */
    Vector2ic getSize();

    /**
     * checks whether an input click can be handled by this object
     * @param tool the current mouse tool
     * @param xSc  the screen x position of the mouse
     * @param ySc  the screen y position of the mouse
     * @return true iff the click has been handled by this object
     */
    boolean checkMouseClick(MouseTool tool, int xSc, int ySc, Vector3f origin, Vector3f direction);

    /**
     * calculates the lowest fraction t such that (origin + direction * t) lies on this map, for 0 <= t < 1.
     * @param origin    a local origin of a ray
     * @param direction the direction of the ray
     * @return fraction t of (origin + direction * t), or null if it does not hit.
     */
    Float gridMapIntersection(Vector3fc origin, Vector3fc direction);

    default boolean isOnFloor(Vector3fc position) {
        return Math.abs(getHeightAt(position.x(), position.y()) - position.z()) < ACCEPTABLE_DIFFERENCE;
    }

    void cleanup();

    /** increases the x or y coordinate in the given direction */
    static void expandCoord(Vector2i coordinate, Vector3f direction) {
        if (Math.abs(direction.x) > Math.abs(direction.y)) {
            if (direction.x > 0) {
                coordinate.x++;
            } else {
                coordinate.x--;
            }
        } else {
            if (direction.y > 0) {
                coordinate.y++;
            } else {
                coordinate.y--;
            }
        }
    }

    interface ChangeListener {
        /** is called when the map is changed */
        void onMapChange();
    }
}
