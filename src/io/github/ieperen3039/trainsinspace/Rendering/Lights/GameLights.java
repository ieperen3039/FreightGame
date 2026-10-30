package io.github.ieperen3039.trainsinspace.Rendering.Lights;

import io.github.ieperen3039.trainsinspace.Core.GameAspect;
import io.github.ieperen3039.trainsinspace.DataStructures.Generic.Color4f;
import io.github.ieperen3039.trainsinspace.Rendering.MatrixStack.SGL;
import io.github.ieperen3039.trainsinspace.Tools.Directory;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 3-2-2019.
 */
public interface GameLights extends GameAspect {
    /**
     * adds a point-light to the game.
     * @param light the new light
     */
    void addPointLight(PointLight light);

    /**
     * initializes the lights on the scene.
     * @param gl the current gl object
     */
    void draw(SGL gl);

    /**
     * set the parameters of the one infinitely-far light source of the scene.
     * @param origin    a vector TO the light source
     * @param color     the color of the light source
     * @param intensity the light intensity
     */
    void addDirectionalLight(Vector3fc origin, Color4f color, float intensity);

    /**
     * update the lights after entity position updates
     */
    void update();

    void dumpShadowMap(Directory dir);

    Matrix4fc getLightMatrix();
}
