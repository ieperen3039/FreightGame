package NG.Rendering.Shapes;

import NG.Rendering.MatrixStack.Mesh;
import NG.Rendering.MatrixStack.SGL;
import NG.Tools.Toolbox;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.Queue;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * @author Geert van Ieperen created on 1-2-2019.
 */
public abstract class AbstractMesh implements Mesh {
    protected static Queue<AbstractMesh> loadedMeshes = new ArrayDeque<>();

    private int vaoId = 0;
    private int elementCount = 0;
    private int[] VBOIndices;
    private int indexBuffer = 0;
    private int indicesCount = 0;

    public void render(SGL.Painter lock) {
        glBindVertexArray(vaoId);

        // enable all non-null attributes
        for (int i = 0; i < VBOIndices.length; i++) {
            if (VBOIndices[i] != 0) {
                glEnableVertexAttribArray(i);
            }
        }

        if (indexBuffer == 0) {
            // draw the regular way
            glDrawArrays(GL_TRIANGLES, 0, elementCount);

        } else {
            // draw using an index buffer
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
            glDrawElements(GL_TRIANGLES, indicesCount, GL_UNSIGNED_INT, 0);
        }

        // disable all enabled attributes
        for (int i = 0; i < VBOIndices.length; i++) {
            if (VBOIndices[i] != 0) {
                glDisableVertexAttribArray(i);
            }
        }

        glBindVertexArray(0);
    }

    /**
     * loads an index array for indexed rendering
     * @param indices an array of indices, which isnot modified nor cached.
     */
    public void createIndexBuffer(int[] indices) {
        indexBuffer = glGenBuffers();
        indicesCount = indices.length;

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
    }

    /**
     * Initiates the creation of a mesh on the GPU.
     */
    protected void createVAO() {
        assert vaoId == 0;
        vaoId = glGenVertexArrays();
        loadedMeshes.add(this);
    }

    public int getVAO() {
        return vaoId;
    }

    public int getElementCount() {
        return elementCount;
    }

    protected void setElementCount(int elementCount) {
        assert this.elementCount == 0;
        this.elementCount = elementCount;
    }

    protected void createVBOTable(int nrOfIndices) {
        assert VBOIndices == null;
        this.VBOIndices = new int[nrOfIndices];
    }

    public int[] getVBOTable() {
        return VBOIndices;
    }

    public void dispose() {
        glDisableVertexAttribArray(0);
        // Delete the VBOs
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        for (int vbo : VBOIndices) {
            glDeleteBuffers(vbo);
        }

        // Delete the VAO
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);

        loadedMeshes.remove(this);
        vaoId = 0;
    }

    /**
     * all meshes that have been written to the GPU will be removed
     */
    public static void cleanAll() {
        while (!loadedMeshes.isEmpty()) {
            loadedMeshes.peek().dispose();
        }
        Toolbox.checkGLError();
    }

    /**
     * Creates a buffer object to transfer data to the GPU. The reference to the resulting VBO is placed at
     * VBOIndices[index].
     * @param data  data to transfer
     * @param index index of the VBO
     * @param size  number of elements in each attribute
     */
    @SuppressWarnings("Duplicates")
    public void createVBO(float[] data, int index, int size) {
        if (index < 0 || index >= VBOIndices.length) {
            throw new IndexOutOfBoundsException(
                    "Given index out of bounds: " + index + " on size " + VBOIndices.length);
        }

//        FloatBuffer buffer = FloatBuffer.wrap(data);
        FloatBuffer buffer = MemoryUtil.memAllocFloat(data.length);
        buffer.put(data).flip();

        try {
            int vboId = glGenBuffers();

            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
            glVertexAttribPointer(index, size, GL_FLOAT, false, 0, 0);

            VBOIndices[index] = vboId;

        } finally {
            MemoryUtil.memFree(buffer);
        }
    }

    /**
     * Creates a buffer object to transfer data to the GPU. The reference to the resulting VBO is placed at
     * VBOIndices[index].
     * @param data  data to transfer
     * @param index index of the VBO
     * @param size  number of elements in each attribute
     * @throws IndexOutOfBoundsException if index is out of bounds of VBO; if (index < 0 || index >= VBOIndices.length)
     */
    @SuppressWarnings("Duplicates")
    public void createVBO(int[] data, int index, int size) {
        if (index < 0 || index >= VBOIndices.length) {
            throw new IndexOutOfBoundsException(
                    "Given index out of bounds: " + index + " on size " + VBOIndices.length);
        }

//        IntBuffer buffer = IntBuffer.wrap(data);
        IntBuffer buffer = MemoryUtil.memAllocInt(data.length);
        buffer.put(data).flip();

        try {
            int vboId = glGenBuffers();

            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
            glVertexAttribPointer(index, size, GL_FLOAT, false, 0, 0);

            VBOIndices[index] = vboId;

        } finally {
            MemoryUtil.memFree(buffer);
        }
    }
}
