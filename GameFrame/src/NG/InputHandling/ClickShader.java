package NG.InputHandling;

import NG.Camera.Camera;
import NG.Core.Game;
import NG.Entities.Entity;
import NG.Rendering.GLFWWindow;
import NG.Rendering.MatrixStack.AbstractSGL;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.ShaderException;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Tools.Directory;
import NG.Tools.Logger;
import NG.Tools.Toolbox;
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

import static NG.Rendering.Shaders.ShaderProgram.createShader;
import static NG.Rendering.Shaders.ShaderProgram.loadText;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * @author Geert van Ieperen created on 7-1-2019.
 */
@SuppressWarnings("Duplicates")
public class ClickShader implements ShaderProgram {
    private static final Path VERTEX_PATH = Directory.shaders.getPath("Click", "click.vert");
    private static final Path FRAGMENT_PATH = Directory.shaders.getPath("Click", "click.frag");
    private final Map<String, Integer> uniforms;

    private final int frameBuffer;
    private final int colorBuffer;
    private final int depthBuffer;

    private int programId;
    private int vertexShaderID;
    private int fragmentShaderID;

    private ArrayList<Entity> mapping;
    private Entity lastEntity;
    private int windowWidth = 0;
    private int windowHeight = 0;

    private Entity cachedEntity = null;
    private Vector2i mousePosition;

    public ClickShader() {
        this.uniforms = new HashMap<>();

        this.programId = glCreateProgram();
        if (this.programId == 0) {
            throw new ShaderException("OpenGL error: Could not create click-shader");
        }

        try {
            if (VERTEX_PATH != null) {
                final String shaderCode = loadText(VERTEX_PATH);
                vertexShaderID = createShader(programId, GL_VERTEX_SHADER, shaderCode);
            }

            if (FRAGMENT_PATH != null) {
                final String shaderCode = loadText(FRAGMENT_PATH);
                fragmentShaderID = createShader(programId, GL_FRAGMENT_SHADER, shaderCode);
            }
        } catch (IOException e) {
            Logger.ERROR.print(e);
            throw new ShaderException("IO error: Could not create click-shader");
        }

        link();

        // Create uniforms for world and projection matrices
        createUniform("viewProjectionMatrix");
        createUniform("modelMatrix");
        createUniform("color");
        mapping = new ArrayList<>();

        // we generate one-pixel buffers, as we only render one pixel to check
        frameBuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);

        // color buffer to write to
        colorBuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, colorBuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_RGB8, 0, 0);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, colorBuffer);

        // depth buffer to use for depth testing
        depthBuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, 0, 0);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthBuffer);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);

        Toolbox.checkGLError(this.toString());

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    public void initialize(Game game) {
        mapping.clear();

        if (mousePosition == null) {
            Logger.printOnline(() -> String.format("(%4d, %4d) : %s", mousePosition.x, mousePosition.y, cachedEntity));
        }

        GLFWWindow window = game.window();
        mousePosition = window.getMousePosition();

        // if the screen size changed, resize buffers to match the new dimensions
        int newWidth = window.getWidth();
        int newHeight = window.getHeight();
        if (newWidth != windowWidth || newHeight != windowHeight) {
            windowWidth = newWidth;
            windowHeight = newHeight;
            glBindRenderbuffer(GL_RENDERBUFFER, colorBuffer);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_RGB8, newWidth, newHeight);
            glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, newWidth, newHeight);
            glBindRenderbuffer(GL_RENDERBUFFER, 0);
        }

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new ShaderException("ClickShader could not init FrameBuffer : error " + Toolbox.asHex(status));
        }

        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        // only render where the mouse is
