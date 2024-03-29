package NG.Rendering.MeshLoading;

import NG.DataStructures.Generic.Color4f;
import NG.Tools.Logger;
import NG.Tools.Toolbox;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * @author Geert van Ieperen created on 6-5-2018.
 */
public final class FileLoaders {
    /**
     * @param scale the scaling applied to the loaded object
     * @param objInput a file-input stream of the object
     * @param name  debug name of the shape
     */
    public static MeshFile loadOBJ(Vector3fc scale, InputStream objInput, String name) throws IOException {
        List<Vector2fc> textureCoords;
        List<Vector3fc> vertices;
        List<Vector3fc> normals;
        List<Mesh.Face> faces;
        List<Color4f> colors;
        textureCoords = new ArrayList<>();
        vertices = new ArrayList<>();
        normals = new ArrayList<>();
        faces = new ArrayList<>();
        colors = new ArrayList<>();

        Scanner sc = new Scanner(objInput);
        while (sc.hasNext()) {
            String line = sc.nextLine();
            String[] tokens = Toolbox.WHITESPACE_PATTERN.split(line);
            switch (tokens[0]) {
                case "v" ->
                        // Geometric vertex
                        vertices.add(new Vector3f(
                                        Float.parseFloat(tokens[1]),
                                        Float.parseFloat(tokens[2]),
                                        Float.parseFloat(tokens[3])
                                )
                                        .mul(scale)
                        );
                case "vn" ->
                        // Vertex normal
                        normals.add(new Vector3f(
                                Float.parseFloat(tokens[1]),
                                Float.parseFloat(tokens[2]),
                                Float.parseFloat(tokens[3])
                        ));
                case "vt" -> textureCoords.add(new Vector2f(
                        Float.parseFloat(tokens[1]),
                        Float.parseFloat(tokens[2])
                ));
                case "f" -> faces.add(Mesh.Face.parseOBJ(tokens));
                default -> {
                }
                // Ignore other lines
            }
        }

        if (vertices.isEmpty() || faces.isEmpty()) {
            Logger.ERROR.print("Empty mesh loaded: " + name + " (this may result in errors)");
        }

        return new MeshFile(name, vertices, normals, faces, textureCoords, colors);
    }

    /**
     * Starts the parsing of a new .ply file. Opens the file as a list of strings where each String corresponds to a
     * line in the .ply file. Each line is then processed one by one and at the end the data is restructured to be
     * usable by the Mesh class.
     * @param objInput input stream of the .ply file to parse
     * @throws IOException if file not found
     * @throws IOException if file format not supported
     */
    public static MeshFile loadPLY(Vector3fc scale, InputStream objInput, String name) throws IOException {
        int nrOfVertices = -1;
        int nrOfFaces = -1;

        // Open the file as a list of strings
        Scanner sc = new Scanner(objInput);
        List<String> lines = new ArrayList<>();
        while (sc.hasNext()) {
           lines.add(sc.nextLine());
        }

        // Check if the file format is correct
        int endHeader = lines.indexOf("end_header");
        if (endHeader == -1) {
            throw new IOException("PLYLoader.loadMesh() failed: Unsupported file format. " +
                    "'end_header' keyword is missing");
        }

        List<String> header = new ArrayList<>(lines.subList(0, endHeader)); // exclude "end_header"
        List<String> body = new ArrayList<>(lines.subList(endHeader + 1, lines.size()));

        // Check the header and query the amount of vertices and faces
        int numberOfProperties = 0;

        for (String line : header) {
            String[] tokens = Toolbox.WHITESPACE_PATTERN.split(line);

            switch (tokens[0]) {
                case "ply", "comment" -> {}
                case "format" -> {
                    if (!tokens[1].equals("ascii")) {
                        throw new IOException("Not the ASCII format, but " + tokens[1]);
                    }
                }
                case "element" -> {
                    if (tokens[1].equals("vertex")) {
                        assert nrOfVertices == -1;
                        nrOfVertices = Integer.parseInt(tokens[2]);

                    } else if (tokens[1].equals("face")) {
                        assert nrOfFaces == -1;
                        nrOfFaces = Integer.parseInt(tokens[2]);

                    } else {
                        throw new IOException("Unsupported element " + tokens[1]);
                    }
                }
                case "property" -> numberOfProperties++;
                default -> throw new IOException("Unsupported keyword " + tokens[0]);
            }
        }

        List<Vector3fc> vertices = new ArrayList<>();
        List<Vector3fc> normals = new ArrayList<>();
        List<Mesh.Face> faces = new ArrayList<>();
        List<Color4f> colors = new ArrayList<>();

        // TODO allow variable properties
        if (numberOfProperties < 10) {
            throw new IOException("Wrong number of properties: " + numberOfProperties + " where 10 were expected (3 position, 3 normal, 4 color)");
        }

        // Parse all vertices
        for (int i = 0; i < nrOfVertices; i++) {
            String[] tokens = Toolbox.WHITESPACE_PATTERN.split(body.get(i));

            // Position vector data
            vertices.add(
                    new Vector3f(
                            Float.parseFloat(tokens[0]),
                            Float.parseFloat(tokens[1]),
                            Float.parseFloat(tokens[2])
                    )
                            .mul(scale)
            );

            // Normal vector data
            normals.add(new Vector3f(
                    Float.parseFloat(tokens[3]),
                    Float.parseFloat(tokens[4]),
                    Float.parseFloat(tokens[5])
            ));

            // Color data
            colors.add(Color4f.rgb(
                    Integer.parseInt(tokens[6]),
                    Integer.parseInt(tokens[7]),
                    Integer.parseInt(tokens[8])
            ));
        }

        // Parse all faces
        for (int i = nrOfVertices; i < nrOfVertices + nrOfFaces; i++) {
            String[] s = Toolbox.WHITESPACE_PATTERN.split(body.get(i));
            faces.add(Mesh.Face.parsePLY(s));
        }

        return new MeshFile(name, vertices, normals, faces, Collections.emptyList(), colors);
    }
}
