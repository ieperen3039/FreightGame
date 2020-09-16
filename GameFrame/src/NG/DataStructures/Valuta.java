package NG.DataStructures;

/**
 * default implementation of valuta
 * @author Geert van Ieperen created on 21-1-2019.
 */
public class Valuta {
    public static final Valuta NOTHING = ofUnitValue(0);

    private int quantity;

    private Valuta(int quantity) {
        this.quantity = quantity;
    }

    public float getValueUnits() {
        return quantity;
    }

    public float getDollars() {
        return 10f * quantity;
    }

    public Valuta add(Valuta other) {
        quantity += other.quantity;
        return this;
    }

    public Valuta subtract(Valuta other) {
        quantity -= other.quantity;
        return this;
    }

    public void addUnits(int units) {
        quantity += units;
    }

    public void removeUnits(int units) {
        quantity -= units;
    }

    public Valuta multiply(float scalar) {
        quantity *= scalar;
        return this;
    }

    public static Valuta ofUnitValue(int quantity) {
        return new Valuta(quantity);
    }

    @Override
    public String toString() {
        return "$" + getDollars();
    }
}
