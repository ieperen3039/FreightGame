package NG.Rendering.Shapes;

import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.MeshLoading.MeshFile;
import NG.Rendering.Shapes.Primitives.Plane;
import NG.Resources.GeneratorResource;
import NG.Resources.Resource;
import NG.Tools.Directory;
import org.joml.AABBf;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Collection;

/**
 * A collection of generic shapes
 * @author Geert van Ieperen. Created on 14-9-2018.
 */
public enum GenericShapes implements Mesh, Shape {
    ARROW("arrow.obj"),
    ICOSAHEDRON("icosahedron.obj"),
    INV_CUBE("inverseCube.obj"),
    CUBE("cube.obj"),
    TEXTURED_QUAD("quad.obj"),

    /** a quad of size 2x2 on the xy plane */
    QUAD(createQuad()),
    ;

    private Resource<Mesh> mesh;
    private Shape shape; // we use the actual shape as this is an enum

    GenericShapes(String... path) {
        Resource<MeshFile> pars = MeshFile.createResource(Directory.meshes, path);
        shape = pars.get().getShape();
        mesh = Resource.derive(pars, MeshFile::getMesh, Mesh::dispose);
    }

    GenericShapes(CustomShape frame) {
        shape = frame.toShape();
        mesh = new GeneratorResource<>(frame::toFlatMesh, Mesh::dispose);
    }

    public Resource<Mesh> meshResource() {
        return mesh;
    }

    public Resource<Shape> shapeResource() {
        return new GeneratorResource<>(() -> shape, null);
    }

    @Override
    public void render(SGL.Painter lock) {
        mesh.get().render(lock);
    }

    @Override
    public void dispose() {
        mesh.drop();
    }

    @Override
    public Collection<? extends Plane> getPlanes() {
        return shape.getPlanes();
    }

    @Override
    public Collection<Vector3fc> getPoints() {
        return shape.getPoints();
    }

    @Override
    public AABBf getBoundingBox() {
        return shape.getBoundingBox();
    }

    private static CustomShape createQuad() {
        CustomShape frame = new CustomShape();
        frame.addQuad(
                new Vector3f(1, 1, 0),
                new Vector3f(-1, 1, 0),
                new Vector3f(-1, -1, 0),
                new Vector3f(1, -1, 0),
                new Vector3f(0, 0, 1)
        );
        return frame;
    }

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
