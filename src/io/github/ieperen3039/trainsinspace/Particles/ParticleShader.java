package io.github.ieperen3039.trainsinspace.Particles;

import io.github.ieperen3039.ngn.AssetHandling.Resource;
import io.github.ieperen3039.ngn.Camera.Camera;
import io.github.ieperen3039.ngn.Core.Main;
import io.github.ieperen3039.ngn.Rendering.MatrixStack.AbstractSGL;
import io.github.ieperen3039.ngn.Rendering.MeshLoading.Mesh;
import io.github.ieperen3039.ngn.Rendering.Shaders.ShaderException;
import io.github.ieperen3039.ngn.Rendering.Shaders.ShaderProgram;
import io.github.ieperen3039.ngn.Tools.Logger;
import io.github.ieperen3039.ngn.Tools.TickTime;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;

/**
 * @author Geert van Ieperen created on 17-5-2018.
 */
public class ParticleShader implements ShaderProgram {
    private static final Resource.Path PARTICLE_SHADER_PATH = SHADER_DIRECTORY.resolve("Particle");
    private static final Resource.Path VERTEX_PATH = PARTICLE_SHADER_PATH.resolve("vertex.vert");
    private static final Resource.Path FRAGMENT_PATH = PARTICLE_SHADER_PATH.resolve("fragment.frag");
    private static final Resource.Path GEOMETRY_PATH = PARTICLE_SHADER_PATH.resolve("geometry.glsl");

    private final int programId;
    private final int vertexShaderId;
    private final int fragmentShaderId;

    private final int timeUniform;
    private final int projectionUniform;
    private final int geometryShaderId;
    private final int sizeUniform;
    private float particleSize;

    public ParticleShader(float particleSize) throws IOException {
        this.particleSize = particleSize;

        programId = glCreateProgram();

        final String vertexCode = VERTEX_PATH.asText();
        vertexShaderId = ShaderProgram.createShader(programId, GL_VERTEX_SHADER, vertexCode);

        final String fragmentCode = FRAGMENT_PATH.asText();
        fragmentShaderId = ShaderProgram.createShader(programId, GL_FRAGMENT_SHADER, fragmentCode);

        final String geometryCode = GEOMETRY_PATH.asText();
        geometryShaderId = ShaderProgram.createShader(programId, GL_GEOMETRY_SHADER, geometryCode);

        link();

        timeUniform = glGetUniformLocation(programId, "currentTime");
        projectionUniform = glGetUniformLocation(programId, "viewProjectionMatrix");
        sizeUniform = glGetUniformLocation(programId, "particleSize");
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void cleanup() {
        unbind();
        if (programId != 0) {
            glDeleteProgram(programId);
        }
    }

    private void link() throws ShaderException {
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new ShaderException("Error linking Shader code: " + glGetProgramInfoLog(programId, 1024));
        }

        glDetachShader(programId, vertexShaderId);
        glDetachShader(programId, geometryShaderId);
        glDetachShader(programId, fragmentShaderId);

        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            Logger.ERROR.print("Warning validating Shader code: " + glGetProgramInfoLog(programId, 1024));
        }
    }

    /**
     * sets the projection matrix uniform
     */
    public void setProjection(Matrix4fc matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Dump the matrix into a float buffer
            FloatBuffer fb = stack.mallocFloat(16);
            matrix.get(fb);
            glUniformMatrix4fv(projectionUniform, false, fb);
        }
    }

    public void setParticleSize(float particleSize) {
        this.particleSize = particleSize;
    }

    @Override
    public ParticleGL getGL(Main game) {
        Camera camera = game.camera();
        TickTime timer = game.timer();
        glUniform1f(timeUniform, (float) timer.getRenderTime());
        glUniform1f(sizeUniform, particleSize);

        return new ParticleGL(camera, game.getViewPort());
    }

    public class ParticleGL extends AbstractSGL {
        private final Matrix4f viewProjectionMatrix;

        public ParticleGL(Camera camera, Main.ViewPort viewPort) {
            viewProjectionMatrix = camera.getViewProjection(viewPort);
        }

        @Override
        public void render(Mesh object, int sourceEntity) {
            setProjection(viewProjectionMatrix);
            object.render(LOCK);
        }

        @Override
        public ShaderProgram getShader() {
            return ParticleShader.this;
        }

        @Override
        public Matrix4fc getViewProjectionMatrix() {
            return viewProjectionMatrix;
        }
    }
}
