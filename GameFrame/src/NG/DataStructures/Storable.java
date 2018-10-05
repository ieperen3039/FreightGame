package NG.DataStructures;

import java.io.*;

/**
 * @author Geert van Ieperen. Created on 28-9-2018.
 */
public interface Storable {
    /**
     * writes all the state data of this object to the given output.
     * @param out any stream that reliably transfers data
     * @implNote The written data must be written in exactly the same fashion as {@link
     *         #readFromFile(DataInput)} reads.
     */
    void writeToFile(DataOutput out) throws IOException;

    /**
     * restores this object to the state described on the given input.
     * @param in a stream where this object has been written to.
     * @implNote An object written to a stream using {@link #writeToFile(DataOutput)} should {@link
     *         Object#equals(Object)} an object constructed with a no-arg constructor, where this method is called on
     *         the other end of that same stream.
     */
    void readFromFile(DataInput in) throws IOException;

    /**
     * helper class to make a Storable object also Externalizable
     */
    class AsExternalizable implements Externalizable {
        private final Storable target;

        public AsExternalizable(Storable target) {
            this.target = target;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            target.writeToFile(out);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException {
            target.readFromFile(in);
        }
    }
}