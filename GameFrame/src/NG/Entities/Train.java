package NG.Entities;

import NG.Core.AbstractGameObject;
import NG.Core.Coloring;
import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
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
import NG.InputHandling.KeyControl;
import NG.InputHandling.MouseTool.AbstractMouseTool.MouseAction;
import NG.Mods.CargoType;
import NG.Network.NetworkNode;
import NG.Network.NetworkPosition;
import NG.Network.RailNode;
import NG.Network.Schedule;
import NG.Rendering.MatrixStack.SGL;
import NG.Tools.Logger;
import NG.Tools.NetworkPathFinder;
import NG.Tools.Toolbox;
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
public class Train extends AbstractGameObject implements Entity {
    private static final SComponentProperties BUTTON_PROPERTIES = new SComponentProperties(
            300, 50, false, false, NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER_MIDDLE
    );

    private static final boolean CHOOSE_RANDOM_SPAWN_TRACK = true;
    private static final double TRAIN_PLACEMENT_STUN_TIME = 1;

    protected final List<TrainElement> entities = new CopyOnWriteArrayList<>();

    private final int id;
    private final RailMovement positionEngine;
    private final Coloring coloring = new Coloring(Color4f.WHITE);

    private NetworkPosition storagePosition;
    private double timeOfStore = Double.POSITIVE_INFINITY;
    private double timeOfUnstore = Double.POSITIVE_INFINITY;

    private NetworkPosition temporaryTarget = null;
    private Schedule.Node currentScheduleNode = null;
    protected final Schedule schedule = new Schedule();
    private final List<Schedule.UpdateListener> scheduleUpdateListeners = new ArrayList<>();

    private double loadTimer = Double.NEGATIVE_INFINITY;

    private int maintenancePerSecond = 0;
    private double nextMaintenanceTick;

    private double spawnTime;
    private double despawnTime = Double.POSITIVE_INFINITY;

    public Train(Game game, int id, double spawnTime, TrackPiece startPiece) {
        super(game);
        this.id = id;
        this.spawnTime = spawnTime;
        storagePosition = null;
        positionEngine = new RailMovement(game, this, spawnTime, startPiece, true);
        nextMaintenanceTick = spawnTime;
        addScheduleListener(positionEngine);
    }

    public Train(Game game, int id, double spawnTime, NetworkPosition storagePosition) {
        super(game);
        this.id = id;
        this.spawnTime = spawnTime;
        this.storagePosition = storagePosition;
        TrackPiece anyTrack = storagePosition.getTracks().get(0);
        this.positionEngine = new RailMovement(game, this, spawnTime, anyTrack, true);
        addScheduleListener(positionEngine);
        positionEngine.stop();
        timeOfStore = Double.NEGATIVE_INFINITY;
    }

