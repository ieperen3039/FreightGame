package NG.Shaders;

import nl.NG.Jetfightergame.Tools.Vectors.Color4f;
import org.joml.Vector3f;

import java.io.IOException;

import static nl.NG.Jetfightergame.Settings.ClientSettings.MAX_POINT_LIGHTS;

/**
 * @author Geert van Ieperen created on 2-12-2017.
 */
@SuppressWarnings("Duplicates")
public class PhongShader extends AbstractShader {

    public PhongShader() throws ShaderException, IOException {
        super("Phong/vertex.vert", geometryPath, "Phong/fragment.frag");

        // Create the Material uniform
        createUniform("material.ambient");
        createUniform("material.diffuse");
        createUniform("material.specular");
        createUniform("material.reflectance");

        // Create the lighting uniforms
        createUniform("specularPower");
        createUniform("ambientLight");
        createUniform("cameraPosition");
        createUniform("fogRange");
        createPointLightsUniform(MAX_POINT_LIGHTS);
    }

    public void setSpecular(float power) {
        setUniform("specularPower", power);
    }

    public void setAmbientLight(Color4f ambientLight) {
        setUniform("ambientLight", ambientLight.toVector3f());
    }

    /**
     * set the maximum vision radius to the specified range. Fog will become thicker and take the color of ambientlight
     * inversely to the range.
     */
    public void setFogRange(float range) {
        setUniform("fogRange", range);
    }

    public void setCameraPosition(Vector3f mPosition) {
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
        setUniform("material.ambient", diffuse);
        setUniform("material.diffuse", diffuse);
        setUniform("material.specular", specular);
        setUniform("material.reflectance", reflectance);
    }
}
