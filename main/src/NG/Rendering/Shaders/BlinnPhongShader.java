package NG.Rendering.Shaders;

import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.Rendering.Lights.DirectionalLight;
import NG.Rendering.Textures.GenericTextures;
import NG.Rendering.Textures.Texture;
import NG.AssetHandling.Resource;
import NG.Settings.Settings;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.io.IOException;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.*;

/**
 * A shader that uses a shadow-map and a Blinn-Phong model for lighting
 * @author Geert van Ieperen
 */
@SuppressWarnings("Duplicates")
public class BlinnPhongShader extends SceneShader implements TextureShader {
    private static final Resource.Path BLINN_PHONG_PATH = SHADER_DIRECTORY.resolve("BlinnPhong");
    private static final Resource.Path VERTEX_PATH = BLINN_PHONG_PATH.resolve("blinnphong.vert");
    private static final Resource.Path FRAGMENT_PATH = BLINN_PHONG_PATH.resolve("blinnphong.frag");
    private static final int MAX_POINT_LIGHTS = 10;
    private static final float SPECULAR_POWER = 1f;

    private int nextLightIndex = 0;

    /**
     * @throws ShaderException if a new shader could not be created by some opengl reason
     * @throws IOException     if the defined files could not be found (the file is searched for in the shader folder
     *                         itself, and should exclude any first slash)
     */
    public BlinnPhongShader() throws ShaderException, IOException {
        super(VERTEX_PATH, null, FRAGMENT_PATH);

        createUniform("material.diffuse");
        createUniform("material.specular");
        createUniform("material.reflectance");

        createUniform("directionalLight.color");
        createUniform("directionalLight.direction");
        createUniform("directionalLight.intensity");
        createUniform("directionalLight.lightSpaceMatrix");
        createUniform("directionalLight.doShadows");

        createPointLightsUniform("pointLights", MAX_POINT_LIGHTS);

        createUniform("texture_sampler");
        createUniform("dynamicShadowMap");

        createUniform("ambientLight");
        createUniform("specularPower");
        createUniform("cameraPosition");
        createUniform("drawHeightLines");

        createUniform("hasTexture");
    }

    @Override
    public void initialize(Game game) {
        // Base variables
        Vector3fc eye = game.camera().getEye();
        Settings settings = game.settings();

        setUniform("ambientLight", settings.AMBIENT_LIGHT.toVector3f());
        setUniform("cameraPosition", eye);
        setUniform("specularPower", SPECULAR_POWER);

        setUniform("hasTexture", false);
        setUniform("drawHeightLines", false);

        // Texture for the model
        setUniform("texture_sampler", 0);
        setUniform("dynamicShadowMap", 2);

        GenericTextures.CHECKER.bind(GL_TEXTURE0);
        GenericTextures.CHECKER.bind(GL_TEXTURE1);
        GenericTextures.CHECKER.bind(GL_TEXTURE2);

        nextLightIndex = 0;
        discardRemainingLights();
        nextLightIndex = 0;
    }

    @Override
    public void setPointLight(Vector3fc position, Color4f color, float intensity) {
        int lightNumber = nextLightIndex++;
        setUniform(("pointLights[" + lightNumber + "]") + ".color", color.rawVector3f());
        setUniform(("pointLights[" + lightNumber + "]") + ".mPosition", position);
        setUniform(("pointLights[" + lightNumber + "]") + ".intensity", color.alpha * intensity);
    }

    @Override
    public void setDirectionalLight(DirectionalLight light) {
        Color4f color = light.getColor();
        setUniform("directionalLight.color", color.rawVector3f());
        setUniform("directionalLight.direction", light.getDirectionToLight());
        setUniform("directionalLight.intensity", color.alpha * light.getIntensity());
        setUniform("directionalLight.lightSpaceMatrix", light.getLightSpaceMatrix());

        // Shadows
        boolean doShadows = light.doShadows();
        setUniform("directionalLight.doShadows", doShadows);

        if (doShadows) {
            ShadowMap dynamicShadowMap = light.getShadowMap();
            dynamicShadowMap.bind(GL_TEXTURE2);
        }

    }

    @Override
    public void setMaterial(Color4f diffuse, Color4f specular, float reflectance) {
        setUniform("material.diffuse", diffuse);
        setUniform("material.specular", specular);
        setUniform("material.reflectance", reflectance);
    }

    @Override
    public void setTexture(Texture tex) {
        if (tex != null) {
            setUniform("hasTexture", true);
            tex.bind(GL_TEXTURE0);

        } else {
            unsetTexture();
        }
    }

    @Override
    public void unsetTexture() {
        glBindTexture(GL_TEXTURE_2D, 0);
        setUniform("hasTexture", false);
    }

    @Override
    public void discardRemainingLights() {
        while (nextLightIndex < MAX_POINT_LIGHTS) {
            setPointLight(new Vector3f(), Color4f.INVISIBLE, 0);
        }
    }

    public void setHeightLines(boolean enable) {
        setUniform("drawHeightLines", enable);
    }
}
