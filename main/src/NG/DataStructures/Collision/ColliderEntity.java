package NG.DataStructures.Collision;

import NG.DataStructures.Generic.PairList;
import NG.Entities.Entity;
import NG.Rendering.Shapes.Shape;
import org.joml.AABBf;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 24-6-2020.
 */
public interface ColliderEntity extends Entity {

    /**
     * returns an axis aligned box that completely encases this entity in world position. The returned value should not
     * change over time.
     * @return an axis-aligned bounding box
     */
    AABBf getHitbox();

    /**
     * returns a collection of convex shapes, each with a transformation such that the transformed shapes roughly make
     * up this entity.
     * @return a list of convex shapes and their transformations, transforming the given shape to world-space
     */
    PairList<Shape, Matrix4fc> getConvexCollisionShapes();

    default AABBf computeHitbox() {
        AABBf box = new AABBf();
        Vector3f temp = new Vector3f();

        getConvexCollisionShapes().forEach((s, t) -> {
            for (Vector3fc point : s.getPoints()) {
                box.union(temp.set(point).mulPosition(t));
            }
        });
        return box;
    }
}
