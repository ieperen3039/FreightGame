package NG.Entities;

import NG.DataStructures.Valuta;

/**
 * A material that can be transported.
 * @author Geert van Ieperen created on 7-1-2019.
 * @see Freight
 */
public interface FreightType {
    /** @return a user-friendly canonical name */
    String name();

    /**
     * @param daysInTransit the number of time units this good is in transit
     * @return the amount of currency that the good is worth
     */
    Valuta valueOverTime(float daysInTransit);
}
