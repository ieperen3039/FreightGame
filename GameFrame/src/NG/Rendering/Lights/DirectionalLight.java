/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package NG.Rendering.Lights;


import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.Rendering.Shaders.ShaderException;
import NG.Rendering.Shaders.ShadowMap;
import NG.Resources.GeneratorResource;
import NG.Resources.Resource;
import NG.Settings.Settings;
import NG.Tools.Vectors;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A light source that is infinitely far away. Manages shadow mappings and light properties.
 * @author Dungeons-and-Drawings group
 * @author Geert van Ieperen
 */
public class DirectionalLight {
    private static final float LIGHT_Z_NEAR = 0.5f;
    public static final int LIGHT_Z_FAR_MULTIPLIER = 2;
    public static final int LIGHT_CUBE_SIZE_MULTIPLIER = 2;
    private Color4f color;
    private final Vector3f direction;
    private float intensity;

    private Resource<ShadowMap> shadowMap;

    private Matrix4f ortho = new Matrix4f();
    private Matrix4f lightSpaceMatrix = new Matrix4f();

    private float lightCubeSize;
    private final Vector3f lightCenter = new Vector3f();
    private boolean doShadow;

    public DirectionalLight(Color4f color, Vector3fc direction, float intensity) {
        this.color = color;
        this.direction = new Vector3f(direction);
        this.intensity = intensity;
    }

    /**
     * Performing this on the OpenGL context is faster.
     * @param game a reference to the game
     * @throws ShaderException when the shader can't be initialized correctly
     */
    public void init(Game game) throws ShaderException {
        Settings settings = game.settings();
        int dyRes = settings.SHADOW_RESOLUTION;

        doShadow = dyRes > 0;
        shadowMap = new GeneratorResource<>(() -> new ShadowMap(dyRes), ShadowMap::cleanup);
    }

    public Vector3fc getDirectionToLight() {
        return direction;
    }

    public void setDirection(Vector3fc direction) {
        this.direction.set(direction).normalize();

        lightSpaceMatrix = recalculateLightSpace();
    }

    private Matrix4f recalculateLightSpace() {
        if (!doShadow) {
            return lightSpaceMatrix;
        }

        Vector3f lightPos = new Vector3f(direction)
                .mul(lightCubeSize + LIGHT_Z_NEAR)
                .add(lightCenter);

        return new Matrix4f(ortho)
                .lookAt(lightPos, lightCenter, Vectors.Z);
    }

    public float getIntensity() {
        return intensity;
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    public Matrix4fc getLightSpaceMatrix() {
        return lightSpaceMatrix;
    }

    public ShadowMap getShadowMap() {
        return shadowMap.get();
    }

    public Color4f getColor() {
        return color;
    }

    public void setColor(Color4f color) {
        this.color = color;
    }

    public void setLightSize(float lightSize) {
        lightCubeSize = lightSize * LIGHT_CUBE_SIZE_MULTIPLIER;

        float zFar = lightCubeSize * LIGHT_Z_FAR_MULTIPLIER + LIGHT_Z_NEAR;
        ortho.setOrthoSymmetric(lightCubeSize, lightCubeSize, LIGHT_Z_NEAR, zFar);

        lightSpaceMatrix = recalculateLightSpace();
    }

    public Vector3fc getLightCenter() {
        return lightCenter;
    }

    public void setLightCenter(Vector3fc newFocus) {
        lightCenter.set(newFocus);
        lightSpaceMatrix = recalculateLightSpace();
    }

    public boolean doShadows() {
        return doShadow;
    }

    /**
     * Cleanup memory
     */
    public void cleanup() {
        shadowMap.drop();
    }
}
