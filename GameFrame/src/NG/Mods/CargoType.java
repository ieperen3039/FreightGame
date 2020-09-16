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
    private float minimumPayment;

    public CargoType(String name, float[] pricePerDay, float minimumPayment) {
        assert pricePerDay.length > 1;
        this.name = name;
        this.pricePerDay = pricePerDay;
        this.minimumPayment = minimumPayment;
    }

    public CargoType(String name, float initialValue, float priceDecreasePerSecond) {
        this(name, new float[]{initialValue, initialValue - priceDecreasePerSecond}, initialValue / 100f);
    }

    /** @return a user-friendly canonical name */
    public String name() {
        return name;
    }

    /**
     * @param secondsInTransit the number of seconds this cargo was in transit
     * @return the amount of currency that the good is worth (rounded down)
     */
    public Valuta value(double secondsInTransit, float distanceTravelled) {
        int completedSecondsInTransit = (int) secondsInTransit;
        float unitPricePerMeter;

        if (completedSecondsInTransit == secondsInTransit) { // edge case
            if (completedSecondsInTransit < pricePerDay.length) {
                unitPricePerMeter = pricePerDay[completedSecondsInTransit];

            } else {
                unitPricePerMeter = pricePerDay[pricePerDay.length - 1];
            }

        } else if (completedSecondsInTransit < pricePerDay.length - 1) {
            float lower = pricePerDay[completedSecondsInTransit];
            float upper = pricePerDay[completedSecondsInTransit + 1];

            float fraction = (float) (secondsInTransit - completedSecondsInTransit);
            unitPricePerMeter = Toolbox.interpolate(lower, upper, fraction);

        } else { // extrapolate using the last two entries
            float lower = pricePerDay[pricePerDay.length - 2];
            float upper = pricePerDay[pricePerDay.length - 1];

            float fraction = (float) (secondsInTransit - pricePerDay.length - 2);
            unitPricePerMeter = Toolbox.interpolate(lower, upper, fraction);
        }

        float correctedDistance = distanceTravelled - MINIMUM_PAYMENT_DISTANCE;
        float unitValue = Math.max(unitPricePerMeter * correctedDistance, minimumPayment);
        return Valuta.ofUnitValue((int) unitValue);
    }

    @Override
    public String toString() {
        return name();
    }
}
