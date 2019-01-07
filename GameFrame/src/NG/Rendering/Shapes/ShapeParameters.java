package NG.Rendering.Shapes;

import NG.DataStructures.Pair;
import NG.Rendering.MatrixStack.Mesh;
import NG.Tools.Directory;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Geert van Ieperen created on 6-5-2018.
 */
public class ShapeParameters {
    public List<Vector3fc> vertices;
    public List<Vector3fc> normals;
    public List<Mesh.Face> faces;
    public final String name;

    /**
     * calls {@link #ShapeParameters(Vector3f, float, Path, String)} on the file of the given path without offset and
     * scale of 1
     * @param fileName path to the .obj file. Extension should NOT be included
     */
    public ShapeParameters(String[] fileName) {
        this(Vectors.zeroVector(), 1f, Directory.meshes.getPath(fileName), fileName[fileName.length - 1]);
    }

    /**
     * @param offSet offset of the gravity middle in this mesh as the negative of the vector to the gravity middle
     * @param scale  the scaling applied to the loaded object
     * @param path   the path to the object
     * @param name   debug name of the shape
     */
    public ShapeParameters(Vector3f offSet, float scale, Path path, String name) {
        this.name = name;
        vertices = new ArrayList<>();
        normals = new ArrayList<>();
        faces = new ArrayList<>();

        List<String> lines = openMesh(path);

        for (String line : lines) {
            String[] tokens = line.split("\\s+");
            switch (tokens[0]) {
                case "v":
                    // Geometric vertex
                    Vector3f vec3f = new Vector3f(
                            Float.parseFloat(tokens[3]),
                            Float.parseFloat(tokens[1]),
                            Float.parseFloat(tokens[2]))
                            .mul(scale)
                            .add(offSet);
                    vertices.add(vec3f);
                    break;
                case "vn":
                    // Vertex normal
                    Vector3f vec3fNorm = new Vector3f(
                            Float.parseFloat(tokens[3]),
                            Float.parseFloat(tokens[1]),
                            Float.parseFloat(tokens[2]));
                    normals.add(vec3fNorm);
                    break;
                case "f":
                    faces.add(makeFace(tokens));
                    break;
                default:
                    // Ignore other lines
                    break;
            }
        }

        if (vertices.isEmpty() || faces.isEmpty()) {
            Logger.ERROR.print("Empty mesh loaded: " + path + " (this may result in errors)");
        }
    }

    private static List<String> openMesh(Path path) {
        try {
            // add extension to path
            return Files.readAllLines(path);
        } catch (IOException e) {
            Logger.ERROR.print("Could not read mesh '" + path.toAbsolutePath() + "'. Continuing game without model.");
            return new ArrayList<>();
        }
    }

    /**
     * for storage of vertex-indices face == plane
     */
    private static Mesh.Face makeFace(String... tokens) {
        int nOfTokens = tokens.length - 1;
        int[] vert = new int[nOfTokens];
        int[] norm = new int[nOfTokens];
        for (int i = 0; i < nOfTokens; i++) {
            Pair<Integer, Integer> c = (parseVertex(tokens[i + 1]));
            vert[i] = c.left;
            norm[i] = c.right;
        }

        return new Mesh.Face(vert, norm);
    }

    /**
     * parse and store the references of a single vertex
     */
    private static Pair<Integer, Integer> parseVertex(String line) {
        String[] lineTokens = line.split("/");
        int vertex = Integer.parseInt(lineTokens[0]) - 1;

        if (lineTokens.length > 2) {
            return new Pair<>(vertex, Integer.parseInt(lineTokens[2]) - 1);
        }
        return new Pair<>(vertex, -1);
    }
}