    @Override
    public void update() {
        double gameTime = game.timer().getGameTime();

        if (storagePosition != null) {
            if (!positionEngine.isStopping()) {
                NetworkPosition currentTarget = getTarget(0);
                autoSpawn(currentTarget);
                nextMaintenanceTick = gameTime;
            }

            return;
        }

        if (currentScheduleNode == null && !schedule.isEmpty()) {
            currentScheduleNode = schedule.getFirstNode();

            positionEngine.onScheduleUpdate(currentScheduleNode.element.target);
        }

        positionEngine.update();

        if (gameTime > nextMaintenanceTick) {
            game.playerStatus().money.removeUnits(maintenancePerSecond);
            nextMaintenanceTick += 1;
        }

        if (currentScheduleNode != null && positionEngine.getSpeed() == 0 && !isLoading()) {
            // check whether we have loading to do
            NetworkPosition target = currentScheduleNode.element.target;

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
                            int cargoAmount = transferableCargo.get(anyType);
                            station.load(this, anyType, cargoAmount, true);

                            Logger.DEBUG.printf("Loading %d %s into %s for %4.01f seconds", cargoAmount, anyType, this, loadTimer - gameTime);

                        } else { // !canDeposit(station) && !canLoad(station)
                            // if there is nothing to transfer, then we are already done, and we should continue our journey
                            goToNext();
                        }
                    }
                }
            }
        }
    }

    private void autoSpawn(NetworkPosition currentTarget) {
        assert (storagePosition instanceof Station);

        // autospawn on the best place available
        List<TrackPiece> tracks = storagePosition.getTracks();
        NetworkPathFinder.Path bestPath = NetworkPathFinder.Path.infinite();

        if (currentTarget == null) {
            TrackPiece track = Toolbox.getRandomConditional(tracks, t -> !t.isOccupied());
            if (track == null) return;

            placeTrain(track, true);
            return;
        }

        // TODO path finding cooldown?
        if (CHOOSE_RANDOM_SPAWN_TRACK) {
            TrackPiece track = Toolbox.getRandomConditional(tracks, t -> !t.isOccupied());
            if (track == null) return;


            NetworkPathFinder.Path pathViaStart = new NetworkPathFinder(
                    track, track.getStartNode().getNetworkNode(), currentTarget
            ).call();

            NetworkPathFinder.Path pathViaEnd = new NetworkPathFinder(
                    track, track.getEndNode().getNetworkNode(), currentTarget
            ).call();

            bestPath = pathViaStart.getPathLength() < pathViaEnd.getPathLength() ? pathViaStart : pathViaEnd;

        } else {
            for (TrackPiece track : tracks) {
                if (track.isOccupied()) continue;

                NetworkPathFinder.Path pathViaStart = new NetworkPathFinder(
                        track, track.getStartNode().getNetworkNode(), currentTarget
                ).call();

                if (pathViaStart.getPathLength() < bestPath.getPathLength()) {
                    bestPath = pathViaStart;
                }

                NetworkPathFinder.Path pathViaEnd = new NetworkPathFinder(
                        track, track.getEndNode().getNetworkNode(), currentTarget
                ).call();

                if (pathViaEnd.getPathLength() < bestPath.getPathLength()) {
                    bestPath = pathViaEnd;
                }
            }
        }

        // no available track
        if (bestPath.isEmpty()) return;
        assert bestPath.size() > 1 : "train is already at destination";

        NetworkNode first = bestPath.removeFirst();
        NetworkNode second = bestPath.removeFirst();
        TrackPiece spawnTrack = first.getEntryOf(second).trackPiece;
        NetworkNode trackEndNode = spawnTrack.getEndNode().getNetworkNode();
        placeTrain(spawnTrack, trackEndNode.equals(second));
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

        game.playerStatus().money.add(income);
    }

    public void placeTrain(TrackPiece startPiece, boolean inPositiveDirection) {
        double timeOfPlacement = game.timer().getGameTime();
        positionEngine.setPosition(timeOfPlacement, startPiece, inPositiveDirection);
        addScheduleListener(positionEngine);
        storagePosition = null;
        timeOfUnstore = game.timer().getGameTime();
        addLoadTime(TRAIN_PLACEMENT_STUN_TIME);
    }

    public void storeTrain(Station target) {
        removeScheduleListener(positionEngine);
        positionEngine.stop();
        storagePosition = target;
        target.addTrain(this);
        timeOfStore = game.timer().getGameTime();
        timeOfUnstore = Double.POSITIVE_INFINITY;
    }

    public void addElement(TrainElement e) {
        entities.add(e);
        updateProperties();
    }

    public TrainElement removeLastElement() {
        TrainElement elt = entities.remove(entities.size() - 1);
        updateProperties();
        return elt;
    }

    private void updateProperties() {
        float totalMass = 0;
        float totalTractiveEffort = 0;
        float totalR1 = 0;
        float totalR2 = 0;
        float totalLength = 0;
        float maxSpeed = Float.POSITIVE_INFINITY;
        float maintenance = 0;

        for (TrainElement entity : entities) {
            TrainElement.Properties props = entity.getProperties();
            totalMass += props.mass;
            totalR1 += props.linearResistance;
            totalR2 += props.quadraticResistance;
            totalLength += props.length;
            maxSpeed = Math.min(maxSpeed, props.maxSpeed);
            maintenance += props.maintenancePerSecond;

            if (props instanceof Locomotive.Properties) {
                Locomotive.Properties lProps = (Locomotive.Properties) props;
                totalTractiveEffort += lProps.tractiveEffort;

            } else if (props instanceof Wagon.Properties) {
                Wagon.Properties wProps = (Wagon.Properties) props;
            }
        }

        positionEngine.setProperties(totalTractiveEffort, totalMass, totalR1, totalR2, 5, totalLength, maxSpeed);
        this.maintenancePerSecond = (int) maintenance + 1;
    }

    @Override
    public void draw(SGL gl) {
        if (entities.isEmpty()) return;
        double now = game.timer().getRenderTime();
        if (now > timeOfStore && now < timeOfUnstore) return;
        // position 0 is on the very front of the first wagon, hence the middle of first wagon is displaced
        float displacement = entities.get(0).getProperties().length / 2;

        for (TrainElement entity : entities) {
            // -displacement because we place front to back
            Vector3f position = positionEngine.getPosition(now, -displacement);
            Quaternionf rotation = positionEngine.getRotation(now, -displacement);

            entity.draw(gl, position, rotation, this, coloring.getColor());
            displacement += entity.getProperties().length;
        }
    }

    public Vector3fc getPosition(double time) {
        if (spawnTime > time || despawnTime < time || time > timeOfStore && time < timeOfUnstore) return null;
        return positionEngine.getPosition(time);
    }

    @Override
    public void reactMouse(MouseAction action, KeyControl keys) {
        assert positionEngine != null : "visible but without position";

        if (action == MouseAction.PRESS_ACTIVATE) {
            if (keys.isControlPressed()) {
                if (positionEngine.isStopping()) {
                    positionEngine.start();
                } else {
                    positionEngine.stop();
                }
            } else {
                openUI();
            }
        }
    }

    /**
     * opens the ui of this train
     */
    public void openUI() {
        game.gui().addFrame(new TrainUI());
    }

    public void setMarking(Coloring.Marking mark) {
        coloring.addMark(mark);
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
            int toStore = entity.getStorableAmount(cargo.getType());

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
        if (temporaryTarget != null) {
            if (scheduleDepth == 0) return temporaryTarget;
            scheduleDepth--;
        }

        Schedule.Node nextNode = null;
        if (!schedule.isEmpty()) {
            if (currentScheduleNode == null) {
                currentScheduleNode = schedule.getFirstNode();
            }
            nextNode = currentScheduleNode;
        }

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
        if (currentScheduleNode != null) {
            NetworkPosition target = currentScheduleNode.element.target;
            NetworkNode networkNode = newNode.getNetworkNode();

            if (target.containsNode(next, networkNode) && !shouldWaitFor(target)) {
                goToNext();
            }
        }
    }

    public boolean shouldWaitFor(NetworkPosition target) {
        if (isLoading()) return true;
        if (currentScheduleNode == null) return false;
        return target == currentScheduleNode.element.target;
    }

    public void addScheduleListener(Schedule.UpdateListener listener) {
        scheduleUpdateListeners.add(listener);
    }

    public void removeScheduleListener(Schedule.UpdateListener listener) {
        scheduleUpdateListeners.remove(listener);
    }

    private void goToNext() {
        if (schedule.size() < 2) return;

        if (currentScheduleNode == null) {
            currentScheduleNode = schedule.getFirstNode();
        } else {
            currentScheduleNode = schedule.getNextNode(currentScheduleNode);
        }

        NetworkPosition target = currentScheduleNode.element.target;
        scheduleUpdateListeners.forEach(l -> l.onScheduleUpdate(target));
    }

    @Override
    public String toString() {
        return "Train " + id;
    }

    public List<TrainElement> getElements() {
        return entities;
    }

    public void start() {
        positionEngine.start();
    }

    @Override
    public void restoreFields(Game game) {
        for (TrainElement entity : entities) {
            entity.restore(game);
        }

        positionEngine.restore(game);
        updateProperties();
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

            if (storagePosition != null) {
                return "Stowed away";
            }

            if (positionEngine.isStopping()) {
                if (positionEngine.getSpeed() == 0) {
                    return "Stopped";
                }
                return "Stopping...";
            }

            if (currentScheduleNode == null) {
                if (schedule.isEmpty()) {
                    return "No schedule";
                } else {
                    return "Waiting for schedule start";
                }
            }

            if (!positionEngine.hasPath()) {
                if (shouldWaitFor(currentScheduleNode.element.target)) {
                    if (positionEngine.getSpeed() == 0) {
                        return "Stopped at station (not loading)";
                    }

                    return "Stopping at station";
                }

                return "Waiting for free path...";
            }

            return "Now heading for " + currentScheduleNode.element;
        }
    }
}
