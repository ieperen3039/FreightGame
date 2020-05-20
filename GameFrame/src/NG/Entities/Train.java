package NG.Entities;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.InputHandling.MouseTools.AbstractMouseTool;
import NG.InputHandling.MouseTools.AbstractMouseTool.MouseAction;
import NG.Network.RailNode;
import NG.Rendering.MatrixStack.SGL;
import NG.Tools.Toolbox;
import NG.Tools.Vectors;
import NG.Tracks.RailMovement;
import NG.Tracks.TrackPiece;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.*;

/**
 * @author Geert van Ieperen created on 19-5-2020.
 */
public class Train extends AbstractGameObject implements MovingEntity {
    private Deque<RailNode> plan = new ArrayDeque<>();
    protected double spawnTime;
    protected double despawnTime = Double.POSITIVE_INFINITY;

    private final RailMovement positionEngine;
    private final List<TrainElement> entities;

    public Train(Game game, double spawnTime, TrackPiece startPiece, float fraction) {
        super(game);
        this.positionEngine = new RailMovement(game, this, spawnTime, startPiece, fraction, true, 1);
        this.spawnTime = spawnTime;
        entities = new ArrayList<>();
    }

    private void addElement(TrainElement e) {
        entities.add(e);
    }

    @Override
    public void update() {
        if (positionEngine.getSpeed() > 5f) {
            positionEngine.setAcceleration(0);
        }

        if (positionEngine.getSpeed() < 4f) {
            positionEngine.setAcceleration(2f);
        }

        positionEngine.update();
    }

    @Override
    public void draw(SGL gl) {
        float displacement = 0;
        double now = game.timer().getRenderTime();
        for (TrainElement entity : entities) {
            Vector3f position = positionEngine.getPosition(now, displacement);
            Quaternionf rotation = positionEngine.getRotation(now, displacement);
            entity.draw(gl, position, rotation, this);
            displacement -= entity.realLength();
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
            double sum = entities.stream().mapToDouble(TrainElement::realLength).sum();
            positionEngine.reverse((float) sum);
        }
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

    public static class Placer extends AbstractMouseTool {
        public Placer(Game game) {
            super(game);
        }

        @Override
        public void apply(Entity entity, int xSc, int ySc) {
            if (getMouseAction() == MouseAction.PRESS_ACTIVATE) {
                if (entity instanceof TrackPiece) {
                    TrackPiece trackPiece = (TrackPiece) entity;
                    Vector3f origin = new Vector3f();
                    Vector3f direction = new Vector3f();
                    Vectors.windowCoordToRay(game, xSc, ySc, origin, direction);

                    float fraction = trackPiece.getFractionOfClosest(origin, direction);

                    double now = game.timer().getGameTime();
                    Train train = new Train(game, now, trackPiece, fraction);
                    train.addElement(new Locomotive());
                    train.addElement(new Locomotive());
                    train.addElement(new Locomotive());
                    game.state().addEntity(train);
                    game.inputHandling().setMouseTool(null);
                }

            } else if (getMouseAction() == MouseAction.PRESS_DEACTIVATE) {
                game.inputHandling().setMouseTool(null);
            }
        }

        @Override
        public void apply(Vector3fc position, int xSc, int ySc) {
            if (getMouseAction() == MouseAction.PRESS_DEACTIVATE) {
                game.inputHandling().setMouseTool(null);
            }
        }
    }
}
