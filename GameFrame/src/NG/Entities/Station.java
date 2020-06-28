package NG.Entities;

import NG.Core.GameObject;
import NG.DataStructures.Collision.ColliderEntity;
import NG.DataStructures.Valuta;
import NG.Freight.Cargo;
import NG.Mods.CargoType;
import NG.Network.NetworkPosition;
import NG.Tools.Toolbox;
import org.joml.Vector3fc;

import java.util.Collection;
import java.util.Map;

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

    Valuta sell(Cargo cargo);
}
