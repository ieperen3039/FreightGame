package NG.GUIMenu.Components;

import NG.Camera.Camera;
import NG.Camera.StaticCamera;
import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.Rendering.GLFWWindow;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MatrixStack.SceneShaderGL;
import NG.Rendering.Shaders.SceneShader;
import NG.Tools.Vectors;
import org.joml.*;

import java.lang.Math;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static NG.GUIMenu.Rendering.SFrameLookAndFeel.UIComponent.PANEL;

/**
 * @author Geert van Ieperen created on 18-5-2019.
 */
public class S3DModelDisplay extends SComponent {
    private static final Vector3fc EYE = new Vector3f(0, 0, -1);
    private static final Camera view = new StaticCamera(EYE, Vectors.O, Vectors.Y);
    private static final float MODEL_BASE_SCALING = 1.5f;

    private final int minHeight;
    private final int minWidth;
    private final Game game;
    private final Vector3fc meshSize;
    private final Supplier<Vector3f> viewDirection;
    private Consumer<SGL> renderCall;
    private SceneShader shader;

    /**
     * displays the given mesh as seen from viewDirection on the panel.
     * @param game        the current game object
     * @param height      minimum height of this component
     * @param width       minimum width of this component
     * @param renderCall  the call to draw the target element locally, e.g. {@link NG.Tools.Toolbox#draw3DPointer(SGL)
     *                    Toolbox::draw3DPointer}
     * @param drawSize    bounding box of the mesh to draw. The mesh is assumed to be centered.
     * @param eyePosition a supplier of the eye position, relative to the mesh
     * @param shader      the shader
     */
    public S3DModelDisplay(
            Game game, int height, int width, Consumer<SGL> renderCall,
            AABBf drawSize, Supplier<Vector3f> eyePosition, SceneShader shader
    ) {
        this.minHeight = height;
        this.minWidth = width;
        this.game = game;
        this.renderCall = renderCall;
        this.meshSize = Vectors.sizeOf(drawSize);
        this.viewDirection = eyePosition;
        this.shader = shader;
    }

    @Override
    public int minWidth() {
        return minWidth;
    }

    @Override
    public int minHeight() {
        return minHeight;
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        design.draw(PANEL, screenPosition, getSize());

        design.getPainter().render(() -> {
            shader.bind();
            {
                shader.initialize(game);

                GLFWWindow window = game.window();

                SGL gl = new SceneShaderGL(shader, view, window);
                shader.setPointLight(EYE, Color4f.WHITE, 1f);

                // set relative to pixels
                int windowWidth = window.getWidth();
                int windowHeight = window.getHeight();
                gl.scale(1f / Math.max(windowWidth, windowHeight));

                // get vec to middle
                Vector3f offset = new Vector3f(screenPosition.x(), screenPosition.y(), 0)
                        .add(getWidth() / 2f, getHeight() / 2f, 0)
                        .sub(windowWidth / 2f, windowHeight / 2f, 0)
                        .mul((float) windowWidth / windowHeight)
                        .negate();

                gl.translate(offset);

                // rotation
                Vector3f view = viewDirection.get();
                Quaternionf rotation = new Quaternionf().lookAlong(view, Vectors.Z);
                gl.rotate(rotation);

                // scaling
                Vector3f xyDim = new Vector3f(meshSize).mul(1, 1, 0);
                double xScale = MODEL_BASE_SCALING / xyDim.length(); // scale to size 2
                xScale *= getWidth(); // scale to panel size

                double yScale = MODEL_BASE_SCALING / meshSize.length();
                yScale *= getHeight();

                float scaling = (float) Math.min(xScale, yScale);
                gl.scale(scaling);

                renderCall.accept(gl);
            }
            shader.unbind();
        });
    }
}
