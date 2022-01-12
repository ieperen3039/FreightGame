package NG.Particles;

import NG.Core.Game;
import NG.Core.GameAspect;
import NG.Rendering.MatrixStack.SGL;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ordinary container for particles
 * @author Geert van Ieperen created on 3-4-2019.
 */
public class GameParticles implements GameAspect, Serializable {
    private final List<ParticleCloud> particles;
    private ParticleCloud newParticles = null;
    private transient Game game;

    public GameParticles() {
        this.particles = new ArrayList<>();
    }

    @Override
    public void init(Game game) throws Exception {
        this.game = game;
    }

    public void add(ParticleCloud cloud) {
        synchronized (this) {
            if (newParticles == null) {
                newParticles = cloud;
            } else {
                newParticles.addAll(cloud);
            }
        }
    }

    public void draw(SGL gl) {
        double now = game.timer().getRenderTime();

        particles.removeIf(cloud -> cloud.disposeIfFaded(now));

        if (newParticles != null) {
            synchronized (this) {
                newParticles.granulate()
                        .peek(ParticleCloud::writeToGL)
                        .forEach(particles::add);

                newParticles = null;
            }
        }

        for (ParticleCloud cloud : particles) {
            gl.render(cloud, null);
        }
    }

    @Override
    public void cleanup() {
        synchronized (this) {
            particles.forEach(ParticleCloud::dispose);
            particles.clear();

            if (newParticles != null) {
                newParticles.dispose();
                newParticles = null;
            }
        }
    }
}
