package NG.Rendering.Shaders;

import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.Rendering.Lights.DirectionalLight;
import NG.AssetHandling.Resource;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

import java.io.IOException;

/**
 * @author Geert van Ieperen created on 2-12-2017.
 */
@SuppressWarnings("Duplicates")
public class PhongShader extends SceneShader {
    private static final Resource.Path PHONG_PATH = SHADER_DIRECTORY.resolve("Phong");
    private static final Resource.Path VERTEX_PATH = PHONG_PATH.resolve("phong.vert");
    private static final Resource.Path FRAGMENT_PATH = PHONG_PATH.resolve("phong.frag");
    private static final int MAX_POINT_LIGHTS = 16;
    private int nextLightIndex = 0;

    public PhongShader() throws ShaderException, IOException {
        super(VERTEX_PATH, null, FRAGMENT_PATH);

        // Create the Material uniform
        createUniform("material.diffuse");
        createUniform("material.specular");
        createUniform("material.reflectance");

        // Create the lighting uniforms
        createUniform("ambientLight");
        createUniform("cameraPosition");

        createPointLightsUniform("lights", MAX_POINT_LIGHTS);
    }

    @Override
    public void initialize(Game game) {
        Vector3fc eye = game.camera().getEye();

        setUniform("ambientLight", game.settings().AMBIENT_LIGHT.toVector3f());
        setUniform("cameraPosition", eye);

        prepLights();
    }

    public void prepLights() { // TODO inline
        nextLightIndex = 0;
        discardRemainingLights();
        nextLightIndex = 0;
    }

    @Override
    @SuppressWarnings("Duplicates")
    public void setPointLight(Vector3fc position, Color4f color, float intensity) {
        Vector4fc mPosition = new Vector4f(position, 1.0f);
        setLight(color, mPosition, color.alpha * intensity);
    }

    @Override
    public void setDirectionalLight(DirectionalLight light) {
        Vector4fc mPosition = new Vector4f(light.getDirectionToLight(), 0.0f);
        Color4f color = light.getColor();
        setLight(color, mPosition, color.alpha * light.getIntensity());
    }

    private void setLight(Color4f color, Vector4fc mPosition, float intensity) {
        int lightNumber = nextLightIndex++;
        assert lightNumber < MAX_POINT_LIGHTS;
        setUniform(("lights[" + lightNumber + "]") + ".color", color.rawVector3f());
        setUniform(("lights[" + lightNumber + "]") + ".mPosition", mPosition);
        setUniform(("lights[" + lightNumber + "]") + ".intensity", intensity);
    }

    @Override
    public void setMaterial(Color4f diffuse, Color4f specular, float reflectance) {
        setUniform("material.diffuse", diffuse);
        setUniform("material.specular", specular);
        setUniform("material.reflectance", reflectance);
    }

    /**
     * sets possible unused point-light slots to 'off'. No more point lights can be added after a call to this method.
     */
    @Override
    public void discardRemainingLights() {
        while (nextLightIndex < MAX_POINT_LIGHTS) {
            setPointLight(new Vector3f(), Color4f.INVISIBLE, 0);
        }
    }
}
