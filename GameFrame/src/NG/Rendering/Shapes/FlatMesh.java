package NG.Rendering.Shapes;

import NG.Rendering.MatrixStack.Mesh;
import NG.Rendering.MatrixStack.SGL;
import NG.Tools.Toolbox;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * @author Geert van Ieperen created on 17-11-2017.
 */
public class FlatMesh implements Mesh {
    /**
     * an empty mesh, not bound to any resources.
     */
    public static final FlatMesh EMPTY_MESH = new EmptyMesh();

    private static Queue<FlatMesh> loadedMeshes = new ArrayDeque<>(20);

    private int vaoId;
    private int vertexCount;
    private int posVboID;
    private int normVboID;

    /**
     * Creates a mesh from the given data. This may only be called on the main thread. VERY IMPORTANT that you have
     * first called {@link GL#createCapabilities()} (or similar) for openGL 3 or higher.
     * @param posList    a list of vertices
     * @param normList   a list of normal vectors
     * @param facesList  a list of faces, where each face refers to indices from posList and normList
     */
    public FlatMesh(List<? extends Vector3fc> posList, List<? extends Vector3fc> normList, List<Mesh.Face> facesList) {
        final int nOfEdges = facesList.get(0).size();

        // Create position array in the order it has been declared. faces have (nOfEdges) vertices of 3 indices
        float[] posArr = new float[facesList.size() * 3 * nOfEdges];
        float[] normArr = new float[facesList.size() * 3 * nOfEdges];

        for (int i = 0; i < facesList.size(); i++) {
            Mesh.Face face = facesList.get(i);
            readFaceVertex(face, posList, i, posArr);
            readFaceNormals(face, normList, i, normArr);
        }

        writeToGL(posArr, normArr);
        loadedMeshes.add(this);
    }

    /**
     * creates a mesh without loading it to the GPU. This is useful for methods generating the mesh off the main thread,
     * such that the main thread can load them to the GPU at a later stage.
     * @param posList   a list of vertices
     * @param normList  a list of normal vectors
     * @param facesList a list of faces, where each face refers to indices from posList and normList
     * @return a prepared mesh, where the get() method will load the mesh to the GPU and return the resulting Mesh.
     */
    public static Supplier<FlatMesh> createDelayed(
            List<Vector3fc> posList, List<Vector3fc> normList, List<Mesh.Face> facesList
    ) {
        FlatMesh delayed = new FlatMesh();
        final int nOfEdges = facesList.get(0).size();

        // Create position array in the order it has been declared. faces have (faces.size()) vertices of 3 indices
        float[] posArr = new float[facesList.size() * 3 * nOfEdges];
        float[] normArr = new float[facesList.size() * 3 * nOfEdges];

        for (int i = 0; i < facesList.size(); i++) {
            Mesh.Face face = facesList.get(i);
            readFaceVertex(face, posList, i, posArr);
            readFaceNormals(face, normList, i, normArr);
        }

        return () -> {
            delayed.writeToGL(posArr, normArr);
            loadedMeshes.add(delayed);
            return delayed;
        };
    }

    /**
     * creates a Mesh of a section of the given heightmap. Note that the xEnd value should not be larger than
     * (heightmap.length - 1), same for yEnd.
     * @param heightmap the heightmap, giving the height of a virtual (x, y) coordinate
     * @param xStart    the lowest x index to consider, inclusive
     * @param xEnd      the the highest x index to consider, inclusive.
     * @param yStart    the lowest y index to consider, inclusive
     * @param yEnd      the the highest y index to consider, inclusive.
     * @param edgeSize  the distance between two vertices in real coordinates. Multiplying a virtual coordinate with
     *                  this value gives the real coordinate.
     * @return a mesh of the heightmap, using quads, positioned in absolute coordinates. (no transformation is needed)
     */
    public static Supplier<FlatMesh> meshFromHeightmap(float[][] heightmap, int xStart, int xEnd, int yStart, int yEnd, float edgeSize) {
        int nOfXFaces = xEnd - xStart;
        int nOfYFaces = yEnd - yStart;
        int nOfVertices = (nOfXFaces + 1) * (nOfYFaces + 1);

        // vertices and normals
        List<Vector3fc> vertices = new ArrayList<>(nOfVertices);
        List<Vector3fc> normals = new ArrayList<>(nOfVertices);

        for (int y = yStart; y <= yEnd; y++) {
            for (int x = xStart; x <= xEnd; x++) {
                // vertex
                float height = heightmap[x][y];
                Vector3f vertex = new Vector3f(
                        x * edgeSize,
                        y * edgeSize,
                        height
                );
                vertices.add(vertex);

                // normal
                Vector3f normal = new Vector3f(0, 0, 1);
                if ((x - 1 >= 0) && (y - 1 >= 0) && (x + 1 < heightmap.length) && (y + 1 < heightmap[x].length)) {
                    float dx = heightmap[x - 1][y] - heightmap[x + 1][y];
                    float dy = heightmap[x][y - 1] - heightmap[x][y + 1];
                    normal.x = dx / 2;
                    normal.y = dy / 2;
                }

                // no need for normalisation
                normals.add(normal);
            }
        }

        // faces
        int nOfQuads = nOfXFaces * nOfYFaces;
        int arrayXSize = xEnd - xStart + 1;
        List<Mesh.Face> faces = new ArrayList<>(nOfQuads);

        for (int y = 0; y < nOfYFaces; y++) {
            for (int x = 0; x < nOfXFaces; x++) {
                int left = y * arrayXSize + x;
                int right = (y + 1) * arrayXSize + x;

                faces.add(new Mesh.Face(
                        new int[]{left, right + 1, left + 1},
                        new int[]{left, right + 1, left + 1}
                ));
                faces.add(new Mesh.Face(
                        new int[]{left, right, right + 1},
                        new int[]{left, right, right + 1}
                ));
            }
        }

        return createDelayed(vertices, normals, faces);
    }

