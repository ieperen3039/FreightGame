package NG.DataStructures;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * default implementation of valuta
 * @author Geert van Ieperen created on 21-1-2019.
 */
public class Valuta implements Externalizable {
    public static final Valuta NOTHING = ofUnitValue(0);

    private int quantity = 0;

    public Valuta() {
    }

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

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(quantity);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        quantity = in.readInt();
    }

    public static Valuta ofUnitValue(int quantity) {
        return new Valuta(quantity);
    }

    @Override
    public String toString() {
        return "$" + getDollars();
    }
}
