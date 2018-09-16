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
    private final FreightGame game;
    private PhongShader shader;

    /**
     * creates a new, paused gameloop
     * @param targetTps the target frames per second
     */
    public RenderLoop(FreightGame game, int targetTps) {
        super("Renderloop", targetTps);
        this.game = game;
    }

    public void init() throws IOException {
        shader = new PhongShader(game.settings);
    }

    @Override
    protected void update(float deltaTime) throws Exception {
        // current time
        game.time.updateRenderTime();

        // camera
        game.camera.updatePosition(deltaTime); // real-time deltatime

        // shader uniforms
        shader.bind();
        Vector3fc eye = game.camera.getEye();
        shader.setSpecular(1f);
        shader.setAmbientLight(getAmbientLight());
        shader.setCameraPosition(eye);

        // GL object
        Settings s = game.settings;
        SGL gl = new ShaderUniformGL(shader, s.WINDOW_WIDTH, s.WINDOW_HEIGHT, game.camera);
        game.gamestate.draw(gl);

        // update window
        game.window.update();

        // loop clean
        shader.unbind();

        if (game.window.shouldClose()) stopLoop();
    }

    /**
     * TODO implement day-night cycle
     * @return light color
     */
    private Color4f getAmbientLight() {
        return new Color4f(1, 1, 1, 0.8f);
    }

    @Override
    protected void cleanup() {
        shader.cleanup();
    }
}
