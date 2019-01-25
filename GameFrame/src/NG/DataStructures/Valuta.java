package NG.DataStructures;

/**
 * default implementation of valuta
 * @author Geert van Ieperen created on 21-1-2019.
 */
public class Valuta {
    int quantity;

    public Valuta(int quantity) {
        this.quantity = quantity;
    }

    public float getValue() {
        return quantity;
    }
}
