package NG.GameState;

import NG.DataStructures.MatrixStack.SGL;
import NG.Engine.AbstractGameLoop;
import NG.Engine.Game;
import NG.Entities.Entity;
import NG.Entities.MovingEntity;
import NG.Tools.Toolbox;
import org.joml.Vector3fc;
import org.joml.Vector4f;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Geert van Ieperen. Created on 14-9-2018.
 */
public class GameLoop extends AbstractGameLoop implements GameState {
    private final List<Entity> dynamicEntities;
    private Deque<Runnable> postUpdateActionQueue;
    private Game game;

    private final Lock drawLock;

    public GameLoop(String gameName, int targetTps) {
        super("Gameloop " + gameName, targetTps);
        this.dynamicEntities = new ArrayList<>();
        postUpdateActionQueue = new ConcurrentLinkedDeque<>();

        drawLock = new ReentrantLock();
    }

    @Override
    public void init(Game game) {
        this.game = game;
    }

    /**
     * Adds an entity to the gameloop in a synchronized fashion.
     * @param entity the new entity, with only its constructor called
     * @see #defer(Runnable)
     */
    @Override
    public void addEntity(MovingEntity entity) {
        // Thanks to the reentrant mechanism, this may also be executed by a deferred action.
        defer(() -> {
            drawLock.lock();
            try {
                dynamicEntities.add(entity);
            } finally {
                drawLock.unlock();
            }
        });
    }

    /**
     * updates the server state of all objects
     */
    public void update(float deltaTime) {
        runPostUpdateActions();
        dynamicEntities.forEach(Entity::update);
        runPostUpdateActions();
    }

    @Override
    public void draw(SGL gl) {
        drawLock.lock();
        try {
            Toolbox.drawAxisFrame(gl);
            dynamicEntities.forEach(entity -> entity.draw(gl));

        } finally {
            drawLock.unlock();
        }
    }

    @Override
    public Entity getEntityByRay(Vector4f from, Vector4f to) {
        return null;
    }

    @Override
    public List<Storage> getIndustriesByRange(Vector3fc position, int range) {
        final int rangeSq = range * range;
        List<Storage> industries = new ArrayList<>();

        //TODO efficiency
        for (Entity entity : dynamicEntities) {
            if (entity instanceof Storage) {
                Storage industry = (Storage) entity;
                if (industry.getPosition().distanceSquared(position) < rangeSq) {
                    industries.add(industry);
                }
            }
        }

        return industries;
    }

    /** defer is sync'd with {@link #runPostUpdateActions()} */
    public synchronized void defer(Runnable action) {
        postUpdateActionQueue.offer(action);
    }

    private synchronized void runPostUpdateActions() {
        // this is the only method _removing_ things, so anatomicallity is not required
        while (!postUpdateActionQueue.isEmpty()) {
            postUpdateActionQueue.remove().run();
        }
    }

    @Override
    public void cleanup() {
        dynamicEntities.clear();
    }

    @Override
    public void writeToFile(DataOutput out) {

    }

    @Override
    public void readFromFile(DataInput in) {

    }

    @Override
    public boolean processClick(int button, int xSc, int ySc) {
        // TODO allow a listener that catches any click
        return false;
    }
}
