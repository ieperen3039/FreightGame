package NG.Entities;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.DataStructures.Generic.Pair;
import NG.Freight.Cargo;
import NG.GUIMenu.Components.SActiveTextArea;
import NG.GUIMenu.Components.SButton;
import NG.GUIMenu.Components.SContainer;
import NG.GUIMenu.Components.SFrame;
import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.SComponentProperties;
import NG.GameState.Storage;
import NG.InputHandling.MouseTools.AbstractMouseTool.MouseAction;
import NG.Mods.CargoType;
import NG.Network.NetworkNode;
import NG.Network.NetworkPosition;
import NG.Network.RailNode;
import NG.Network.Schedule;
import NG.Rendering.MatrixStack.SGL;
import NG.Tracks.RailMovement;
import NG.Tracks.TrackPiece;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Geert van Ieperen created on 19-5-2020.
 */
public class Train extends AbstractGameObject implements MovingEntity {
    protected double spawnTime;
    protected double despawnTime = Double.POSITIVE_INFINITY;

    private final RailMovement positionEngine;
    private final List<TrainElement> entities = new CopyOnWriteArrayList<>();
    private final List<Schedule.UpdateListener> scheduleUpdateListeners = new ArrayList<>();

    private final Schedule schedule = new Schedule();
    private Schedule.Node currentTarget = null;
    private double loadTimer = Double.NEGATIVE_INFINITY;

    private static final SComponentProperties BUTTON_PROPERTIES = new SComponentProperties(
            300, 50, false, false, NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER
    );

    public Train(Game game, double spawnTime, TrackPiece startPiece) {
        super(game);
        this.positionEngine = new RailMovement(game, this, spawnTime, startPiece, true);
        addScheduleListener(positionEngine);
        this.spawnTime = spawnTime;
    }

    @Override
    public void update() {
        positionEngine.update();

        Schedule.Node currentTarget = getCurrentTarget();
        if (positionEngine.getSpeed() == 0 && !isLoading() && currentTarget != null) {
            // check whether we have loading to do
            NetworkPosition target = currentTarget.element;

            if (target instanceof Storage) {
                double gameTime = game.timer().getGameTime();
                TrackPiece currentTrack = positionEngine.getTracksAt(gameTime).left;
                NetworkNode startNode = currentTrack.getStartNode().getNetworkNode();
                NetworkNode endNode = currentTrack.getEndNode().getNetworkNode();

                Set<NetworkNode> targetNodes = target.getNodes();
                // if both ends of our current track are part of this same target, we assume we are on the target itself
                if (targetNodes.contains(startNode) && targetNodes.contains(endNode)) {
                    Map<CargoType, Integer> transferableCargo = Storage.getTransferableCargo((Storage) target, this);
                    // if there is nothing to transfer, then we are already done, and we should continue our journey
                    if (transferableCargo.isEmpty()) {
                        goToNext();

                    } else { // otherwise, start loading
                        Storage storage = (Storage) target;
                        storage.load(this, transferableCargo);
                    }
                }
            }
        }
    }

    public void addElement(TrainElement e) {
        entities.add(e);
        updateProperties();
    }

    public void removeLastElement() {
        entities.remove(entities.size() - 1);
        updateProperties();
    }

