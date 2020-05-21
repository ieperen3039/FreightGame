package NG.Entities;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.InputHandling.MouseTools.AbstractMouseTool.MouseAction;
import NG.Network.RailNode;
import NG.Rendering.MatrixStack.SGL;
import NG.Tools.Toolbox;
import NG.Tracks.RailMovement;
import NG.Tracks.TrackPiece;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Geert van Ieperen created on 19-5-2020.
 */
public class Train extends AbstractGameObject implements MovingEntity {
    private Deque<RailNode> plan = new ArrayDeque<>();
    protected double spawnTime;
    protected double despawnTime = Double.POSITIVE_INFINITY;

    private final RailMovement positionEngine;
    private final List<TrainElement> entities = new CopyOnWriteArrayList<>();

    public Train(Game game, double spawnTime, TrackPiece startPiece, float fraction) {
        super(game);
        this.positionEngine = new RailMovement(game, this, spawnTime, startPiece, fraction, true, 0);
        this.spawnTime = spawnTime;
    }

    public void addElement(TrainElement e) {
        entities.add(e);
    }

    public void removeLastElement() {
        entities.remove(entities.size() - 1);
    }

    @Override
    public void update() {
        if (positionEngine.getSpeed() > 5f) {
            positionEngine.setAcceleration(0);
        }

        positionEngine.update();
    }

    @Override
    public void draw(SGL gl) {
        if (entities.isEmpty()) return;
        // position 0 is on the very front of the first wagon, hence the middle of first wagon is displaced
        float displacement = entities.get(0).getProperties().length / 2;
        double now = game.timer().getRenderTime();

        for (TrainElement entity : entities) {
            // -displacement because we place front to back
            Vector3f position = positionEngine.getPosition(now, -displacement);
            Quaternionf rotation = positionEngine.getRotation(now, -displacement);
            entity.draw(gl, position, rotation, this);
            displacement += entity.getProperties().length;
        }
    }

    @Override
    public Vector3fc getPosition(double time) {
        if (spawnTime > time || despawnTime < time) return null;
        return positionEngine.getPosition(time);
    }

    /**
     * queries the next action that this locomotive is going to take based on the given directions. If the plan is
     * 'null', this method prefers dead ends.
     * @param options the directions it may choose from
     * @return the chosen direction
     */
    public RailNode.Direction pickNextTrack(List<RailNode.Direction> options) {
        assert !options.isEmpty();
        RailNode plannedNext = plan.peek();

        RailNode.Direction result = null;
        float shortest = Float.POSITIVE_INFINITY;

        for (RailNode.Direction dir : options) {
            if (Objects.equals(dir.networkNode, plannedNext)) {
                if (dir.distanceToNetworkNode < shortest) {
                    result = dir;
                }
                break;
            }
        }

        if (result != null) return result;

        int i = Toolbox.random.nextInt(options.size());
        result = options.get(i);

        return result;
    }

    @Override
    public void reactMouse(MouseAction action) {
        if (action == MouseAction.PRESS_ACTIVATE) {
            if (positionEngine.getSpeed() > 0) {
                positionEngine.reverse(getLength());
            }
            positionEngine.setAcceleration(2f);
        }
    }

    public float getLength() {
        float sum = 0.0f;

        for (TrainElement entity : entities) {
            sum += entity.getProperties().length;
        }

        return sum;
    }

    @Override
    public void despawn(double gameTime) {
        despawnTime = gameTime;
    }

    @Override
    public double getSpawnTime() {
        return spawnTime;
    }

    @Override
    public double getDespawnTime() {
        return despawnTime;
    }
}
