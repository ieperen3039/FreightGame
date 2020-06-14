package NG.Entities;

import NG.Core.GameObject;
import NG.Network.NetworkPosition;
import org.joml.Vector3fc;

/**
 * A station
 * @author Geert van Ieperen created on 29-4-2020.
 */
public interface Station extends GameObject, Entity, NetworkPosition {
    void loadAvailable(Train train);

    Vector3fc getPosition();
}
