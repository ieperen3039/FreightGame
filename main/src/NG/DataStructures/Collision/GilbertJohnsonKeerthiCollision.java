package NG.DataStructures.Collision;

import NG.DataStructures.Generic.PairList;
import NG.Rendering.Shapes.Shape;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains just the GJK collision detection algorithm.
 * @author Geert van Ieperen created on 25-6-2020.
 */
public final class GilbertJohnsonKeerthiCollision {

    public static boolean checkCollision(ColliderEntity a, ColliderEntity b) {
        PairList<Shape, Matrix4fc> aShapes = a.getConvexCollisionShapes();
        PairList<Shape, Matrix4fc> bShapes = b.getConvexCollisionShapes();

        // pre-compute inverses of b transformation
        List<Matrix4f> bInverses = new ArrayList<>();
        for (int i = 0; i < bShapes.size(); i++) {
            bInverses.add(new Matrix4f(bShapes.right(i)).invert());
        }

        Vector3fc initialDirection = Vectors.Z;

        // check carthesian product of all shapes
        for (int i = 0; i < aShapes.size(); i++) {
            Shape aShape = aShapes.left(i);
            Matrix4fc aTransform = aShapes.right(i);
            Matrix4f aAntiTransform = new Matrix4f(aTransform).invert();

            for (int j = 0; j < bShapes.size(); j++) {
                Shape bShape = bShapes.left(j);
                Matrix4fc bTransform = bShapes.right(j);
                Matrix4f bAntiTransform = bInverses.get(j);

                boolean doesCollide = gjk(aShape, aTransform, aAntiTransform, bShape, bTransform, bAntiTransform, initialDirection);

                if (doesCollide) return true;
            }
        }

        return false;
    }

    public static boolean gjk(
            Shape aShape, Matrix4f aTransform, Shape bShape, Matrix4f bTransform, Vector3fc initialDirection
    ) {
        return gjk(
                aShape, aTransform, new Matrix4f(aTransform).invert(),
                bShape, bTransform, new Matrix4f(bTransform).invert(),
                initialDirection
        );
    }

    /**
     * The actual algorithm
     * @param aShape           a shape in local space
     * @param aTransform       the transformation to transform LOCAL to GLOBAL space of aShape
     * @param aAntiTransform   the transformation to transform GLOBAL to LOCAL space of aShape
     * @param bShape           a shape in local space
     * @param bTransform       the transformation to transform LOCAL to GLOBAL space of bShape
     * @param bAntiTransform   the transformation to transform GLOBAL to LOCAL space of bShape
     * @param initialDirection the initial iteration direction, a to b
     * @return true iff aShape and bShape collide in real space
     */
    public static boolean gjk(
            Shape aShape, Matrix4fc aTransform, Matrix4fc aAntiTransform, Shape bShape,
            Matrix4fc bTransform, Matrix4fc bAntiTransform,
            Vector3fc initialDirection
    ) {
        // case 1 : point
        Vector3fc A = support(initialDirection, aShape, aTransform, aAntiTransform, bShape, bTransform, bAntiTransform);
        Vector3f direction = new Vector3f(A).negate();

        // case 2 : line
        Vector3fc B = support(direction, aShape, aTransform, aAntiTransform, bShape, bTransform, bAntiTransform);
        if (!Vectors.inSameDirection(B, direction)) return false; // B is not behind the origin

        Vector3f AB = new Vector3f(B).sub(A);
        Vector3f BO = new Vector3f(B).negate();
        direction = new Vector3f(AB).cross(BO).cross(AB); // perpendicular to AB towards origin

        if (direction.lengthSquared() < 1 / 128f) return true; // edge case : origin lies on AB

        // case 3 : triangle
        Vector3fc newPoint = support(direction, aShape, aTransform, aAntiTransform, bShape, bTransform, bAntiTransform);
        if (!Vectors.inSameDirection(newPoint, direction)) return false; // new point is not behind the origin

        int i = 0;

        // case 4 : triangles and tetrahedrons
        Vector3fc[] points = {A, B, null, null};
        do {
            i++;
            direction = simplex(points, newPoint);
            if (direction == null) return true;

            newPoint = support(direction, aShape, aTransform, aAntiTransform, bShape, bTransform, bAntiTransform);

            if (i % 100_000 == 0) {
                Logger.WARN.print(i);
            }
        } while (Vectors.inSameDirection(newPoint, direction));

        // last iteration couldn't reach the origin
        return false;
    }

    private static Vector3f simplex(Vector3fc[] points, Vector3fc newPoint) {
        if (points[2] == null) {
            points[2] = newPoint;
            return triangleSimplex(points);
        } else {
            points[3] = newPoint;
            return tetrahedronSimplex(points);
        }
    }

