package Shapes;

import nl.NG.Jetfightergame.Settings.ServerSettings;
import nl.NG.Jetfightergame.Tools.DataStructures.Pair;
import nl.NG.Jetfightergame.Tools.Directory;
import nl.NG.Jetfightergame.Tools.Logger;
import nl.NG.Jetfightergame.Tools.Resource;
import nl.NG.Jetfightergame.Tools.Vectors.DirVector;
import nl.NG.Jetfightergame.Tools.Vectors.PosVector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Geert van Ieperen created on 6-5-2018.
 */
public class ShapeParameters {
    public List<PosVector> vertices;
    public List<DirVector> normals;
    public List<Mesh.Face> faces;
    public final String name;

    /**
     * calls {@link #ShapeParameters(PosVector, float, Path, String)} on the file of the given path without offset and
     * scale of 1
     * @param fileName path to the .obj file. Extension should NOT be included
     */
    public ShapeParameters(String[] fileName) {
        this(PosVector.zeroVector(), 1f, Directory.meshes.getPath(fileName), fileName[fileName.length - 1]);
    }

    /**
     * @param offSet offset of the gravity middle in this mesh as {@code GM * -1}
     * @param scale  the scale standard applied to this object, to let it correspond to its contract
     * @param res    path from the directory main to the .obj file. Extension should NOT be included
     */
    public ShapeParameters(PosVector offSet, float scale, Resource res) {
        this(offSet, scale, res.getPathAs(".obj"), res.name());
    }

    /**
     * @param offSet offset of the gravity middle in this mesh as {@code GM * -1}
     * @param scale  the scale standard applied to this object, to let it correspond to its contract
     * @param path
     * @param name
     */
    public ShapeParameters(PosVector offSet, float scale, Path path, String name) {
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
                    PosVector vec3f = new PosVector();
                    new PosVector(
                            Float.parseFloat(tokens[3]),
                            Float.parseFloat(tokens[1]),
                            Float.parseFloat(tokens[2]))
                            .scale(scale, vec3f)
                            .add(offSet, vec3f);
                    vertices.add(vec3f);
                    break;
                case "vn":
                    // Vertex normal
                    DirVector vec3fNorm = new DirVector(
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
            if (ServerSettings.DEBUG) e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * for storage of vertex-indices face == plane
     */
    private static Mesh.Face makeFace(String... faces) {
        int nOfFaces = faces.length - 1;
        int[] vert = new int[nOfFaces];
        int[] norm = new int[nOfFaces];
        for (int i = 0; i < nOfFaces; i++) {
            Pair<Integer, Integer> c = (parseVertex(faces[i + 1]));
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
