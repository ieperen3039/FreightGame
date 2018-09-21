package NG.Rendering;

import NG.DataStructures.MatrixStack.SGL;
import NG.DataStructures.MatrixStack.ShaderUniformGL;
import NG.Engine.AbstractGameLoop;
import NG.Engine.Game;
import NG.Engine.GameAspect;
import NG.Settings.Settings;
import NG.Shaders.PhongShader;
import NG.Shaders.ShaderProgram;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class RenderLoop extends AbstractGameLoop implements GameAspect {
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

        shaders = new ArrayList<>();
        addShader(new PhongShader(maxPointLights));
    }

    private boolean addShader(ShaderProgram shader) {
        return shaders.add(shader);
    }

    @Override
    protected void update(float deltaTime) {
        Settings sett = game.settings();

        // current time
        game.timer().updateRenderTime();

        // camera
        game.camera().updatePosition(deltaTime); // real-time deltatime

        for (ShaderProgram shader : shaders) {
            // shader uniforms
            shader.bind();
            shader.setUniforms(game);

            // GL object
            SGL gl = new ShaderUniformGL(shader, sett.WINDOW_WIDTH, sett.WINDOW_HEIGHT, game.camera(), true);
            game.state().draw(gl);
            shader.unbind();
        }

        game.painter().draw(sett.WINDOW_WIDTH, sett.WINDOW_HEIGHT, 10, 10, 12);

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
