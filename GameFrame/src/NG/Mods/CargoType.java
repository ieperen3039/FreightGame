package NG.Mods;

import NG.DataStructures.Valuta;
import NG.Freight.Cargo;
import NG.Tools.Toolbox;

/**
 * A material that can be transported.
 * @author Geert van Ieperen created on 7-1-2019.
 * @see Cargo
 */
public class CargoType {
    public static final CargoType NO_CARGO = new CargoType("No Freight", 0, 0);

    private static final float MINIMUM_PAYMENT_DISTANCE = 10;
    private final String name;
    private final float[] pricePerDay;

    public CargoType(String name, float[] pricePerDay) {
        assert pricePerDay.length > 1;
        this.name = name;
        this.pricePerDay = pricePerDay;
    }

    public CargoType(String name, float initialValue, float priceDecreasePerDay) {
        this(name, new float[]{initialValue, initialValue - priceDecreasePerDay});
    }

    /** @return a user-friendly canonical name */
    public String name() {
        return name;
    }

    /**
     * @param daysInTransit the number of time units this good is in transit
     * @return the amount of currency that the good is worth (rounded down)
     */
    public Valuta value(double daysInTransit, float distanceTravelled) {
        int fullDaysInTransit = (int) daysInTransit;
        float unitPricePerMeter;

        if (fullDaysInTransit == daysInTransit) { // edge case
            if (fullDaysInTransit < pricePerDay.length) {
                unitPricePerMeter = pricePerDay[fullDaysInTransit];

            } else {
                unitPricePerMeter = pricePerDay[pricePerDay.length - 1];
            }

        } else if (fullDaysInTransit < pricePerDay.length - 1) {
            float lower = pricePerDay[fullDaysInTransit];
            float upper = pricePerDay[fullDaysInTransit + 1];

            float fraction = (float) (daysInTransit - fullDaysInTransit);
            unitPricePerMeter = Toolbox.interpolate(lower, upper, fraction);

        } else { // extrapolate using the last two entries
            float lower = pricePerDay[pricePerDay.length - 2];
            float upper = pricePerDay[pricePerDay.length - 1];

            float fraction = (float) (daysInTransit - pricePerDay.length - 2);
            unitPricePerMeter = Toolbox.interpolate(lower, upper, fraction);
        }

        float correctedDistance = distanceTravelled - MINIMUM_PAYMENT_DISTANCE;
        float unitValue = unitPricePerMeter * correctedDistance;
        return Valuta.ofUnitValue((int) unitValue);
    }

    @Override
    public String toString() {
        return name();
    }
}
