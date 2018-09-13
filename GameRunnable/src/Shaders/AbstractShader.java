package Shaders;

import DataStructures.Color4f;
import Tools.Directory;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static org.lwjgl.opengl.GL20.*;

/**
 * @author Yoeri Poels
 * @author Geert van Ieperen
 */
public abstract class AbstractShader implements ShaderProgram {

    private final Map<String, Integer> uniforms;

    private int programId;
    private int vertexShaderId;
    private int fragmentShaderId;

    /**
     * create a shader and manages the interaction of its uniforms
     * @param vertexPath   the vertex shader file in the shader directory
     * @param fragmentPath the fragment shader file in the shader directory
     * @throws ShaderException if a new shader could not be created by some opengl reason
     * @throws IOException     if the defined files could not be found (the file is searched for in the shader folder
     *                         itself, and should exclude any first slash)
     */
    public AbstractShader(String vertexPath, String fragmentPath) throws ShaderException, IOException {
        uniforms = new HashMap<>();

        programId = glCreateProgram();
        if (programId == 0) {
            throw new ShaderException("OpenGL error: Could not create Shader");
        }

        if (vertexPath != null) {
            final String shaderCode = loadText(Directory.shaders.getPath(vertexPath));
            vertexShaderId = createShader(programId, GL_VERTEX_SHADER, shaderCode);
        }

        if (fragmentPath != null) {
            final String shaderCode = loadText(Directory.shaders.getPath(fragmentPath));
            fragmentShaderId = createShader(programId, GL_FRAGMENT_SHADER, shaderCode);
        }

        link();

        // Create uniforms for world and projection matrices
        createUniform("viewProjectionMatrix");
        createUniform("modelMatrix");
        createUniform("normalMatrix");
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

    @Override
    public void link() throws ShaderException {
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new ShaderException("Error linking Shader code: " + glGetProgramInfoLog(programId, 1024));
        }

        if (vertexShaderId != 0) {
            glDetachShader(programId, vertexShaderId);
        }
        if (fragmentShaderId != 0) {
            glDetachShader(programId, fragmentShaderId);
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
    protected void createUniform(String uniformName) throws ShaderException {
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
    protected void setUniform(String uniformName, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Dump the matrix into a float buffer
            FloatBuffer fb = stack.mallocFloat(16);
            value.get(fb);
            glUniformMatrix4fv(uniforms.get(uniformName), false, fb);
        }
    }

    /**
     * Set the value of a 3x3 matrix shader uniform.
     * @param uniformName The name of the uniform.
     * @param value       The new value of the uniform.
     */
    protected void setUniform(String uniformName, Matrix3f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Dump the matrix into a float buffer
            FloatBuffer fb = stack.mallocFloat(9);
            value.get(fb);
            glUniformMatrix3fv(uniforms.get(uniformName), false, fb);
        }
    }

    /**
     * Set the value of a certain integer shader uniform
     * @param uniformName The name of the uniform.
     * @param value       The new value of the uniform.
     */
    protected void setUniform(String uniformName, int value) {
        glUniform1i(uniforms.get(uniformName), value);
    }

    /**
     * Set the value of a certain float shader uniform
     * @param uniformName The name of the uniform.
     * @param value       The new value of the uniform.
     */
    protected void setUniform(String uniformName, float value) {
        glUniform1f(uniforms.get(uniformName), value);
    }

    /**
     * Set the value of a certain 3D Vector shader uniform
     * @param uniformName The name of the uniform.
     * @param value       The new value of the uniform.
     */
    protected void setUniform(String uniformName, Vector3f value) {
        glUniform3f(uniforms.get(uniformName), value.x, value.y, value.z);
    }

    protected void setUniform(String uniformName, float[] value) {
        glUniform4f(uniforms.get(uniformName), value[0], value[1], value[2], value[3]);
    }

    /**
     * Set the value of a certain 4D Vector shader uniform
     * @param uniformName The name of the uniform.
     * @param value       The new value of the uniform.
     */
    protected void setUniform(String uniformName, Vector4f value) {
        glUniform4f(uniforms.get(uniformName), value.x, value.y, value.z, value.w);
    }

    /**
     * Set the value of a certain PointLight shader uniform
     * @param uniformName The name of the uniform.
     * @param mPosition   position in modelSpace
     * @param color       the light color with its intensity as alpha value
     */
    protected void setPointLightUniform(String uniformName, Vector3f mPosition, Color4f color) {
        setUniform(uniformName + ".color", color.rawVector3f());
        setUniform(uniformName + ".mPosition", mPosition);
        setUniform(uniformName + ".intensity", color.alpha);
    }

    /**
     * Create a new shader and return the id of the newly created shader.
     * @param programId
     * @param shaderType The type of shader, e.g. GL_VERTEX_SHADER.
     * @param shaderCode The shaderCode as a String.
     * @return The id of the newly created shader.
     * @throws ShaderException If an error occurs during the creation of a shader.
     */
    public static int createShader(int programId, int shaderType, String shaderCode) throws ShaderException {
        int shaderId = glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new ShaderException("Error creating shader. Type: " + shaderType);
        }

        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            throw new ShaderException("Error compiling Shader code:\n" + glGetShaderInfoLog(shaderId, 1024));
        }

        glAttachShader(programId, shaderId);

        return shaderId;
    }

    protected void setUniform(String uniformName, boolean value) {
        setUniform(uniformName, value ? 1 : 0);
    }

    protected void setUniform(String uniformName, Color4f color) {
        glUniform4f(uniforms.get(uniformName), color.red, color.green, color.blue, color.alpha);
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
        setUniform("normalMatrix", normalMatrix);
    }


    public static String loadText(Path path) throws IOException {
        String result;
        try (
                InputStream in = new FileInputStream(path.toFile());
                Scanner scanner = new Scanner(in, "UTF-8")
        ) {
            result = scanner.useDelimiter("\\A").next();
        } catch (FileNotFoundException e) {
            throw new IOException("Resource not found: " + path);
        }
        return result;
    }
}
