package NG.Entities;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.GUIMenu.Components.SButton;
import NG.GUIMenu.Components.SContainer;
import NG.GUIMenu.Components.SFrame;
import NG.GUIMenu.Components.SInteractiveTextArea;
import NG.InputHandling.MouseTools.AbstractMouseTool.MouseAction;
import NG.Network.NetworkNode;
import NG.Network.RailNode;
import NG.Network.Schedule;
import NG.Rendering.MatrixStack.SGL;
import NG.Tools.NetworkPathFinder;
import NG.Tools.Toolbox;
import NG.Tracks.RailMovement;
import NG.Tracks.TrackPiece;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Geert van Ieperen created on 19-5-2020.
 */
public class Train extends AbstractGameObject implements MovingEntity {
    protected double spawnTime;
    protected double despawnTime = Double.POSITIVE_INFINITY;

    private final RailMovement positionEngine;
    private final List<TrainElement> entities = new CopyOnWriteArrayList<>();

    private Schedule schedule = new Schedule();
    private Schedule.Node currentTarget = null;

    public Train(Game game, double spawnTime, TrackPiece startPiece, float fraction) {
        super(game);
        this.positionEngine = new RailMovement(game, this, spawnTime, startPiece, fraction, true);
        this.spawnTime = spawnTime;
    }

    public void addElement(TrainElement e) {
        entities.add(e);
        updateForceFunction();
    }

    public void removeLastElement() {
        entities.remove(entities.size() - 1);
        updateForceFunction();
    }

    private void updateForceFunction() {
        float totalMass = 0;
        float totalTractiveEffort = 0;
        float totalR1 = 0;
        float totalR2 = 0;

        for (TrainElement entity : entities) {
            TrainElement.Properties props = entity.getProperties();
            totalMass += props.mass;
            totalR1 += props.linearResistance;
            totalR2 += props.quadraticResistance;

            if (props instanceof Locomotive.Properties) {
                Locomotive.Properties lProps = (Locomotive.Properties) props;
                totalTractiveEffort += lProps.tractiveEffort;
            }
        }

        positionEngine.setForceFunction(totalTractiveEffort, totalMass, totalR1, totalR2, 1);
    }

    @Override
    public void update() {
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

    @Override
    public void reactMouse(MouseAction action) {
        if (action == MouseAction.PRESS_ACTIVATE) {
            game.gui().addFrame(new TrainUI());
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

    public NetworkNode.Direction pickNextTrack(TrackPiece currentTrack, RailNode node) {
        NetworkNode networkNode = node.getNetworkNode();

        if (currentTarget == null) {
            currentTarget = schedule.getFirstNode();
        }

        if (currentTarget != null) {
            if (currentTarget.element.getNodes().contains(networkNode)) {
                currentTarget = schedule.getNextNode(currentTarget);
            }

        }

        List<NetworkNode.Direction> options = networkNode.getNext(currentTrack);

        assert options != null : String.format("Node %s not connected to %s", node, currentTrack);
        int size = options.size();
        if (size == 0) {
            return null;

        } else if (size == 1) {
            return options.get(0);

        } else if (currentTarget == null) {
            return options.get(Toolbox.random.nextInt(size));

        } else {
            NetworkPathFinder pathFinder = new NetworkPathFinder(currentTrack, networkNode, currentTarget.element);
            List<NetworkNode> path = pathFinder.call();
            return networkNode.getEntryOfNetwork(path.get(0));
        }
    }

    private class TrainUI extends SFrame {
        public TrainUI() {
            super(Train.this.toString());
            setMainPanel(SContainer.column(
                    new SInteractiveTextArea(() -> currentTarget == null ? "No schedule" : "Now heading for " + currentTarget.element, 50),
                    new SInteractiveTextArea(() -> String.format("Speed: %6.02f", positionEngine.getSpeed()), 50),
                    new SButton("Start", () -> positionEngine.setAcceleration(1f)),
                    new SButton("Reverse", () -> positionEngine.reverse(getLength())),
                    new SButton("Schedule", () -> game.gui().addFrame(schedule.getUI(game)))
            ));
        }

    }
}
