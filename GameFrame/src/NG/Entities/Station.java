package NG.Entities;

import NG.Core.GameObject;

/**
 * An immutable station
 * @author Geert van Ieperen created on 29-4-2020.
 */
public interface Station extends GameObject, Entity {
    float getElevation();
}
