package NG.Rendering.Shapes;

import NG.DataStructures.MatrixStack.Renderable;
import NG.DataStructures.MatrixStack.SGL;
import NG.Tools.Logger;
import NG.Tools.Toolbox;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * @author Geert van Ieperen created on 17-11-2017.
 */
public class Mesh implements Renderable {
    private static Queue<Mesh> loadedMeshes = new ArrayDeque<>(20);
    public static Mesh EMPTY_MESH = new EmptyMesh();

    private final int drawMethod;
    private int vaoId;
    private int vertexCount;
    private int posVboID;
    private int normVboID;

    /**
     * VERY IMPORTANT that you have first called GLFW windowhints (or similar) for openGL 3 or higher.
     */
    public Mesh(List<Vector3f> posList, List<Vector3f> normList, List<Face> facesList, int drawMethod) {
        final int nOfEdges = nOfEdges(drawMethod);
        this.drawMethod = drawMethod;

        // Create position array in the order it has been declared. faces have 3 vertices of 3 indices
        float[] posArr = new float[facesList.size() * 3 * nOfEdges];
        float[] normArr = new float[facesList.size() * 3 * nOfEdges];

        for (int i = 0; i < facesList.size(); i++) {
            Face face = facesList.get(i);
            readFaceVertex(face, posList, i, posArr);
            readFaceNormals(face, normList, i, normArr);
        }

        writeToGL(posArr, normArr);
        loadedMeshes.add(this);
    }

    private static int nOfEdges(int drawMethod) {
        switch (drawMethod) {
            case GL_TRIANGLES:
                return 3;
            case GL_QUADS:
                return 4;
        }
        Logger.ERROR.print("Could not determine number of edges of draw method " + glGetString(drawMethod));
        return 3;
    }

    private void readFaceVertex(Face face, List<Vector3f> posList, int faceNumber, float[] posArr) {
        int indices = faceNumber * face.size();
        for (int i = 0; i < face.size(); i++) {
            readVector(indices + i, posList, posArr, face.vert[i]);
        }
    }

    private void readFaceNormals(Face face, List<Vector3f> normList, int faceNumber, float[] normArr) {
        int indices = faceNumber * face.size();
        for (int i = 0; i < face.size(); i++) {

            readVector(indices + i, normList, normArr, face.norm[i]);
        }
    }

    private static void readVector(int vectorNumber, List<? extends Vector3f> sourceList, float[] targetArray, int index) {
        Vector3f vertex = sourceList.get(index);
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

        glDrawArrays(drawMethod, 0, vertexCount);

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
    }

    /** allows for an empty mesh */
    private Mesh() {
        drawMethod = 0;
    }

    /**
     * a record class to describe a plane by indices
     */
    public static class Face {
        int[] vert;
        int[] norm;

        /**
         * a description of a plane. The parameters should alternate pairs of (vertex, normal)
         */
        public Face(int[] vertices, int[] normals) {
            vert = vertices;
            norm = normals;
        }

        public Face(int[] vertices, int nInd) {
            vert = vertices;
            norm = new int[]{nInd, nInd, nInd};
        }

        public int size() {
            return vert.length;
        }
    }

    /**
     * an error replacement
     */
    private static class EmptyMesh extends Mesh {
        public EmptyMesh() {
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
