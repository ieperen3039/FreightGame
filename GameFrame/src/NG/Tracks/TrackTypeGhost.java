package NG.Tracks;

import NG.DataStructures.Generic.Color4f;
import NG.Entities.Entity;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shapes.CustomShape;
import org.joml.Math;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 29-4-2020.
 */
public class TrackTypeGhost implements TrackType {
    public static final float WIDTH = 0.5f;
    public static final float HEIGHT = 0.1f;
    public static final float RESOLUTION = 1;
    private final TrackType source;

    public TrackTypeGhost(TrackType source) {
        this.source = source;
    }

    @Override
    public Mesh generateCircle(float radius, float angle, float endHeight) {
        float hDelta = endHeight / (angle * radius);
        float length = Math.abs(radius * angle);
        int resolution = (int) Math.max(RESOLUTION * length, Math.abs((8 / Math.PI) * angle));

        CustomShape frame = TrackType.generateFunctional(
                t -> new Vector3f(radius * Math.cos(angle * t), radius * Math.sin(angle * t), endHeight * t),
                t -> new Vector3f(-Math.sin(angle * t), Math.cos(angle * t), hDelta).normalize(),
                WIDTH, HEIGHT, resolution
        );

        return frame.toFlatMesh();
    }

    @Override
    public Mesh generateStraight(Vector3fc displacement) {
        float length = displacement.length();

        CustomShape frame = TrackType.generateFunctional(
                t -> new Vector3f(displacement).mul(t),
                t -> new Vector3f(displacement).div(length),
                WIDTH, HEIGHT, (int) (length * RESOLUTION)
        );

        return frame.toFlatMesh();
    }

    @Override
    public void setMaterial(
            MaterialShader shader, TrackPiece track, Entity.Marking marking
    ) {
        Color4f color = Color4f.BLACK;

        if (marking.isValid()) {
            color = marking.color;

        } else if (track instanceof CircleTrack) {
            float radius = ((CircleTrack) track).getRadius();
            if (getMaximumSpeed(radius) == 0) {
                color = Color4f.RED;
            }
        }

        shader.setMaterial(new Color4f(1, 1, 1, 0.5f), color, 0);
    }

    @Override
    public float getMaximumSpeed() {
        return source.getMaximumSpeed();
    }
}
