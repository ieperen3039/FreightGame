package NG.Entities;

/**
 * @author Geert van Ieperen created on 7-1-2019.
 */
public interface GoodsType {
    /** @return a user-friendly canonical name */
    String name();

    /**
     * @param daysInTransit the number of time units this good is in transit
     * @return the amount of currency that the good is worth
     */
    float valueOverTime(float daysInTransit);
}
