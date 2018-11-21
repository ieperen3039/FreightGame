package NG.Rendering.Shapes;

import NG.DataStructures.MatrixStack.Mesh;
import NG.DataStructures.MatrixStack.SGL;

/**
 * @author Geert van Ieperen. Created on 14-9-2018.
 */
public enum BasicShapes implements Mesh {
    ARROW("arrow.obj"),
    ICOSAHEDRON("icosahedron.obj"),
    INV_CUBE("inverseCube.obj"),
    CUBE("cube.obj");

    private final FlatMesh shape;

    BasicShapes(String... path) {
        ShapeParameters pars = new ShapeParameters(path);
        shape = new FlatMesh(pars.vertices, pars.normals, pars.faces);
    }

    @Override
    public void render(SGL.Painter lock) {
        shape.render(lock);
    }

    @Override
    public void dispose() {
        shape.dispose();
    }
}
