package NG.Rendering.Lights;

import NG.Camera.Camera;
import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shaders.DepthShader;
import NG.Rendering.Shaders.LightShader;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Rendering.Shaders.ShadowMap;
import NG.Tools.Directory;
import NG.Tools.Toolbox;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author Geert van Ieperen created on 3-2-2019.
 */
public class SingleShadowMapLights implements GameLights {
    private static final float UPDATE_MARGIN = 0.5f;

    private final Lock pointLightEditLock;
    private final Lock pointLightReadLock;

    private final List<PointLight> lights;
    private DirectionalLight sunLight;

    private Game game;
    private DepthShader shadowShader;

    private float lightDist = 1;

    public SingleShadowMapLights() {
        ReadWriteLock rwl = new ReentrantReadWriteLock(false);
        this.pointLightEditLock = rwl.writeLock();
        this.pointLightReadLock = rwl.readLock();

        this.lights = new ArrayList<>();
        // these are overridden by #addDirectionalLight
        this.sunLight = new DirectionalLight(Color4f.WHITE, new Vector3f(1, -1, 1), 0.5f);
    }

    @Override
    public void init(Game game) throws Exception {
        this.game = game;
        Future<DepthShader> shader = game.computeOnRenderThread(DepthShader::new);

        this.sunLight.init(game);

        this.shadowShader = shader.get();
    }

    @Override
    public void addPointLight(PointLight light) {
        pointLightEditLock.lock();
        try {
            lights.add(light);
        } finally {
            pointLightEditLock.unlock();
        }
    }

    @Override
    public void addDirectionalLight(Vector3fc origin, Color4f color, float intensity) {
        sunLight.setDirection(origin);
        sunLight.setColor(color);
        sunLight.setIntensity(intensity);
    }

    @Override
    public void update() {
        Camera camera = game.camera();
        Vector3fc playerFocus = camera.getFocus();
        float viewDist = camera.vectorToFocus().length();
        Vector3fc lightFocus = sunLight.getLightCenter();

        if (playerFocus.distanceSquared(lightFocus) > UPDATE_MARGIN * UPDATE_MARGIN) {
            sunLight.setLightCenter(playerFocus);

        } else if (Math.abs(viewDist - lightDist) > UPDATE_MARGIN) {
            sunLight.setLightSize(viewDist + UPDATE_MARGIN);
            lightDist = viewDist;
        }

        if (sunLight.doDynamicShadows()) {
            // shadow render
            shadowShader.bind();
            {
                glClear(GL_DEPTH_BUFFER_BIT);
                shadowShader.initialize(game);

                if (sunLight.doDynamicShadows()) {
                    DepthShader.DepthGL gl = shadowShader.getGL(game);
                    shadowShader.setDirectionalLight(sunLight);

                    game.map().draw(gl);
                    game.state().draw(gl);

                    gl.cleanup();
                }
            }
            shadowShader.unbind();

            Toolbox.checkGLError(toString());
        }
    }

    public void print(String name, int res) {
        glReadBuffer(GL_BACK);
        int bpp = 4; // Assuming a 32-bit display with a byte each for red, green, blue, and alpha.
        ByteBuffer buffer = BufferUtils.createByteBuffer(res * res * bpp);
        GL11.glReadPixels(0, 0, res, res, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        new Thread(() ->
                Toolbox.writePNG(Directory.screenshots, name, buffer, bpp, res, res),
                "Writing frame to disc"
        ).start();
    }

    @Override
    public void dumpShadowMap(Directory dir) {
        if (sunLight.doDynamicShadows()) {
            ShadowMap dsm = sunLight.getDynamicShadowMap();
            dsm.dump("dynamic");
        }
    }

    @Override
    public Matrix4fc getLightMatrix() {
        return sunLight.getLightSpaceMatrix();
    }

    @Override
    public void draw(SGL gl) {
        ShaderProgram shader = gl.getShader();
        if (shader instanceof LightShader) {
            LightShader lightShader = (LightShader) shader;

            pointLightReadLock.lock();
            try {
                for (PointLight light : lights) {
                    Vector3fc mPosition = gl.getPosition(light.getPosition());
                    lightShader.setPointLight(mPosition, light.getColor(), light.getIntensity());
                }
            } finally {
                pointLightReadLock.unlock();
            }

            lightShader.setDirectionalLight(sunLight);
        }
    }

    @Override
    public void cleanup() {
        shadowShader.cleanup();
        sunLight.cleanup();
        lights.clear();
    }
}
