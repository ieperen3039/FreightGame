package NG.Rendering.Shapes;

import NG.Rendering.MatrixStack.Mesh;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * defines a custom, static object shape
 * <p>
 * Created by Geert van Ieperen on 1-3-2017.
 */
public class CustomShape {

    private final boolean invertMiddle;
    private final Map<Vector3fc, Integer> points;
    private final List<Vector3fc> normals;
    private final List<Mesh.Face> faces;
    private Vector3fc middle;

    /**
     * custom shape with middle on (0, 0, 0) and non-inverted
     * @see #CustomShape(Vector3fc, boolean)
     */
    public CustomShape() {
        this(Vectors.zeroVector());
    }

    /**
     * @param middle the middle of this object.
     * @see #CustomShape(Vector3fc, boolean)
     */
    public CustomShape(Vector3fc middle) {
        this(middle, false);
    }

    /**
     * A shape that may be defined by the client code using methods of this class. When the shape is finished, call
     * {@link #asMesh()} to load it into the GPU. The returned shape should be re-used as a static mesh for any future
     * calls to such shape.
     * @param middle the middle of this object. More specifically, from this point, all normal vectors point outward
     *               except maybe for those that have their normal explicitly defined.
     */
    public CustomShape(Vector3fc middle, boolean invertMiddle) {
        this.middle = middle;
        this.faces = new ArrayList<>();
        this.points = new Hashtable<>();
        this.normals = new ArrayList<>();
        this.invertMiddle = invertMiddle;
    }

    /**
     * defines a quad in rotational order. The vectors do not have to be given clockwise
     * @param A      (0, 0)
     * @param B      (0, 1)
     * @param C      (1, 1)
     * @param D      (1, 0)
     * @param normal the direction of the normal of this plane
     * @throws NullPointerException if any of the vectors is null
     */
    public void addQuad(Vector3fc A, Vector3fc B, Vector3fc C, Vector3fc D, Vector3fc normal) {
        Vector3f currentNormal = Vectors.getNormalVector(A, B, C);

        if (currentNormal.dot(normal) >= 0) {
            addFinalQuad(A, B, C, D, currentNormal);
        } else {
            currentNormal.negate();
            addFinalQuad(D, C, B, A, currentNormal);
        }
    }

    /** a quad in rotational, counterclockwise order */
    private void addFinalQuad(Vector3fc A, Vector3fc B, Vector3fc C, Vector3fc D, Vector3fc normal) {
        int aInd = addHitpoint(A);
        int bInd = addHitpoint(B);
        int cInd = addHitpoint(C);
        int dInd = addHitpoint(D);
        int nInd = addNormal(normal);
        faces.add(new Mesh.Face(new int[]{aInd, bInd, cInd, dInd}, nInd));
    }

    /**
     * defines a quad that is mirrored over the xz-plane
     * @see CustomShape#addFinalQuad(Vector3fc, Vector3fc, Vector3fc, Vector3fc, Vector3fc)
     */
    public void addQuad(Vector3fc A, Vector3fc B) {
        addQuad(A, B, mirrorY(B, new Vector3f()), mirrorY(A, new Vector3f()));
    }

    /**
     * @see CustomShape#addQuad(Vector3fc, Vector3fc, Vector3fc, Vector3fc, Vector3fc)
     */
    public void addQuad(Vector3fc A, Vector3fc B, Vector3fc C, Vector3fc D) {
        Vector3f normal = Vectors.getNormalVector(A, B, C);

        final Vector3f direction = new Vector3f(B).sub(middle);

        if ((normal.dot(direction) >= 0) != invertMiddle) {
            addFinalQuad(A, B, C, D, normal);
        } else {
            normal.negate();
            addFinalQuad(D, C, B, A, normal);
        }
    }

    /**
     * Adds a quad which is mirrored in the XZ-plane
     * @see #addQuad(Vector3fc, Vector3fc, Vector3fc, Vector3fc, Vector3fc)
     */
    public void addMirrorQuad(Vector3fc A, Vector3fc B, Vector3fc C, Vector3fc D) {
        addQuad(A, B, C, D);
        addQuad(
                mirrorY(A, new Vector3f()),
                mirrorY(B, new Vector3f()),
                mirrorY(C, new Vector3f()),
                mirrorY(D, new Vector3f())
        );
    }

    /**
     * @see CustomShape#addFinalTriangle(Vector3fc, Vector3fc, Vector3fc, Vector3fc)
     */
    public void addTriangle(Vector3fc A, Vector3fc B, Vector3fc C) {
        Vector3f normal = Vectors.getNormalVector(A, B, C);
        final Vector3f direction = new Vector3f(B).sub(middle);

        if ((normal.dot(direction) >= 0) != invertMiddle) {
            addFinalTriangle(A, B, C, normal);
        } else {
            normal.negate();
            addFinalTriangle(C, B, A, normal);
        }
    }


    public void addTriangle(Vector3fc A, Vector3fc B, Vector3fc C, Vector3fc normal) {
        Vector3f currentNormal = Vectors.getNormalVector(A, B, C);

        if (currentNormal.dot(normal) >= 0) {
            addFinalTriangle(A, B, C, currentNormal);
        } else {
            currentNormal.negate();
            addFinalTriangle(C, B, A, currentNormal);
        }
    }

    /**
     * defines a triangle with the given points in counterclockwise ordering
     * @see CustomShape#addQuad(Vector3fc, Vector3fc, Vector3fc, Vector3fc)
     */
    private void addFinalTriangle(Vector3fc A, Vector3fc B, Vector3fc C, Vector3fc normal) {
        int aInd = addHitpoint(A);
        int bInd = addHitpoint(B);
        int cInd = addHitpoint(C);
        int nInd = addNormal(normal);
        faces.add(new Mesh.Face(new int[]{aInd, bInd, cInd}, nInd));
    }

