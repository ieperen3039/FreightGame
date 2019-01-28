package NG.GameState;

import NG.DataStructures.FreightStorage;
import NG.Entities.Entity;
import NG.Entities.Freight;
import org.joml.Vector2f;
import org.joml.Vector2fc;

/**
 * A storage is any building that stores {@link Freight}. This includes stations, industries and possibly a rearranging
 * terrain, but not a train.
 * @author Geert van Ieperen. Created on 12-11-2018.
 */
public abstract class Storage implements Entity {
    private FreightStorage contents;
    private Vector2f position;
    private boolean isDisposed = false;

    public Storage(Vector2f position) {
        this.position = position;
        this.contents = new FreightStorage();
    }

    protected FreightStorage contents() {
        return contents;
    }

    public void setPosition(Vector2fc position) {
        this.position.set(position);
    }

    public Vector2fc getPosition() {
        return position;
    }

    @Override
    public void dispose() {
        isDisposed = true;
    }

    @Override
    public boolean isDisposed() {
        return isDisposed;
    }
}
