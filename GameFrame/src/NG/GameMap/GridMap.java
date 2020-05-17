package NG.GameMap;

import NG.InputHandling.MouseTools.MouseTool;
import NG.Tools.Logger;
import org.joml.Math;
import org.joml.*;

/**
 * An object that represents the world where all other entities stand on. This includes both the graphical and the
 * physical representation. The map considers a difference between coordinates and position, in that a coordinate may be
 * of different magnitude than an equivalent position.
 * @author Geert van Ieperen. Created on 29-9-2018.
 */
public abstract class GridMap implements GameMap {
    public static final float EPSILON = 1f / (1 << 16);

    @Override
    public boolean checkMouseClick(MouseTool tool, int xSc, int ySc, Vector3f origin, Vector3f direction) {
        Float t = gridMapIntersection(origin, direction);
        if (t == null) return false;

        Vector3f position = new Vector3f(direction).mul(t).add(origin);
        tool.apply(position, xSc, ySc);
        return true;
    }

    @Override
    public Float gridMapIntersection(Vector3fc origin, Vector3fc direction) {
        Vector2ic size = getCoordinateSize();
        // edge case, direction == (0, 0, dz)
        if (direction.x() == 0 && direction.y() == 0) {
            Vector3f realSize = getPosition(size.x(), size.y());
            if (origin.x() < 0 || origin.y() < 0 || origin.x() > realSize.x || origin.y() > realSize.y) {
                return null;
            }

            Vector2i coord = getCoordinate(origin);
            Float sect = getTileIntersect(origin, direction, coord.x, coord.y);
            if (sect == null || sect < 0 || sect > 1) {
                return null;
            } else {
                return sect;
            }
        }

        Vector2f coordPos = getCoordPosf(origin);
        Vector2f coordDir = getCoordDirf(direction).normalize();

        Vector2f worldClip = new Vector2f();
        boolean isOnWorld = Intersectionf.intersectRayAab(
                coordPos.x, coordPos.y, 0,
                coordDir.x, coordDir.y, 0,
                0, 0, -1,
                size.x(), size.y(), 1,
                worldClip
        );

        if (!isOnWorld) return null;

        if (worldClip.x > 0) {
            coordPos.add(new Vector2f(coordDir).mul(worldClip.x + EPSILON));

        } else {
            // check this tile before setting up voxel ray casting
            Float secFrac = getTileIntersect(origin, direction, (int) coordPos.x, (int) coordPos.y);
            if (secFrac < 1) {
                return secFrac;
            }
        }

        boolean xIsPos = coordDir.x > 0;
        boolean yIsPos = coordDir.y > 0;

        // next t of x border
        float pointX = xIsPos ? Math.ceil(coordPos.x) : Math.floor(coordPos.x);
        float normalX = (xIsPos ? -1 : 1);
        float denomX = normalX * coordDir.x;
        float tNextX = (denomX > -EPSILON) ? 0.0f : ((pointX - coordPos.x) * normalX) / denomX;

        // next t of y border
        float pointY = yIsPos ? Math.ceil(coordPos.y) : Math.floor(coordPos.y);
        float normalY = (yIsPos ? -1 : 1);
        float denomY = normalY * coordDir.y;
        float tNextY = (denomY > -EPSILON) ? 0.0f : ((pointY - coordPos.y) * normalY) / denomY;

        int xCoord = (int) coordPos.x;
        int yCoord = (int) coordPos.y;

        final int dx = (coordDir.x == 0) ? 0 : (coordDir.x > 0 ? 1 : -1); // [-1, 0, 1]
        final int dy = (coordDir.y == 0) ? 0 : (coordDir.y > 0 ? 1 : -1); // [-1, 0, 1]

        final float dtx = (coordDir.x == 0 ? Float.POSITIVE_INFINITY : (dx / coordDir.x));
        final float dty = (coordDir.y == 0 ? Float.POSITIVE_INFINITY : (dy / coordDir.y));

        while (xCoord >= 0 && yCoord >= 0 && xCoord < size.x() && yCoord < size.y()) {
            Float secFrac = getTileIntersect(origin, direction, xCoord, yCoord);

            if (secFrac == null) {
                Logger.ASSERT.printf("got (%d, %d) which is out of bounds", xCoord, yCoord);
                return null;

            } else if (secFrac < 1) {
                assert secFrac > 0;
                return secFrac;
            }

            if (tNextX < tNextY) {
                tNextX = tNextX + dtx;
                xCoord = xCoord + dx;
            } else {
                tNextY = tNextY + dty;
                yCoord = yCoord + dy;
            }
        }

        return null;
    }

    /**
     * maps a real position to the nearest coordinate. This coordinate may not exist.
     * @param position a position in real space
     * @return the coordinate that is closest to the given position.
     * @see #getPosition(int, int)
     */
    public abstract Vector2i getCoordinate(Vector3fc position);

    /**
     * maps a coordinate to a real position
     * @param x an x-coordinate
     * @param y an y-coordinate
     * @return a vector such that {@link #getCoordinate(Vector3fc)} will result in {@code x} and {@code y}
     * @see #getCoordinate(Vector3fc)
     */
    public abstract Vector3f getPosition(int x, int y);

    /** returns the floating-point transformation to coordinates */
    public abstract Vector2f getCoordPosf(Vector3fc position);

    /** returns the floating-point transformation to coordinates */
    public abstract Vector2f getCoordDirf(Vector3fc direction);

    /**
     * computes the intersection of a ray on the given coordinate
     * @param origin    the origin of the ray in real space
     * @param direction the direction of the ray
     * @param xCoord    the x coordinate
     * @param yCoord    the y coordinate
     * @return the first intersection of the ray with this tile, {@link Float#POSITIVE_INFINITY} if it does not hit and
     * null if the given coordinate is not on the map.
     */
    abstract Float getTileIntersect(Vector3fc origin, Vector3fc direction, int xCoord, int yCoord);

    /**
     * the number of voxels in x and y direction. The real (floating-point) size can be completely different.
     */
    public abstract Vector2ic getCoordinateSize();
}
