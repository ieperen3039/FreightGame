package NG.DataStructures;

/**
 * default implementation of valuta
 * @author Geert van Ieperen created on 21-1-2019.
 */
public class Valuta {
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

    public void add(Valuta other) {
        quantity += other.quantity;
    }

    public void remove(Valuta other) {
        quantity -= other.quantity;
    }

    public static Valuta ofUnitValue(int quantity) {
        return new Valuta(quantity);
    }
}
