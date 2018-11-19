package NG.Rendering.Shapes;

import NG.Tools.Toolbox;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author Geert van Ieperen created on 17-11-2017.
 */
public class FlatMesh extends AbstractMesh {

    /**
     * Creates a mesh from the given data. This may only be called on the main thread. VERY IMPORTANT that you have
     * first called {@link GL#createCapabilities()} (or similar) for openGL 3 or higher.
     * @param posList    a list of vertices
     * @param normList   a list of normal vectors
     * @param facesList  a list of faces, where each face refers to indices from posList and normList
     */
    public FlatMesh(List<Vector3f> posList, List<Vector3f> normList, List<Face> facesList) {
        final int nOfEdges = facesList.get(0).size();

        // Create position array in the order it has been declared. faces have (nOfEdges) vertices of 3 indices
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

    /**
     * creates a mesh without loading it to the GPU. This is useful for methods generating the mesh off the main thread,
     * such that the main thread can load them to the GPU at a later stage.
     * @param posList   a list of vertices
     * @param normList  a list of normal vectors
     * @param facesList a list of faces, where each face refers to indices from posList and normList
     * @return a prepared mesh, where the get() method will load the mesh to the GPU and return the resulting Mesh.
     */
    public static Supplier<AbstractMesh> createDelayed(List<Vector3f> posList, List<Vector3f> normList, List<Face> facesList) {
        AbstractMesh delayed = new FlatMesh();
        final int nOfEdges = facesList.get(0).size();

        // Create position array in the order it has been declared. faces have (faces.size()) vertices of 3 indices
        float[] posArr = new float[facesList.size() * 3 * nOfEdges];
        float[] normArr = new float[facesList.size() * 3 * nOfEdges];

        for (int i = 0; i < facesList.size(); i++) {
            Face face = facesList.get(i);
            readFaceVertex(face, posList, i, posArr);
            readFaceNormals(face, normList, i, normArr);
        }

        return () -> {
            delayed.writeToGL(posArr, normArr);
            loadedMeshes.add(delayed);
            return delayed;
        };
    }

    private static void readFaceVertex(Face face, List<Vector3f> posList, int faceNumber, float[] posArr) {
        int indices = faceNumber * face.size();
        for (int i = 0; i < face.size(); i++) {
            readVector(indices + i, posList, posArr, face.vert[i]);
        }
    }

    private static void readFaceNormals(Face face, List<Vector3f> normList, int faceNumber, float[] normArr) {
        int indices = faceNumber * face.size();
        for (int i = 0; i < face.size(); i++) {
            readVector(indices + i, normList, normArr, face.norm[i]);
        }
    }

    private static void readVector(int vectorNumber, List<Vector3f> sourceList, float[] targetArray, int index) {
        Vector3f vertex = sourceList.get(index);
        int arrayPosition = vectorNumber * 3;
        targetArray[arrayPosition] = vertex.x();
        targetArray[arrayPosition + 1] = vertex.y();
        targetArray[arrayPosition + 2] = vertex.z();
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

    /**
     * allows for an empty mesh
     */
    private FlatMesh() {
    }

}
