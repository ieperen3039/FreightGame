package NG.Entities;

import NG.Core.GameObject;
import NG.DataStructures.Collision.ColliderEntity;
import NG.DataStructures.Valuta;
import NG.Freight.Cargo;
import NG.Mods.CargoType;
import NG.Network.NetworkPosition;
import NG.Tools.Toolbox;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import static org.joml.Math.cos;
import static org.joml.Math.sin;

/**
 * A station
 * @author Geert van Ieperen created on 29-4-2020.
 */
public interface Station extends GameObject, ColliderEntity, NetworkPosition {
    /**
     * @return the middle of this station
     */
    Vector3fc getPosition();

    Map<CargoType, Integer> getAvailableCargo();

    default Map<CargoType, Integer> getTransferableCargo(Train train) {
        Map<CargoType, Integer> available = getAvailableCargo();
        Map<CargoType, Integer> freeSpace = train.getFreeSpace();
        return Toolbox.getIntersection(freeSpace, available);
    }

    /**
     * loads the given amount of the given cargo type from this station and nearby industries onto the train.
     * @param train    the train to load into
     * @param cargo    the type of cargo to load
     * @param amount   the amount of the given cargo type to load into the train in units
     * @param oldFirst whether loading should prefer old cargo over new cargo.
     * @return true iff the full amount of the given cargo was loaded into this train.
     */
    boolean load(Train train, CargoType cargo, int amount, boolean oldFirst);

    /**
     * returns all cargo types accepted by anyting in range of this station. Cargo that is not accepted can still be
     * deposited.
     */
    Collection<CargoType> getAcceptedCargo();

    /**
     * @param cargo
     * @return
     */
    Valuta sell(Cargo cargo);

    void addTrain(Train train);

    void forEachCorner(Consumer<Vector3fc> action);

    static void forEachCorner(
            Vector3fc position, float length, float orientation, float width, Consumer<Vector3fc> action
    ) {
        float oSin = sin(orientation);
        float oCos = cos(orientation);
        Vector3fc forward = new Vector3f(oCos, oSin, 0).normalize(length / 2f);
        Vector3fc toRight = new Vector3f(oSin, -oCos, 0).normalize(width / 2f);

        Vector3f point = new Vector3f();
        action.accept(point.set(position).add(forward).add(toRight));
        action.accept(point.set(position).add(forward).sub(toRight));
        action.accept(point.set(position).sub(forward).add(toRight));
        action.accept(point.set(position).sub(forward).sub(toRight));
    }
}
