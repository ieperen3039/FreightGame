package NG.ActionHandling;

import NG.DataStructures.Color4f;
import NG.DataStructures.Material;
import NG.Engine.Game;
import NG.Entities.Entity;
import NG.Rendering.GLFWWindow;
import NG.Rendering.MatrixStack.ShaderUniformGL;
import NG.Rendering.Shaders.ShaderException;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Settings.Settings;
import NG.Tools.Directory;
import NG.Tools.Logger;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static NG.Rendering.Shaders.AbstractShader.createShader;
import static NG.Rendering.Shaders.AbstractShader.loadText;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

/**
 * @author Geert van Ieperen created on 7-1-2019.
 */
@SuppressWarnings("Duplicates")
public class ClickShader implements ShaderProgram {

    private static final Path VERTEX_PATH = Directory.shaders.getPath("Click", "click.vert");
    private static final Path FRAGMENT_PATH = Directory.shaders.getPath("Click", "click.frag");
    private static final int COLOR_SHIFT = 2;
    private static final int COLOR_MASK = 1 << COLOR_SHIFT;
    private static final Color4f DEFAULT_COLOR = Color4f.BLACK;
    private final Map<String, Integer> uniforms;

    private ArrayList<Entity> mapping;
    private int programId;
    private int vertexShaderID;
    private int geometryShaderID;
    private int fragmentShaderID;

    private ClickShader() throws ShaderException, IOException {
        this.uniforms = new HashMap<>();

        this.programId = glCreateProgram();
        if (this.programId == 0) {
            throw new ShaderException("OpenGL error: Could not create Shader");
        }

        if (VERTEX_PATH != null) {
            final String shaderCode = loadText(VERTEX_PATH);
            vertexShaderID = createShader(ClickShader.this.programId, GL_VERTEX_SHADER, shaderCode);
        }

        if (FRAGMENT_PATH != null) {
            final String shaderCode = loadText(FRAGMENT_PATH);
            fragmentShaderID = createShader(ClickShader.this.programId, GL_FRAGMENT_SHADER, shaderCode);
        }

        link();

        // Create uniforms for world and projection matrices
        createUniform("viewProjectionMatrix");
        createUniform("modelMatrix");
        createUniform("color");
        mapping = new ArrayList<>();
    }

    @Override
    public void initialize(Game game) {
        mapping.clear();
    }

    @Override
    public void setPointLight(int lightNumber, Vector4f mPosition, Color4f color) {
        // ignore
    }

    @Override
    public void setMaterial(Color4f diffuse, Color4f specular, float reflectance) {
        // ignore
    }

    @Override
    public void setEntity(Entity entity) {
        mapping.add(entity);
        int i = mapping.size();
        Vector3i color = numberToColor(i);
        setColor(color);
    }

    @Override
    public void unsetEntity() {
        setColor(new Vector3i());
    }

    @Override
    public void bind() {
        glUseProgram(programId);
    }

    @Override
    public void unbind() {
        glUseProgram(0);
    }

    @Override
    public void cleanup() {
        unbind();
        if (programId != 0) {
            glDeleteProgram(programId);
        }
    }

    public void link() throws ShaderException {
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new ShaderException("Error linking Shader code: " + glGetProgramInfoLog(programId, 1024));
        }

        if (vertexShaderID != 0) {
            glDetachShader(programId, vertexShaderID);
        }
        if (geometryShaderID != 0) {
            glDetachShader(programId, geometryShaderID);
        }
        if (fragmentShaderID != 0) {
            glDetachShader(programId, fragmentShaderID);
        }

        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            System.err.println("Warning validating Shader code: " + glGetProgramInfoLog(programId, 1024));
        }
    }

    /**
     * Create a new uniform and get its memory location.
     * @param uniformName The name of the uniform.
     * @throws ShaderException If an error occurs while fetching the memory location.
     */
    private void createUniform(String uniformName) throws ShaderException {
        int uniformLocation = glGetUniformLocation(programId, uniformName);
        if (uniformLocation < 0) {
            throw new ShaderException("Could not find uniform:" + uniformName);
        }
        uniforms.put(uniformName, uniformLocation);
    }

    /**
     * Set the value of a 4x4 matrix shader uniform.
     * @param uniformName The name of the uniform.
     * @param value       The new value of the uniform.
     */
    private void setUniform(String uniformName, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Dump the matrix into a float buffer
            FloatBuffer fb = stack.mallocFloat(16);
            value.get(fb);
            glUniformMatrix4fv(uniforms.get(uniformName), false, fb);
        }
    }

    @Override
    public void setMaterial(Material material, Color4f color) {
    }

    @Override
    public boolean accepts(Entity entity) {
        return true;
    }

    @Override
    public void setProjectionMatrix(Matrix4f viewProjectionMatrix) {
        setUniform("viewProjectionMatrix", viewProjectionMatrix);
    }

    @Override
    public void setModelMatrix(Matrix4f modelMatrix) {
        setUniform("modelMatrix", modelMatrix);
    }

    @Override
    public void setNormalMatrix(Matrix3f normalMatrix) {
    }

    protected void setColor(Vector3i color) {
        Vector3f toFloats = new Vector3f(color).div(255);
        glUniform4f(uniforms.get("color"), toFloats.x, toFloats.y, toFloats.z, 1);
    }

    private static Vector3i numberToColor(int i) {
        assert i < 65535;
        byte r = (byte) (i % 256);
        byte g = (byte) ((i >> 8) % 256);
        byte b = (byte) ((i >> 16) % 256);
        return new Vector3i(r, g, b);
    }

    private static int colorToNumber(Vector3i value) {
        int i = 0;
        i += value.x;
        i += (value.y << 8);
        i += (value.z << 16);
        return i;
    }

    /**
     * @param xPos x screen coordinate of the pixel
     * @param yPos y screen coordinate of the pixel
     * @return the color of a given pixel in (r, g, b) value
     */
    private static Vector3i getPixelValue(int xPos, int yPos) {
        glReadBuffer(GL11.GL_BACK);
        int bpp = 4; // Assuming a 32-bit display with a byte each for red, green, blue, and alpha.
        ByteBuffer buffer = BufferUtils.createByteBuffer(bpp);
        glReadPixels(xPos, yPos, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        int r = buffer.get(0);
        int g = buffer.get(1);
        int b = buffer.get(2);
        assert !(r < 0 || g < 0 || b < 0) : String.format("got (%d, %d, %d)", r, g, b);
        buffer.clear();

        return new Vector3i(r, g, b);
    }

    /**
     * may only be called on the current OpenGL context
     * @param game the current game
     * @param xPos x screen coordinate
     * @param yPos y screen coordinate
     * @return the entity that is visible on the given pixel coordinate.
     */
    public static Entity getEntity(Game game, int xPos, int yPos) {
        try {
            ClickShader shader = new ClickShader();
            shader.initialize(game);
            shader.bind();

            GLFWWindow window = game.window();
            ShaderUniformGL flatColorRender =
                    new ShaderUniformGL(shader, window.getWidth(), window.getHeight(), game.camera(), Settings.ISOMETRIC_VIEW);

            game.state().draw(flatColorRender);
            shader.unbind();

            window.printScreen(Directory.screenshots, "click", GL11.GL_BACK);
            Vector3i value = ClickShader.getPixelValue(xPos, yPos);
            glClear(GL_COLOR_BUFFER_BIT);

            int i = colorToNumber(value);

            if (i == 0) return null;
            return shader.mapping.get(i - 1);

        } catch (IOException e) {
            Logger.ERROR.print("Failed to get entity", e);
            return null;
        }

    }
}