//        glViewport(mousePosition.x, mousePosition.y, 1, 1);
        Toolbox.checkGLError(this.toString());
    }

    @Override
    public void bind() {
        glUseProgram(programId);
    }

    @Override
    public void unbind() {
        Toolbox.checkGLError(this.toString());

        // first get our result
        this.cachedEntity = getEntity();

        // then reset
        glUseProgram(0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
//        glViewport(0, 0, windowWidth, windowHeight);
    }

    private Entity getEntity() {
        ByteBuffer buffer = BufferUtils.createByteBuffer(3); // one for each color
        glReadPixels(mousePosition.x, windowHeight - mousePosition.y, 1, 1, GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);

        int r = Byte.toUnsignedInt(buffer.get(0));
        int g = Byte.toUnsignedInt(buffer.get(1));
        int b = Byte.toUnsignedInt(buffer.get(2));
        assert !(r < 0 || g < 0 || b < 0) : String.format("got (%d, %d, %d)", r, g, b);
        buffer.clear();

        // convert to local
        int entityIndex = colorToNumber(new Vector3i(r, g, b));

        if (entityIndex == 0) return null;
        return mapping.get(entityIndex - 1);
    }

    @Override
    public SGL getGL(Game game) {
        GLFWWindow window = game.window();
        Camera camera = game.camera();

        return new ClickShaderGL(camera, window);
    }

    @Override
    public void cleanup() {
        if (programId != 0) {
            glDeleteProgram(programId);
        }

        glDeleteFramebuffers(frameBuffer);
        glDeleteRenderbuffers(colorBuffer);
        glDeleteRenderbuffers(depthBuffer);
    }

    private void setEntity(Entity entity) {
        if (entity == null) return;
        if (!entity.equals(lastEntity)) {
            mapping.add(entity);
        }

        int i = mapping.size();
        Vector3i color = numberToColor(i);
        setColor(color);

        lastEntity = entity;
    }

    private void unsetEntity() {
        setColor(new Vector3i());
    }

    private void link() throws ShaderException {
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new ShaderException("Error linking Shader code: " + glGetProgramInfoLog(programId, 1024));
        }

        if (vertexShaderID != 0) {
            glDetachShader(programId, vertexShaderID);
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

    private void setProjectionMatrix(Matrix4f viewProjectionMatrix) {
        setUniform("viewProjectionMatrix", viewProjectionMatrix);
    }

    private void setModelMatrix(Matrix4f modelMatrix) {
        setUniform("modelMatrix", modelMatrix);
    }

    protected void setColor(Vector3i color) {
        Vector3f toFloats = new Vector3f(color).div(255);
        glUniform4f(uniforms.get("color"), toFloats.x, toFloats.y, toFloats.z, 1);
    }

    private static Vector3i numberToColor(int i) {
        assert i < (1 << 18);
        final int bitSize = (1 << 6);
        int r = (i % bitSize) << 2;
        int g = (((i >> 6) % bitSize) << 2);
        int b = (((i >> 12) % bitSize) << 2);

        return new Vector3i(r, g, b);
    }

    private static int colorToNumber(Vector3i value) {
        int i = 0;
        i += nearest(value.x) >> 2;
        i += nearest(value.y) << 4;
        i += nearest(value.z) << 10;

//        Logger.DEBUG.printf("%s -> %d", Vectors.toString(value), i);
        return i;
    }

    /**
     * if the number is not divisible by 4, move the number up or down such that it is
     * @param i a number
     * @return the closest value divisible by 4
     */
    private static int nearest(int i) {
        int mod = i % 4;
        if (mod == 1) {
            i -= 1;
        } else if (mod == 3) {
            i += 1;
        } else if (mod == 2) {
            i -= 2;
        }
        return i;
    }

    /**
     * automatically subroutines to the openGL context
     * @param game the current game
     * @param xPos x screen coordinate
     * @param yPos y screen coordinate
     * @return the entity that is visible on the given pixel coordinate.
     */
    public Entity getEntity(Game game, int xPos, int yPos) {
        return cachedEntity;
    }

    /**
     * @author Geert van Ieperen created on 30-1-2019.
     */
    public class ClickShaderGL extends AbstractSGL {
        private final Matrix4f viewProjectionMatrix;

        ClickShaderGL(Camera viewpoint, GLFWWindow window) {
            viewProjectionMatrix = viewpoint.getViewProjection(window);
        }

        @Override
        public void render(Mesh object, Entity sourceEntity) {
            setEntity(sourceEntity);
            setProjectionMatrix(viewProjectionMatrix);
            setModelMatrix(getModelMatrix());
            object.render(LOCK);
            unsetEntity();
        }

        public ShaderProgram getShader() {
            return ClickShader.this;
        }

        @Override
        public Matrix4fc getViewProjectionMatrix() {
            return viewProjectionMatrix;
        }
    }
}
