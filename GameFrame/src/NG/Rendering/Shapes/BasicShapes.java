package NG.Rendering.Shapes;

import NG.DataStructures.MatrixStack.Renderable;
import NG.DataStructures.MatrixStack.SGL;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

/**
 * @author Geert van Ieperen. Created on 14-9-2018.
 */
public enum BasicShapes implements Renderable {
    ARROW("arrow.obj"),
    ICOSAHEDRON("icosahedron.obj"),
    INV_CUBE("inverseCube.obj"),
    CUBE("cube.obj");

    private final Mesh shape;

    BasicShapes(String... path) {
        ShapeParameters pars = new ShapeParameters(path);
        shape = new Mesh(pars.vertices, pars.normals, pars.faces, GL_TRIANGLES);
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
