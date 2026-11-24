package io.github.ieperen3039.trainsinspace.Entities;

import io.github.ieperen3039.ngn.Rendering.MeshLoading.Mesh;
import io.github.ieperen3039.ngn.Rendering.Shapes.CustomShape;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class Shapes {
    /** creates a horizontal ring */
    public static Mesh createRing(float radius, int radialParts, float ringThiccness) {
        Vector3f orthogonal = new Vector3f(radius, 0, 0);

        float angle = (float) ((2 * Math.PI) / radialParts);
        AxisAngle4f axis = new AxisAngle4f(angle, 0, 0, 1);

        Vector3f[] ring = new Vector3f[radialParts];
        Vector3f[] expand = new Vector3f[radialParts];

        for (int j = 0; j < radialParts; j++) {
            ring[j] = new Vector3f(orthogonal);
            expand[j] = new Vector3f(orthogonal).normalize(ringThiccness);
            axis.transform(orthogonal);
        }

        CustomShape frame = new CustomShape();
        Vector3f up = new Vector3f(0, 0, ringThiccness);

        Vector3f BOF = new Vector3f();
        Vector3f BIF = new Vector3f();
        Vector3f BOB = new Vector3f(); // Look, it's bob!
        Vector3f BIB = new Vector3f();

        Vector3f ringMiddle = ring[radialParts - 1];
        Vector3f out = expand[radialParts - 1];
        ringMiddle.add(up, BOF).add(out, BOF);
        ringMiddle.add(up, BIF).sub(out, BIF);
        ringMiddle.sub(up, BOB).add(out, BOB);
        ringMiddle.sub(up, BIB).sub(out, BIB);

        for (int i = 0; i < ring.length; i++) {
            ringMiddle = ring[i];
            out = expand[i];
            // I = inner, O = outer, F = front, B = back
            Vector3f AOF = new Vector3f();
            Vector3f AIF = new Vector3f();
            Vector3f AOB = new Vector3f();
            Vector3f AIB = new Vector3f();

            ringMiddle.add(up, AOF).add(out, AOF);
            ringMiddle.add(up, AIF).sub(out, AIF);
            ringMiddle.sub(up, AOB).add(out, AOB);
            ringMiddle.sub(up, AIB).sub(out, AIB);

            frame.addQuad(AIF, BIF, BOF, AOF, new Vector3f(0, 0, 1));
            frame.addQuad(AOF, BOF, BOB, AOB, out);
            frame.addQuad(AOB, BOB, BIB, AIB, new Vector3f(0, 0, -1));
            out.negate();
            frame.addQuad(AIF, BIF, BIB, AIB, out);

            BOF = AOF;
            BIF = AIF;
            BOB = AOB;
            BIB = AIB;
        }

        return frame.toFlatMesh();
    }
}