    // returns the new scan direction given three points of which C is the last added
    private static Vector3f triangleSimplex(Vector3fc[] points) {
        Vector3fc A = points[0], B = points[1], C = points[2];
        assert points[3] == null;

        // we know the origin is not behind AB by construction
        Vector3f CO = new Vector3f(C).negate();
        Vector3f CB = new Vector3f(B).sub(C);
        Vector3f CA = new Vector3f(A).sub(C);
        Vector3f cPerpendicular = new Vector3f(CB).cross(CA);

        Vector3f caPerpendicular = new Vector3f(cPerpendicular).cross(CA);
        if (Vectors.inSameDirection(caPerpendicular, CO)) {
            assert !Vectors.inSameDirection(new Vector3f(CB).cross(cPerpendicular), CO);
            // origin lies closest to CA
            // points = {A, C}
            points[1] = C;
            points[2] = null;
            return new Vector3f(CA).cross(CO).cross(CA);

        } else {
            Vector3fc cbPerpendicular = new Vector3f(CB).cross(cPerpendicular);
            if (Vectors.inSameDirection(cbPerpendicular, CO)) {
                // origin lies closest to CB
                // points = {B, C}
                points[0] = B;
                points[1] = C;
                points[2] = null;
                return new Vector3f(CB).cross(CO).cross(CB);

            } else {
                if (Vectors.inSameDirection(cPerpendicular, CO)) {
                    // points = {A, B, C}
                    return cPerpendicular;

                } else {
                    // points = {A, C, B}
                    points[1] = C;
                    points[2] = B;
                    return cPerpendicular.negate();
                }
            }
        }
    }

    /**
     * @param points an array of {A, B, C, D} where CB.cross(CA) is in direction of D. points is modified by resetting
     *               it back to a triangle case.
     * @return the new scan direction, assuming points[3] is the last added, or null if the origin is contained.
     */
    private static Vector3f tetrahedronSimplex(Vector3fc[] points) {
        Vector3fc A = points[0], B = points[1], C = points[2], D = points[3];

        Vector3f DA = new Vector3f(A).sub(D);
        Vector3f DB = new Vector3f(B).sub(D);
        Vector3fc DO = new Vector3f(D).negate();

        // we know the origin is not behind ABC by construction
        assert !Vectors.inSameDirection(new Vector3f(B).sub(C).cross(new Vector3f(A).sub(C)), DO);

        Vector3f abdPerpendicular = new Vector3f(DB).cross(DA);
        if (Vectors.inSameDirection(abdPerpendicular, DO)) {
            // origin is behind ABD
            // points = {A, B, D}
            points[2] = D;
            return tetrahedronTriangle(DA, DB, DO, abdPerpendicular, points);

        } else {
            Vector3f DC = new Vector3f(C).sub(D);
            Vector3f acdPerpendicular = new Vector3f(DA).cross(DC);
            if (Vectors.inSameDirection(acdPerpendicular, DO)) {
                // origin is behind ADC
                // points = {C, A, D}
                points[0] = C;
                points[1] = A;
                points[2] = D;
                return tetrahedronTriangle(DC, DA, DO, acdPerpendicular, points);

            } else {
                Vector3f bcdPerpendicular = new Vector3f(DC).cross(DB);
                if (Vectors.inSameDirection(bcdPerpendicular, DO)) {
                    // origin is behind BCD
                    // points = {D, B, C}
                    points[0] = D;
                    return tetrahedronTriangle(DB, DC, DO, bcdPerpendicular, points);

                } else {
                    // tetrahedron contains origin
                    return null; // yay!
                }
            }
        }
    }

    /**
     * similar to triangleSimplex, but with (abcPerpendicular = CA.cross(CB)) in the same direction as CO.
     * @param points the array of triangle points, with points[0] = A, points[1] = B and points[2] = C. points[3] is
     *               ignored. This array is modified to reflect the new tetrahedron to consider
     */
    private static Vector3f tetrahedronTriangle(
            Vector3f CA, Vector3f CB, Vector3fc CO, Vector3f abcPerpendicular, Vector3fc[] points
    ) {
        // we know the origin is not behind ABC by construction
        assert Vectors.inSameDirection(abcPerpendicular, CO);

        Vector3f caPerpendicular = new Vector3f(abcPerpendicular).cross(CA);
        if (Vectors.inSameDirection(caPerpendicular, CO)) {
            assert !Vectors.inSameDirection(new Vector3f(CB).cross(abcPerpendicular), CO);
            // origin lies closest to CA
            // points = {A, C}
            Vector3fc C = points[2];
            points[1] = C;
            points[2] = null;
            points[3] = null;
            return new Vector3f(CA).cross(CO).cross(CA);

        } else {
            Vector3fc cbPerpendicular = new Vector3f(CB).cross(abcPerpendicular);
            if (Vectors.inSameDirection(cbPerpendicular, CO)) {
                // origin lies closest to CB
                // points = {B, C}
                Vector3fc B = points[1];
                Vector3fc C = points[2];
                points[0] = B;
                points[1] = C;
                points[2] = null;
                points[3] = null;
                return new Vector3f(CB).cross(CO).cross(CB);

            } else {
                // we know the origin is not behind ABC by construction
                points[3] = null;
                // points = {A, B, C}
                return abcPerpendicular;
            }
        }
    }

    private static Vector3fc support(
            Vector3fc direction, Shape a, Matrix4fc aTransform, Matrix4fc aAntiTransform, Shape b, Matrix4fc bTransform,
            Matrix4fc bAntiTransform
    ) {
        Vector3f aLocalDirection = new Vector3f(direction).mulDirection(aAntiTransform);
        Vector3fc supportPointA = a.getSupportPoint(aLocalDirection);

        Vector3f bLocalDirection = new Vector3f(direction).negate().mulDirection(bAntiTransform);
        Vector3fc supportPointB = b.getSupportPoint(bLocalDirection);

        Vector3f bGlobalSpace = new Vector3f(supportPointB).mulPosition(bTransform);
        Vector3f aGlobalSpace = new Vector3f(supportPointA).mulPosition(aTransform);
        return aGlobalSpace.sub(bGlobalSpace);
    }
}
