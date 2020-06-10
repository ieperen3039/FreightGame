package NG.Entities;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.GUIMenu.Components.SActiveTextArea;
import NG.GUIMenu.Components.SButton;
import NG.GUIMenu.Components.SContainer;
import NG.GUIMenu.Components.SFrame;
import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.SComponentProperties;
import NG.InputHandling.MouseTools.AbstractMouseTool.MouseAction;
import NG.Network.NetworkNode;
import NG.Network.NetworkPosition;
import NG.Network.Schedule;
import NG.Rendering.MatrixStack.SGL;
import NG.Tracks.RailMovement;
import NG.Tracks.TrackPiece;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Geert van Ieperen created on 19-5-2020.
 */
public class Train extends AbstractGameObject implements MovingEntity {
    protected double spawnTime;
    protected double despawnTime = Double.POSITIVE_INFINITY;

    private final RailMovement positionEngine;
    private final List<TrainElement> entities = new CopyOnWriteArrayList<>();

    private final Schedule schedule = new Schedule();
    private Schedule.Node currentTarget = null;

    private static final SComponentProperties BUTTON_PROPERTIES = new SComponentProperties(
            300, 50, false, false, NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER
    );

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
        float totalLength = 0;

        for (TrainElement entity : entities) {
            TrainElement.Properties props = entity.getProperties();
            totalMass += props.mass;
            totalR1 += props.linearResistance;
            totalR2 += props.quadraticResistance;
            totalLength += props.length;

            if (props instanceof Locomotive.Properties) {
                Locomotive.Properties lProps = (Locomotive.Properties) props;
                totalTractiveEffort += lProps.tractiveEffort;
            }
        }

        positionEngine.setProperties(totalTractiveEffort, totalMass, totalR1, totalR2, 5, totalLength);
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

    public NetworkPosition getTarget(NetworkNode currentNode) {
        return getTarget(currentNode, 0);
    }

    public NetworkPosition getTarget(NetworkNode currentNode, int i) {
        if (!updateTarget(currentNode)) return null;

        Schedule.Node nextNode = currentTarget;
        for (int j = 0; j < i; j++) {
            nextNode = schedule.getNextNode(nextNode);
        }

        return nextNode.element;
    }

    /** @return true iff this train has a schedule ; currentTarget is not null */
    private boolean updateTarget(NetworkNode currentNode) {
        if (currentTarget == null) {
            currentTarget = schedule.getFirstNode();
        }

        if (currentTarget == null) return false;

        Set<NetworkNode> targetNodes = currentTarget.element.getNodes();
        if (targetNodes.contains(currentNode)) {
            currentTarget = schedule.getNextNode(currentTarget);
        }

        return true;
    }

    public int getScheduleSize() {
        return schedule.size();
    }

    private class TrainUI extends SFrame {

        public TrainUI() {
            super(Train.this.toString());
            setMainPanel(SContainer.column(
                    new SActiveTextArea(this::getStatus, 50),
                    new SActiveTextArea(() -> String.format("Speed: %6.02f", positionEngine.getSpeed()), 50),
                    new SButton("Start", positionEngine::start, BUTTON_PROPERTIES),
                    new SButton("Stop", positionEngine::stop, BUTTON_PROPERTIES),
                    new SButton("Reverse", positionEngine::reverse, BUTTON_PROPERTIES),
                    new SButton("Schedule", () -> game.gui().addFrame(schedule.getUI(game)), BUTTON_PROPERTIES)
            ));
            pack();
        }

        private String getStatus() {
            if (currentTarget == null) {
                currentTarget = schedule.getFirstNode();
            }
            if (currentTarget != null) {
                if (!positionEngine.hasPath()) {
                    return "Waiting for free path...";
                }

                return "Now heading for " + currentTarget.element;
            }

            if (positionEngine.isStopping()) {
                if (positionEngine.getSpeed() == 0) {
                    return "Stopped";
                }
                return "Stopping...";
            }

            return "No schedule";
        }

    }
}
