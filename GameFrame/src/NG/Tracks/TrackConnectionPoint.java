package NG.Tracks;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.Entities.Entity;
import NG.InputHandling.ClickShader;
import NG.Network.NetworkNode;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shapes.GenericShapes;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A reference point to build tracks with.
 * @author Geert van Ieperen created on 23-12-2018.
 */
public class TrackConnectionPoint extends AbstractGameObject implements Entity {
    private static final float NODE_SIZE = 0.5f;
    private NetworkNode reference;
    private final Vector3f position;
    private boolean isDisposed = false;

    public TrackConnectionPoint(Game game, Vector3fc coordinate) {
        super(game);
        this.position = new Vector3f(coordinate);
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
        if (gl.getShader() instanceof ClickShader) {
            gl.pushMatrix();
            {
                gl.translate(position);
                gl.scale(NODE_SIZE, NODE_SIZE, NODE_SIZE);
                gl.render(GenericShapes.CUBE, this);
            }
            gl.popMatrix();
        }
    }

    @Override
    public void onClick(int button) {

    }

    @Override
    public void dispose() {
        isDisposed = true;
    }

    @Override
    public boolean isDisposed() {
        return isDisposed;
    }

    public Vector3fc getPosition() {
        return position;
    }

    public void setPosition(Vector3fc newPosition) {
        position.set(newPosition);
    }

    public Vector3f vectorTo(TrackConnectionPoint bPosition) {
        return new Vector3f(bPosition.position).sub(position);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof TrackConnectionPoint) {
            TrackConnectionPoint other = (TrackConnectionPoint) obj;
            return other.position.equals(this.position);
        }
        return false;
    }

    @Override
    public UpdateFrequency getUpdateFrequency() {
        return UpdateFrequency.NEVER;
    }

}