    private void updateProperties() {
        float totalMass = 0;
        float totalTractiveEffort = 0;
        float totalR1 = 0;
        float totalR2 = 0;
        float totalLength = 0;
        float maxSpeed = Float.POSITIVE_INFINITY;

        for (TrainElement entity : entities) {
            TrainElement.Properties props = entity.getProperties();
            totalMass += props.mass;
            totalR1 += props.linearResistance;
            totalR2 += props.quadraticResistance;
            totalLength += props.length;
            maxSpeed = Math.min(maxSpeed, props.maxSpeed);

            if (props instanceof Locomotive.Properties) {
                Locomotive.Properties lProps = (Locomotive.Properties) props;
                totalTractiveEffort += lProps.tractiveEffort;

            } else if (props instanceof Wagon.Properties) {
                Wagon.Properties wProps = (Wagon.Properties) props;
            }
        }

        positionEngine.setProperties(totalTractiveEffort, totalMass, totalR1, totalR2, 5, totalLength, maxSpeed);
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

    public Map<CargoType, Integer> getFreeSpace() {
        Map<CargoType, Integer> capacity = new HashMap<>();

        for (TrainElement entity : entities) {
            Pair<CargoType, Integer> contents = entity.getContents();
            if (contents.right > 0) {
                int typeCapacity = entity.getCargoTypes().get(contents.left);
                capacity.put(contents.left, typeCapacity - contents.right);

            } else {
                Map<CargoType, Integer> cargoTypes = entity.getCargoTypes();
                for (CargoType type : cargoTypes.keySet()) {
                    capacity.merge(type, cargoTypes.get(type), Integer::sum);
                }
            }
        }

        return capacity;
    }

    public Map<CargoType, Integer> getContents() {
        Map<CargoType, Integer> capacity = new HashMap<>();

        for (TrainElement entity : entities) {
            Pair<CargoType, Integer> contents = entity.getContents();
            if (contents.right > 0) {
                capacity.merge(contents.left, contents.right, Integer::sum);
            }
        }

        return capacity;
    }

    /** true iff the cargo has been stored in its entirety */
    public boolean store(Cargo cargo) {
        assert cargo.quantity() > 0 : cargo;

        for (TrainElement entity : entities) {
            int toStore = entity.getStorableAmount(cargo.type);

            if (toStore > 0) {
                if (toStore >= cargo.quantity()) {
                    store(cargo, entity);
                    return true;

                } else {
                    Cargo part = cargo.split(toStore);
                    store(part, entity);
                }
            }
        }

        return false;
    }

    private void store(Cargo cargo, TrainElement entity) {
        double loadTime = entity.addContents(cargo);
        loadTimer = Math.max(game.timer().getGameTime() + loadTime, loadTimer);
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

    /**
     * @param scheduleDepth the number of targets in the schedule to look ahead, for any signed value
     * @return the target on the given number of steps from the current target, or null if the schedule is empty
     */
    public NetworkPosition getTarget(int scheduleDepth) {
        Schedule.Node nextNode = getCurrentTarget();
        if (nextNode == null) return null;

        if (scheduleDepth > 0) {
            for (int j = 0; j < scheduleDepth; j++) {
                nextNode = schedule.getNextNode(nextNode);
            }

        } else if (scheduleDepth < 0) {
            for (int j = 0; j > scheduleDepth; j--) {
                nextNode = schedule.getPreviousNode(nextNode);
            }
        }

        return nextNode.element;
    }

    public boolean isLoading() {
        return loadTimer > game.timer().getGameTime();
    }

    public void onArrival(RailNode newNode) {
        Schedule.Node currentTarget = getCurrentTarget();
        if (currentTarget != null) {
            NetworkPosition target = currentTarget.element;
            NetworkNode networkNode = newNode.getNetworkNode();
            if (target.getNodes().contains(networkNode)) {
                if (!shouldWaitFor(target)) {
                    goToNext();
                }
            }
        }
    }

    public boolean shouldWaitFor(NetworkPosition target) {
        if (isLoading()) return true;

        Schedule.Node currentTarget = getCurrentTarget();
        if (currentTarget == null) return false;
        if (target != currentTarget.element) return false;

        if (target instanceof Storage) {
            Map<CargoType, Integer> transferableCargo = Storage.getTransferableCargo((Storage) target, this);
            return !transferableCargo.isEmpty();
        }

        return false;
    }

    public void addScheduleListener(Schedule.UpdateListener listener) {
        scheduleUpdateListeners.add(listener);
    }

    public void removeScheduleListener(Schedule.UpdateListener listener) {
        scheduleUpdateListeners.remove(listener);
    }

    private void goToNext() {
        Schedule.Node currentTarget = getCurrentTarget();
        if (currentTarget == null) {
            this.currentTarget = schedule.getFirstNode();

        } else {
            this.currentTarget = schedule.getNextNode(currentTarget);
        }

        if (this.currentTarget != null) {
            scheduleUpdateListeners.forEach(l -> l.onScheduleUpdate(this.currentTarget.element));
        }
    }

    public Schedule.Node getCurrentTarget() {
        if (schedule.isEmpty()) return null;

        if (currentTarget == null) {
            currentTarget = schedule.getFirstNode();
        }

        return currentTarget;
    }

    private class TrainUI extends SFrame {

        public TrainUI() {
            super(Train.this.toString());
            setMainPanel(SContainer.column(
                    new SActiveTextArea(this::getStatus, 50),
                    new SActiveTextArea(() -> String.format("Speed: %5.01f", positionEngine.getSpeed()), 50),
                    new SActiveTextArea(() -> String.format("Cargo: %s", getContents()), 50),
                    new SButton("Start", positionEngine::start, BUTTON_PROPERTIES),
                    new SButton("Stop", positionEngine::stop, BUTTON_PROPERTIES),
                    new SButton("Reverse", positionEngine::reverse, BUTTON_PROPERTIES),
                    new SButton("Schedule", () -> game.gui()
                            .addFrame(new Schedule.ScheduleUI(game, schedule)), BUTTON_PROPERTIES)
            ));
            pack();
        }

        private String getStatus() {
            if (getCurrentTarget() == null) {
                currentTarget = schedule.getFirstNode();
            }
            if (getCurrentTarget() != null) {
                if (!positionEngine.hasPath()) {
                    return "Waiting for free path...";
                }

                return "Now heading for " + getCurrentTarget().element;
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
