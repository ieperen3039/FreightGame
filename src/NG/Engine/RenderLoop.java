package NG.Engine;

import NG.DataStructures.Color4f;
import NG.DataStructures.MatrixStack.SGL;
import NG.DataStructures.MatrixStack.ShaderUniformGL;
import NG.Settings.Settings;
import NG.Shaders.PhongShader;
import org.joml.Vector3fc;

import java.io.IOException;

/**
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class RenderLoop extends AbstractGameLoop {
    private PhongShader shader;
    private Game game;

    /**
     * creates a new, paused gameloop
     * @param targetTps the target frames per second
     */
    public RenderLoop(int targetTps) {
        super("Renderloop", targetTps);
    }

    public void init(Game game) throws IOException {
        this.game = game;
        int maxPointLights = game.settings().MAX_POINT_LIGHTS;
        shader = new PhongShader(maxPointLights);
    }

    @Override
    protected void update(float deltaTime) throws Exception {
        // current time
        game.timer().updateRenderTime();

        // camera
        game.camera().updatePosition(deltaTime); // real-time deltatime

        // shader uniforms
        shader.bind();
        Vector3fc eye = game.camera().getEye();
        shader.setSpecular(1f);
        shader.setAmbientLight(getAmbientLight());
        shader.setCameraPosition(eye);

        // GL object
        Settings s = game.settings();
        SGL gl = new ShaderUniformGL(shader, s.WINDOW_WIDTH, s.WINDOW_HEIGHT, game.camera());
        game.getGamestate().draw(gl);

        // update window
        game.window().update();

        // loop clean
        shader.unbind();

        if (game.window().shouldClose()) stopLoop();
    }

    /**
     * TODO implement day-night cycle
     * @return light color
     */
    private Color4f getAmbientLight() {
        return new Color4f(1, 1, 1, 0.8f);
    }

    @Override
    public void cleanup() {
        shader.cleanup();
    }
}
