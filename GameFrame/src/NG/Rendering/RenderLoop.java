package NG.Rendering;

import NG.Engine.AbstractGameLoop;
import NG.Engine.Game;
import NG.Engine.GameAspect;
import NG.GameState.GameMap;
import NG.GameState.GameState;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MatrixStack.ShaderUniformGL;
import NG.Rendering.Shaders.PhongShader;
import NG.Rendering.Shaders.ShaderProgram;
import NG.ScreenOverlay.ScreenOverlay;
import NG.Settings.Settings;
import NG.Tools.Toolbox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Repeatedly renders a frame of the main camera of the game given by {@link #init(Game)}
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class RenderLoop extends AbstractGameLoop implements GameAspect {
    private final ScreenOverlay overlay;

    private List<ShaderProgram> shaders;
    private Game game;

    /**
     * creates a new, paused gameloop
     * @param targetTps the target frames per second
     */
    public RenderLoop(int targetTps) {
        super("Renderloop", targetTps);
        overlay = new ScreenOverlay();
    }

    public void init(Game game) throws IOException {
        this.game = game;
        int maxPointLights = game.settings().MAX_POINT_LIGHTS;
        overlay.init(game);

        shaders = new ArrayList<>();
        addShader(new PhongShader(maxPointLights));
    }

    private boolean addShader(ShaderProgram shader) {
        return shaders.add(shader);
    }

    @Override
    protected void update(float deltaTime) {
        Toolbox.checkGLError();

        // current time
        game.timer().updateRenderTime();

        // camera
        game.camera().updatePosition(deltaTime); // real-time deltatime

        GameMap world = game.map();
        GameState entities = game.state();
        GLFWWindow window = game.window();
        int windowWidth = window.getWidth();
        int window_height = window.getHeight();

        for (ShaderProgram shader : shaders) {
            // shader uniforms
            shader.bind();
            shader.initialize(game);

            // GL object
            SGL gl = new ShaderUniformGL(shader, windowWidth, window_height, game.camera(), Settings.ISOMETRIC_VIEW);

            entities.drawLights(gl);
            world.draw(gl);
            entities.draw(gl);
            shader.unbind();
        }

        overlay.draw(windowWidth, window_height, 10, 10, 12);

        // update window
        window.update();

        // loop clean
        Toolbox.checkGLError();
        if (window.shouldClose()) stopLoop();
    }

    @Override
    public void cleanup() {
        shaders.forEach(ShaderProgram::cleanup);
    }

    public void addHudItem(Consumer<ScreenOverlay.Painter> draw) {
        overlay.addHudItem(draw);
    }
}