    private static void readFaceVertex(
            Mesh.Face face, List<? extends Vector3fc> posList, int faceNumber, float[] posArr
    ) {
        int indices = faceNumber * face.size();
        for (int i = 0; i < face.size(); i++) {
            readVector(indices + i, posList, posArr, face.vert[i]);
        }
    }

    private static void readFaceNormals(
            Mesh.Face face, List<? extends Vector3fc> normList, int faceNumber, float[] normArr
    ) {
        int indices = faceNumber * face.size();
        for (int i = 0; i < face.size(); i++) {
            readVector(indices + i, normList, normArr, face.norm[i]);
        }
    }

    private static void readVector(
            int vectorNumber, List<? extends Vector3fc> sourceList, float[] targetArray, int index
    ) {
        Vector3fc vertex = sourceList.get(index);
        int arrayPosition = vectorNumber * 3;
        targetArray[arrayPosition] = vertex.x();
        targetArray[arrayPosition + 1] = vertex.y();
        targetArray[arrayPosition + 2] = vertex.z();
    }


    /**
     * create a mesh and store it to the GL. For both lists it holds that the ith vertex has the ith normal Vector3f
     * @param positions the vertices, concatenated in groups of 3
     * @param normals   the normals, concatenated in groups of 3
     * @throws IllegalArgumentException if any of the arrays has length not divisible by 3
     * @throws IllegalArgumentException if the arrays are of unequal length
     */
    private void writeToGL(float[] positions, float[] normals) {
        if (((positions.length % 3) != 0) || (positions.length == 0)) {
            throw new IllegalArgumentException("received invalid position array of length " + positions.length + ".");
        } else if (normals.length != positions.length) {
            throw new IllegalArgumentException("received a normals array that is not as long as positions: " +
                    positions.length + " position values and " + normals.length + "normal values");
        } else if (vaoId != 0) {
            throw new IllegalStateException("Tried loading a mesh that was already loaded");
        }

        vertexCount = positions.length;
        FloatBuffer posBuffer = MemoryUtil.memAllocFloat(vertexCount);
        FloatBuffer normBuffer = MemoryUtil.memAllocFloat(vertexCount);

        try {
            posBuffer.put(positions).flip();
            normBuffer.put(normals).flip();

            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            // Position VBO
            posVboID = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, posVboID);
            glBufferData(GL_ARRAY_BUFFER, posBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

            // Vertex normals VBO
            normVboID = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, normVboID);
            glBufferData(GL_ARRAY_BUFFER, normBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);

            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);

        } finally {
            MemoryUtil.memFree(posBuffer);
            MemoryUtil.memFree(normBuffer);
        }
    }

    public void render(SGL.Painter lock) {
        glBindVertexArray(vaoId);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        glDrawArrays(GL_TRIANGLES, 0, vertexCount);

        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);
    }

    /**
     * all meshes that have been written to the GPU will be removed
     */
    @SuppressWarnings("ConstantConditions")
    public static void cleanAll() {
        while (!loadedMeshes.isEmpty()) {
            loadedMeshes.peek().dispose();
        }
        Toolbox.checkGLError();
    }

    public void dispose() {
        glDisableVertexAttribArray(0);

        glDeleteBuffers(posVboID);
        glDeleteBuffers(normVboID);

        // Delete the VAO
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);

        loadedMeshes.remove(this);
        vaoId = 0;
    }

    /**
     * allows for an empty mesh
     */
    private FlatMesh() {
    }

    private static class EmptyMesh extends FlatMesh {
        private EmptyMesh() {
            super();
        }

        @Override
        public void render(SGL.Painter lock) {
        }

        @Override
        public void dispose() {
        }
    }
}
