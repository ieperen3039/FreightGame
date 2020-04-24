package NG.Tracks;

import NG.DataStructures.Generic.Color4f;
import NG.Entities.Entity;
import NG.GameState.GameMap;
import NG.InputHandling.ClickShader;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shapes.GenericShapes;
import NG.Rendering.Shapes.Primitives.Collision;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 23-12-2018.
 */
public class NetworkNodePoint implements Entity {
    private static final float NODE_SIZE = 0.5f;
    private final GameMap heightMap;
    private NetworkNode reference;
    private final Vector2fc position;
    private boolean isDisposed = false;
    private static final Color4f TRANSPARENT_WHITE = new Color4f(1f, 1f, 1f, 0.2f);

    public NetworkNodePoint(Vector2fc coordinate, GameMap map) {
        this.heightMap = map;
        this.position = new Vector2f(coordinate);
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
                gl.translate(heightMap.getPosition(position));
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

    @Override
    public Collision getRayCollision(Vector3fc origin, Vector3fc direction) {
        return new Collision(heightMap.getPosition(position));
    }
}