    private int addNormal(Vector3fc normal) {
        if ((normal == null) || normal.equals(Vectors.zeroVector())) {
            throw new IllegalArgumentException("Customshape.addNormal(Vector3fc): invalid normal: " + normal);
        }

        normals.add(normal);
        return normals.size() - 1;
    }

    /**
     * stores a vector in the collection, and returns its resulting position
     * @param vector
     * @return index of the vector
     */
    private int addHitpoint(Vector3fc vector) {
        points.putIfAbsent(vector, points.size());
        return points.get(vector);
    }

    /**
     * Adds a triangle which is mirrored in the XZ-plane
     */
    public void addMirrorTriangle(Vector3fc A, Vector3fc B, Vector3fc C) {
        addTriangle(A, B, C);
        addTriangle(mirrorY(A, new Vector3f()), mirrorY(B, new Vector3f()), mirrorY(C, new Vector3f()));
    }

    /**
     * Adds a triangle which is mirrored in the XZ-plane, where the defined triangle has a normal in the given
     * direction
     */
    public void addMirrorTriangle(Vector3fc A, Vector3fc B, Vector3fc C, Vector3fc normal) {
        addTriangle(A, B, C, normal);
        Vector3f otherNormal = normal.negate(new Vector3f());
        addTriangle(mirrorY(A, new Vector3f()), mirrorY(B, new Vector3f()), mirrorY(C, new Vector3f()), otherNormal);
    }

    private Vector3f mirrorY(Vector3fc target, Vector3f dest) {
        dest.set(target.x(), -target.y(), target.z());
        return dest;
    }

    /**
     * adds a strip as separate quad objects
     * @param quads an array of 2n+4 vertices defining quads as {@link #addQuad(Vector3fc, Vector3fc, Vector3fc,
     *              Vector3fc)} for every natural number n.
     */
    public void addStrip(Vector3f... quads) {
        final int inputSize = quads.length;
        if (((inputSize % 2) != 0) || (inputSize < 4)) {
            throw new IllegalArgumentException(
                    "input arguments can not be of odd length or less than 4 (length is " + inputSize + ")");
        }

        for (int i = 4; i < inputSize; i += 2) {
            // create quad as [1, 2, 4, 3], as rotational order is required
            addQuad(quads[i - 4], quads[i - 3], quads[i - 1], quads[i - 2]);
        }
    }

    /**
     * convert this object into a Mesh
     * @return a hardware-accelerated Mesh object
     */
    public Mesh asMesh() {
        return new FlatMesh(getSortedVertices(), normals, faces);
    }

    private List<Vector3fc> getSortedVertices() {
        // this is the most clear, structured way of the duplicate-vector problem. maybe not the most efficient.
        Vector3fc[] sortedVertices = new Vector3f[points.size()];
        points.forEach((v, i) -> sortedVertices[i] = v);

        return Arrays.asList(sortedVertices);
    }

    /**
     * writes an object to the given filename
     * @param filename
     * @throws IOException if any problem occurs while creating the file
     */
    public void writeOBJFile(String filename) throws IOException {
        PrintWriter writer = new PrintWriter(filename, StandardCharsets.UTF_8);

        writer.println("# created using a simple obj writer by Geert van Ieperen");
        writer.println("# calling method: " + Logger.getCallingMethod(2));
        writer.println("mtllib arrow.mtl"); // TODO is this necessary?

        List<Vector3fc> sortedVertices = getSortedVertices();

        for (Vector3fc vec : sortedVertices) {
            writer.println(String.format(Locale.US, "v %1.09f %1.09f %1.09f", vec.x(), vec.z(), vec.y()));
        }

        for (Vector3fc vec : normals) {
            writer.println(String.format(Locale.US, "vn %1.09f %1.09f %1.09f", vec.x(), vec.z(), vec.y()));
        }

        writer.println("usemtl None");
        writer.println("s off");
        writer.println("");

        for (Mesh.Face face : faces) {
            writer.print("f ");
            for (int i = 0; i < face.vert.length; i++) {
                writer.print(" " + vertexToString(face.vert[i], face.norm[i]));
            }
            writer.println();
        }

        writer.close();

        Logger.DEBUG.print("Successfully created obj file: " + filename);
    }

    public void setMiddle(Vector3f middle) {
        this.middle = middle;
    }

    @Override
    public String toString() {
        return getSortedVertices().toString();
    }

    public Shape wrapToShape() {
        return new BasicShape(getSortedVertices(), normals, faces);
    }

    /**
     * Adds an arbitrary polygon to the object. For correct rendering, the plane should be flat
     * @param normal the direction of the normal of this plane. When null, it is calculated using the middle
     * @param edges  the edges of this plane
     */
    public void addPlane(Vector3fc normal, Vector3fc... edges) {
        switch (edges.length) {
            case 3:
                if (normal == null) {
                    addTriangle(edges[0], edges[1], edges[2]);
                } else {
                    addTriangle(edges[0], edges[1], edges[2], normal);
                }
                return;
            case 4:
                if (normal == null) {
                    addQuad(edges[0], edges[1], edges[2], edges[3]);
                } else {
                    addQuad(edges[0], edges[1], edges[2], edges[3], normal);
                }
                return;
        }
        for (int i = 1; i < (edges.length - 2); i++) {
            if (normal == null) {
                addTriangle(edges[i], edges[i + 1], edges[i + 2]);
            } else {
                addTriangle(edges[i], edges[i + 1], edges[i + 2], normal);
            }
        }
    }

    private static String vertexToString(int vertex, int normal) {
        return String.format("%d//%d", vertex + 1, normal + 1);
    }
}