package NG.Rendering;

import NG.Core.AbstractGameLoop;
import NG.Core.Game;
import NG.Core.GameAspect;
import NG.GUIMenu.Rendering.NVGOverlay;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shaders.BlinnPhongShader;
import NG.Rendering.Shaders.SceneShader;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Settings.Settings;
import NG.Tools.Logger;
import NG.Tools.TimeObserver;
import NG.Tools.Toolbox;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;

/**
 * Repeatedly renders a frame of the main camera of the game given by {@link #init(Game)}
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class RenderLoop extends AbstractGameLoop implements GameAspect {
    private final NVGOverlay overlay;
    private Game game;
    private Map<ShaderProgram, RenderBundle> renders;
    private SceneShader defaultShader;

    private TimeObserver timeObserver;
    private boolean accurateTiming = false;

    /**
     * creates a new, paused gameloop
     * @param targetFPS the target frames per second
     */
    public RenderLoop(int targetFPS) {
        super("Renderloop", targetFPS);
        overlay = new NVGOverlay();
        renders = new HashMap<>();

        timeObserver = new TimeObserver((targetFPS / 4) + 1, true);
    }

    public void init(Game game) throws IOException {
        if (this.game != null) return;
        this.game = game;

        Settings settings = game.settings();
        accurateTiming = settings.DEBUG;

        overlay.init(settings.ANTIALIAS_LEVEL);
        overlay.addHudItem((hud) -> {
            if (game.settings().DEBUG) {
                Logger.putOnlinePrint(hud::printRoll);
            }
        });

        defaultShader = new BlinnPhongShader();
    }

    /**
     * generates a new render bundle, which allows adding rendering actions which are executed in order on the given
     * shader. There is no guarantee on execution order between shaders
     * @param shader the shader used, or null to use a basic Phong shading
     * @return a bundle that allows adding rendering options.
     */
    public RenderBundle renderSequence(ShaderProgram shader) {
        return renders.computeIfAbsent(shader == null ? defaultShader : shader, RenderBundle::new);
    }

    @Override
    protected void update(float deltaTime) {
        // Clear framebuffer
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        Toolbox.checkGLError("Pre-loop");
        timeObserver.startNewLoop();

        // current time
        game.timer().updateRenderTime();

        GLFWWindow window = game.window();
        if (window.getWidth() == 0 || window.getHeight() == 0) return;

        // camera
        game.camera().updatePosition(deltaTime); // real-time deltatime

        doTimed("Lights Update", () -> game.lights().update());

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, window.getWidth(), window.getHeight());
        glEnable(GL_LINE_SMOOTH);
        glDisable(GL_CULL_FACE);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        Toolbox.checkGLError(window.toString());

        for (RenderBundle renderBundle : renders.values()) {
            String identifier = renderBundle.shader.getClass().getSimpleName();

            doTimed(identifier, renderBundle::draw);

            Toolbox.checkGLError(identifier);
        }

        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();

        doTimed("GUI", () ->
                overlay.draw(windowWidth, windowHeight, 10, Settings.TOOL_BAR_HEIGHT + 10, 12)
        );

        Toolbox.checkGLError(overlay.toString());

        // update window
        window.update();

        // loop clean
        Toolbox.checkGLError("Render loop");
        if (window.shouldClose()) stopLoop();

        timeObserver.startTiming("Loop Overhead");
    }

    private void doTimed(String identifier, Runnable action) {
        if (accurateTiming) {
            timeObserver.startTiming(identifier);
        }

        action.run();

        if (accurateTiming) {
            glFinish();
            timeObserver.endTiming(identifier);
        }
    }

    public void addHudItem(Consumer<NVGOverlay.Painter> draw) {
        overlay.addHudItem(draw);
    }

    @Override
    public void cleanup() {
        defaultShader.cleanup();
        overlay.cleanup();
    }

    public SceneShader getUIShader() {
        return defaultShader;
    }

    public class RenderBundle {
        private ShaderProgram shader;
        private List<BiConsumer<SGL, Game>> targets;

        public RenderBundle(ShaderProgram shader) {
            this.shader = shader;
            this.targets = new ArrayList<>();
        }

        /**
         * appends the given consumer to the end of the render sequence
         * @return this
         */
        public RenderBundle add(BiConsumer<SGL, Game> drawable) {
            targets.add(drawable);
            return this;
        }

        /**
         * executes the given drawables in order
         */
        public void draw() {
            shader.bind();
            {
                shader.initialize(game);

                // GL object
                SGL gl = shader.getGL(game);

                for (BiConsumer<SGL, Game> tgt : targets) {
                    tgt.accept(gl, game);

                    assert gl.getPosition(new Vector3f(1, 1, 1))
                            .equals(new Vector3f(1, 1, 1)) : "GL object has not been properly restored";
                }
            }
            shader.unbind();
        }
    }
}
