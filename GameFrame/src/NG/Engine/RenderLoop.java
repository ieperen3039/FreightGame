package NG.Engine;

import NG.DataStructures.MatrixStack.SGL;
import NG.DataStructures.MatrixStack.ShaderUniformGL;
import NG.Settings.Settings;
import NG.Shaders.PhongShader;
import NG.Shaders.ShaderProgram;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class RenderLoop extends AbstractGameLoop {
    private List<ShaderProgram> shaders;
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

        shaders = Arrays.asList(
                new PhongShader(maxPointLights) // todo modular shaders
        );
    }

    @Override
    protected void update(float deltaTime) {
        // current time
        game.timer().updateRenderTime();

        // camera
        game.camera().updatePosition(deltaTime); // real-time deltatime

        for (ShaderProgram shader : shaders) {
            // shader uniforms
            shader.bind();
            shader.setUniforms(game);

            // GL object
            Settings s = game.settings();
            SGL gl = new ShaderUniformGL(shader, s.WINDOW_WIDTH, s.WINDOW_HEIGHT, game.camera(), true);
            game.getGamestate().draw(gl);
            shader.unbind();
        }

        // update window
        game.window().update();

        // loop clean

        if (game.window().shouldClose()) stopLoop();
    }

    @Override
    public void cleanup() {
        shaders.forEach(ShaderProgram::cleanup);
    }
}
