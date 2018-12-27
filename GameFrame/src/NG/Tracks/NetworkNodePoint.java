package NG.Tracks;

import NG.DataStructures.MatrixStack.SGL;
import NG.Entities.Entity;
import org.joml.Vector2f;
import org.joml.Vector2fc;

/**
 * @author Geert van Ieperen created on 23-12-2018.
 */
public class NetworkNodePoint implements Entity {
    private NetworkNode reference;
    private final Vector2fc position;

    public NetworkNodePoint(Vector2fc position) {
        this.position = position;
    }

    public void setReference(NetworkNode reference) {
        this.reference = reference;
    }

    public NetworkNode getNode() {
        return reference;
    }

    @Override
    public void update() {

    }

    @Override
    public void draw(SGL gl) {

    }

    @Override
    public void onClick(int button) {

    }

    public Vector2fc getPosition() {
        return position;
    }

    public Vector2f vectorTo(NetworkNodePoint bPosition) {
        return new Vector2f(bPosition.position).sub(position);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof NetworkNodePoint) {
            NetworkNodePoint other = (NetworkNodePoint) obj;
            return other.position.equals(this.position);
        }
        return false;
    }

    @Override
    public UpdateFrequency getUpdateFrequency() {
        return UpdateFrequency.NEVER;
    }
}
