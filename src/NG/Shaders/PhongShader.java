package NG.Shaders;

import NG.DataStructures.Color4f;
import NG.Settings.Settings;
import NG.Tools.Directory;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Geert van Ieperen created on 2-12-2017.
 */
@SuppressWarnings("Duplicates")
public class PhongShader extends AbstractShader {
    private static final Path VERTEX_PATH = Directory.shaders.getPath("Phong", "vertex.vert");
    private static final Path FRAGMENT_PATH = Directory.shaders.getPath("Phong", "fragment.frag");

    public PhongShader(Settings s) throws ShaderException, IOException {
        super(VERTEX_PATH, null, FRAGMENT_PATH);

        // Create the Material uniform
        createUniform("material.diffuse");
        createUniform("material.specular");
        createUniform("material.reflectance");

        // Create the lighting uniforms
        createUniform("specularPower");
        createUniform("ambientLight");
        createUniform("cameraPosition");

        createPointLightsUniform(s.MAX_POINT_LIGHTS);
    }

    public void setSpecular(float power) {
        setUniform("specularPower", power);
    }

    public void setAmbientLight(Color4f ambientLight) {
        setUniform("ambientLight", ambientLight.toVector3f());
    }

    public void setCameraPosition(Vector3fc mPosition) {
        setUniform("cameraPosition", mPosition);
    }

    /**
     * Create an uniform for a pointslight array.
     * @param size The size of the array.
     * @throws ShaderException If an error occurs while fetching the memory location.
     */
    private void createPointLightsUniform(int size) throws ShaderException {
        for (int i = 0; i < size; i++) {
            createUniform(("pointLights" + "[" + i + "]") + ".color");
            createUniform(("pointLights" + "[" + i + "]") + ".mPosition");
            createUniform(("pointLights" + "[" + i + "]") + ".intensity");
        }
    }

    @Override
    public void setPointLight(int lightNumber, Vector3f mPosition, Color4f color) {
        setPointLightUniform("pointLights[" + lightNumber + "]", mPosition, color);
    }

    @Override
    public void setMaterial(Color4f diffuse, Color4f specular, float reflectance) {
        setUniform("material.diffuse", diffuse);
        setUniform("material.specular", specular);
        setUniform("material.reflectance", reflectance);
    }
}
