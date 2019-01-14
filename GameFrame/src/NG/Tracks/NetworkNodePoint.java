package NG.Tracks;

import NG.ActionHandling.ClickShader;
import NG.Entities.Entity;
import NG.GameState.GameMap;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shapes.Primitives.Collision;
import NG.Rendering.Shapes.ShapesGeneric;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;

/**
 * @author Geert van Ieperen created on 23-12-2018.
 */
public class NetworkNodePoint implements Entity {
    private static final float NODE_SIZE = 10;
    private final GameMap heightMap;
    private NetworkNode reference;
    private final Vector2fc position;

    public NetworkNodePoint(Vector2fc coordinate, GameMap map) {
        this.heightMap = map;
        this.position = coordinate;
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
            gl.scale(NODE_SIZE, NODE_SIZE, 0.001f);
            gl.render(ShapesGeneric.CUBE);
            gl.popMatrix();
        }
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

    @Override
    public Collision getRayCollision(Vector3f origin, Vector3f direction) {
        return new Collision(heightMap.getPosition(position));
    }
}
