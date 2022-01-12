package NG.Rendering;

/**
 * @author Geert van Ieperen created on 1-10-2020.
 */
public interface IntersectionTester {
    /**
     * Test whether the given axis-aligned box is partly or completely within or outside of the frustum defined by
     * <code>this</code> frustum culler. The box is specified via its min and max corner coordinates.
     * <p>
     * The algorithm implemented by this method is conservative. This means that in certain circumstances a <i>false
     * positive</i> can occur, when the method returns <code>-1</code> for boxes that are actually not visible/do not
     * intersect the frustum. See <a href="http://iquilezles.org/www/articles/frustumcorrect/frustumcorrect.htm">iquilezles.org</a>
     * for an examination of this problem.
     * <p>
     * Reference: <a href="http://old.cescg.org/CESCG-2002/DSykoraJJelinek/">Efficient View Frustum Culling</a>
     * @param minX the x-coordinate of the minimum corner
     * @param minY the y-coordinate of the minimum corner
     * @param minZ the z-coordinate of the minimum corner
     * @param maxX the x-coordinate of the maximum corner
     * @param maxY the y-coordinate of the maximum corner
     * @param maxZ the z-coordinate of the maximum corner
     * @return <code>true</code> if the axis-aligned box is completely or partly inside of the frustum;
     * <code>false</code> otherwise
     */
    boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ);
}
