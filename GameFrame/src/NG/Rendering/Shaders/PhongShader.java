package NG.Rendering.Shaders;

import NG.DataStructures.Color4f;
import NG.Engine.Game;
import NG.Entities.Entity;
import NG.Tools.Directory;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Geert van Ieperen created on 2-12-2017.
 */
@SuppressWarnings("Duplicates")
public class PhongShader extends AbstractShader {
    private static final Path VERTEX_PATH = Directory.shaders.getPath("Phong", "phong.vert");
    private static final Path FRAGMENT_PATH = Directory.shaders.getPath("Phong", "phong.frag");

    public PhongShader(int maxPointLights) throws ShaderException, IOException {
        super(VERTEX_PATH, null, FRAGMENT_PATH);

        // Create the Material uniform
        createUniform("material.diffuse");
        createUniform("material.specular");
        createUniform("material.reflectance");

        // Create the lighting uniforms
        createUniform("ambientLight");
        createUniform("cameraPosition");

        createPointLightsUniform(maxPointLights);
    }

    @Override
    public void initialize(Game game) {
        Vector3fc eye = game.camera().getEye();
        setUniform("ambientLight", getAmbientLight().toVector3f());
        setUniform("cameraPosition", eye);
    }

    @Override
    public void setEntity(Entity entity) {

    }

    @Override
    public void unsetEntity() {

    }

    /**
     * TODO implement day-night cycle
     * @return light color
     */
    private Color4f getAmbientLight() {
        return new Color4f(1, 1, 1, 0.1f);
    }

    /**
     * Create an uniform for a pointslight array.
     * @param size The size of the array.
     * @throws ShaderException If an error occurs while fetching the memory location.
     */
    private void createPointLightsUniform(int size) throws ShaderException {
        for (int i = 0; i < size; i++) {
            try {
                createUniform(("lights" + "[" + i + "]") + ".color");
                createUniform(("lights" + "[" + i + "]") + ".mPosition");
                createUniform(("lights" + "[" + i + "]") + ".intensity");

            } catch (ShaderException ex) {
                if (i == 0) throw ex;
                else throw new IllegalArgumentException(
                        "Number of lights in shader is not equal to game value (" + (i - 1) + " instead of " + size + ")", ex);
            }
        }
    }

    public void setPointLight(int lightNumber, Vector4f mPosition, Color4f color) {
        setUniform(("lights[" + lightNumber + "]") + ".color", color.rawVector3f());
        setUniform(("lights[" + lightNumber + "]") + ".mPosition", mPosition);
        setUniform(("lights[" + lightNumber + "]") + ".intensity", color.alpha);
    }

    @Override
    public void setMaterial(Color4f diffuse, Color4f specular, float reflectance) {
        setUniform("material.diffuse", diffuse);
        setUniform("material.specular", specular);
        setUniform("material.reflectance", reflectance);
    }

    /**
     * Set the value of a certain Light shader uniform
     * @param uniformName The name of the uniform.
     * @param mPosition   position in modelSpace
     * @param color       the light color with its intensity as alpha value
     */
    protected void setPointLightUniform(String uniformName, Vector3f mPosition, Color4f color) {
        setUniform(uniformName + ".color", color.rawVector3f());
        setUniform(uniformName + ".mPosition", mPosition);
        setUniform(uniformName + ".intensity", color.alpha);
    }
}
