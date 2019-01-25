package NG.Mods;

import NG.DataStructures.Valuta;
import NG.Entities.Freight;

import java.util.Collection;

/**
 * @author Geert van Ieperen created on 25-1-2019.
 */
public interface FreightMod extends Mod {
    /**
     * @return a collection of all types of freight to be created.
     */
    Collection<FreightType> getTypes();

    /**
     * A material that can be transported.
     * @author Geert van Ieperen created on 7-1-2019.
     * @see Freight
     */
    interface FreightType {
        /** @return a user-friendly canonical name */
        String name();

        /**
         * @param daysInTransit the number of time units this good is in transit
         * @return the amount of currency that the good is worth
         */
        Valuta valueOverTime(float daysInTransit);
    }
}
