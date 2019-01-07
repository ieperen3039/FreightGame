package NG.ActionHandling;

import NG.DataStructures.Color4f;
import NG.Engine.FreightGame;
import NG.Engine.Game;
import NG.Entities.Entity;
import NG.Rendering.GLFWWindow;
import NG.Rendering.MatrixStack.ShaderUniformGL;
import NG.Rendering.Shaders.AbstractShader;
import NG.Rendering.Shaders.ShaderException;
import NG.Settings.Settings;
import NG.Tools.Directory;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.HashMap;

import static org.lwjgl.opengl.GL11.glReadBuffer;
import static org.lwjgl.opengl.GL11.glReadPixels;

/**
 * @author Geert van Ieperen created on 7-1-2019.
 */
public class ClickShader extends AbstractShader {

    private static final Path VERTEX_PATH = Directory.shaders.getPath("Click", "click.vert");
    private static final Path FRAGMENT_PATH = Directory.shaders.getPath("Click", "click.frag");

    private HashMap<Entity, Integer> mapping;
    private int nextNumber = 0;

    public ClickShader() throws ShaderException, IOException {
        super(VERTEX_PATH, null, FRAGMENT_PATH);
        mapping = new HashMap<>();
    }

    @Override
    public void initialize(Game game) {
    }

    @Override
    public void setPointLight(int lightNumber, Vector4f mPosition, Color4f color) {
        // ignore
    }

    @Override
    public void setMaterial(Color4f diffuse, Color4f specular, float reflectance) {
        // ignore
    }

    @Override
    public void setEntity(Entity entity) {
        mapping.computeIfAbsent(entity, k -> nextNumber++);
    }

    @Override
    public void unsetEntity() {
    }

    /**
     * @param xPos
     * @param yPos
     * @return the color of a given pixel in (r, g, b) value
     */
    private static Vector3i getPixelValue(int xPos, int yPos) {
        glReadBuffer(GL11.GL_BACK);
        int bpp = 4; // Assuming a 32-bit display with a byte each for red, green, blue, and alpha.
        ByteBuffer buffer = BufferUtils.createByteBuffer(bpp);
        glReadPixels(xPos, yPos, 1, 1, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);

        int r = buffer.get();
        int g = buffer.get();
        int b = buffer.get();
        buffer.clear();

        return new Vector3i(r, g, b);
    }

    /**
     * may only be called on the current OpenGL context
     * @param game
     * @param xPos
     * @param yPos
     * @return the entity that is clicked on
     */
    public static Entity getEntity(FreightGame game, int xPos, int yPos) {
        try {
            ClickShader shader = new ClickShader();
            GLFWWindow window = game.window();
            ShaderUniformGL flatColorRender =
                    new ShaderUniformGL(shader, window.getWidth(), window.getHeight(), game.camera(), Settings.ISOMETRIC_VIEW);

            game.state().draw(flatColorRender);
            Vector3i value = ClickShader.getPixelValue(xPos, yPos);
            return null;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }
}
