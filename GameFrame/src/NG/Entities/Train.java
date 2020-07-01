package NG.Entities;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.DataStructures.Generic.Pair;
import NG.DataStructures.Valuta;
import NG.Freight.Cargo;
import NG.GUIMenu.Components.SActiveTextArea;
import NG.GUIMenu.Components.SButton;
import NG.GUIMenu.Components.SContainer;
import NG.GUIMenu.Components.SFrame;
import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.SComponentProperties;
import NG.InputHandling.MouseTools.AbstractMouseTool.MouseAction;
import NG.Mods.CargoType;
import NG.Network.NetworkNode;
import NG.Network.NetworkPosition;
import NG.Network.RailNode;
import NG.Network.Schedule;
import NG.Rendering.MatrixStack.SGL;
import NG.Tools.Logger;
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
    private static final SComponentProperties BUTTON_PROPERTIES = new SComponentProperties(
            300, 50, false, false, NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER
    );

    private final int id;
    private final RailMovement positionEngine;
    private final List<TrainElement> entities = new CopyOnWriteArrayList<>();
    private final List<Schedule.UpdateListener> scheduleUpdateListeners = new ArrayList<>();

    private final Schedule schedule = new Schedule();
    private Schedule.Node currentTarget = null;
    private double loadTimer = Double.NEGATIVE_INFINITY;

    protected double spawnTime;
    protected double despawnTime = Double.POSITIVE_INFINITY;
    private Marking marking = new Marking();

    public Train(Game game, int id, double spawnTime, TrackPiece startPiece) {
        super(game);
        this.id = id;
        this.positionEngine = new RailMovement(game, this, spawnTime, startPiece, true);
        addScheduleListener(positionEngine);
        this.spawnTime = spawnTime;
    }

    @Override
    public void update() {
        double gameTime = game.timer().getGameTime();
        positionEngine.update();

        Schedule.Node currentTarget = getCurrentTarget();
        if (positionEngine.getSpeed() == 0 && !isLoading() && currentTarget != null) {
            // check whether we have loading to do
            NetworkPosition target = currentTarget.element.target;

            if (target instanceof Station) {
                Station station = (Station) target;

                TrackPiece currentTrack = positionEngine.getTracksAt(gameTime).left;
                RailNode endNodeR = currentTrack.getEndNode();
                NetworkNode endNodeN = endNodeR.getNetworkNode();

                boolean isWithinLoadingArea = target.containsNode(currentTrack, endNodeN);
                if (isWithinLoadingArea) {
                    if (canDeposit(station)) {
                        depositAvailable(station);

                    } else {
                        Map<CargoType, Integer> transferableCargo = station.getTransferableCargo(this);
                        if (!transferableCargo.isEmpty()) {
                            CargoType anyType = transferableCargo.keySet().iterator().next();
                            station.load(this, anyType, transferableCargo.get(anyType), true);

                            Logger.DEBUG.printf("Loading %s for %4.01f seconds", this, loadTimer - gameTime);

                        } else { // !canDeposit(station) && !canLoad(station)
                            // if there is nothing to transfer, then we are already done, and we should continue our journey
                            goToNext();
                        }
                    }
                }
            }
        }

        positionEngine.discardUpTo(gameTime - 1.0);
    }

    /** station -> train */
    private boolean canLoad(Station station) {
        return station.getTransferableCargo(this).isEmpty();
    }

    /** train -> station */
    private boolean canDeposit(Station station) {
        Collection<CargoType> acceptedCargo = station.getAcceptedCargo();
        for (TrainElement entity : entities) {
            Pair<CargoType, Integer> contents = entity.getContents();
            if (acceptedCargo.contains(contents.left) && contents.right > 0) {
                return true;
            }
        }

        return false;
    }

    protected void depositAvailable(Station storage) {
        Collection<CargoType> acceptedCargo = storage.getAcceptedCargo();
        Valuta income = Valuta.ofUnitValue(0);

        for (TrainElement entity : entities) {
            // if these contents can be sold
            if (acceptedCargo.contains(entity.getCurrentCargoType())) {
                // sell
                Collection<Cargo> elts = entity.takeAll();
                for (Cargo cargo : elts) {
                    addLoadTime(entity.getLoadTime(cargo));
                    Valuta sellValue = storage.sell(cargo);
                    income.add(sellValue);
                }
            }
        }

        // TODO add income to player pocket
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

    @Override
    public void setMarking(Marking marking) {
        this.marking = marking;
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
        boolean complete = false;

        double loadTime = 0;
        for (TrainElement entity : entities) {
            int toStore = entity.getStorableAmount(cargo.type);

            if (toStore > 0) {
                if (toStore >= cargo.quantity()) {
                    loadTime += entity.getLoadTime(cargo);
                    entity.addContents(cargo);
                    complete = true;
                    break;

                } else {
                    Cargo part = cargo.split(toStore);
                    loadTime += entity.getLoadTime(part);
                    entity.addContents(part);
                }
            }
        }

        addLoadTime(loadTime);

        return complete;
    }

    protected void addLoadTime(double loadTime) {
        double gameTime = game.timer().getGameTime();
        if (loadTimer < gameTime) loadTimer = gameTime;
        loadTimer = loadTimer + loadTime;
    }

    @Override
    public void despawn(double gameTime) {
        despawnTime = gameTime;
        positionEngine.removePath();
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

        return nextNode.element.target;
    }

    public boolean isLoading() {
        return loadTimer > game.timer().getGameTime();
    }

    public void onArrival(TrackPiece next, RailNode newNode) {
        Schedule.Node currentTarget = getCurrentTarget();
        if (currentTarget != null) {
            NetworkPosition target = currentTarget.element.target;
            NetworkNode networkNode = newNode.getNetworkNode();

            if (target.containsNode(next, networkNode) && !shouldWaitFor(target)) {
                goToNext();
            }
        }
    }

    public boolean shouldWaitFor(NetworkPosition target) {
        if (isLoading()) return true;

        Schedule.Node currentTarget = getCurrentTarget();
        if (currentTarget == null) return false;

        return target == currentTarget.element.target;
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
            NetworkPosition target = this.currentTarget.element.target;
            scheduleUpdateListeners.forEach(l -> l.onScheduleUpdate(target));
        }
    }

    private Schedule.Node getCurrentTarget() {
        if (schedule.isEmpty()) return null;

        if (currentTarget == null) {
            currentTarget = schedule.getFirstNode();
        }

        return currentTarget;
    }

    @Override
    public String toString() {
        return "Train " + id;
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
            if (isLoading()) {
                double time = loadTimer - game.timer().getGameTime();
                return String.format("Loading / Unloading (%4.01f sec left)", time);
            }

            if (positionEngine.isStopping()) {
                if (positionEngine.getSpeed() == 0) {
                    return "Stopped";
                }
                return "Stopping...";
            }

            Schedule.Node target = getCurrentTarget();

            if (target == null) {
                return "No schedule";
            }

            if (!positionEngine.hasPath()) {
                if (shouldWaitFor(target.element.target)) {
                    if (positionEngine.getSpeed() == 0) {
                        return "Stopped at station (not loading)";
                    }

                    return "Stopping at station";
                }

                return "Waiting for free path...";
            }

            return "Now heading for " + target.element;
        }
    }
}
