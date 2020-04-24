package NG.Tracks;

import NG.Core.Game;
import NG.Entities.Entity;
import NG.GUIMenu.Components.SToggleButton;
import NG.InputHandling.MouseTools.SurfaceBuildTool;
import NG.Rendering.Shapes.Primitives.Collision;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

/**
 * @author Geert van Ieperen created on 16-12-2018.
 */
public class TrackBuilder extends SurfaceBuildTool {
    private final TrackType type;
    private NetworkNode firstNode;

    /**
     * this mousetool lets the player place a track by clicking on the map
     * @param game   the game instance
     * @param type   the type of track to place
     * @param source
     */
    public TrackBuilder(Game game, TrackType type, SToggleButton source) {
        super(game, () -> source.setActive(false));
        this.type = type;
    }

    @Override
    public void apply(Vector2fc newPosition) {
        if (getButton() == GLFW_MOUSE_BUTTON_RIGHT) {
            dispose();
            return;
        }

        Logger.DEBUG.print("Clicked on position " + Vectors.toString(newPosition));
        if (firstNode != null) {
            Logger.DEBUG.print("Placing track from " + Vectors.toString(firstNode.getPosition()) +
                    " to " + Vectors.toString(newPosition));

            firstNode = NetworkNode.connectToNew(game, firstNode, newPosition);
        }
    }

    @Override
    public void apply(Entity entity, int xSc, int ySc) {
        if (getButton() == GLFW_MOUSE_BUTTON_RIGHT) {
            dispose();
            return;
        }

        Logger.DEBUG.print("Clicked on entity " + entity);
        if (entity instanceof TrackPiece) {
            Vector3f origin = new Vector3f();
            Vector3f direction = new Vector3f();
            Vectors.windowCoordToRay(game, xSc, ySc, origin, direction);

            Collision rayCollision = entity.getRayCollision(origin, direction);
            Vector3fc hitPosition = rayCollision.hitPosition();
            Vector2f flatRay = new Vector2f(hitPosition.x(), hitPosition.y());

            TrackPiece targetTrack = (TrackPiece) entity;

            if (firstNode == null) {
                NetworkNodePoint aPoint = targetTrack.getStartNodePoint();
                NetworkNodePoint bPoint = targetTrack.getEndNodePoint();
                Vector2f closestPoint = targetTrack.closestPointOf(flatRay);
                NetworkNodePoint newPoint = new NetworkNodePoint(closestPoint, game.map());

                firstNode = NetworkNode.split(game, newPoint, aPoint.getNode(), bPoint.getNode());
            } else {
                Logger.ERROR.print("Not implemented yet :(");
            }

        } else if (entity instanceof NetworkNodePoint) {
            if (firstNode == null) {
                NetworkNodePoint nodePoint = (NetworkNodePoint) entity;
                firstNode = nodePoint.getNode();

            } else {
                Logger.ERROR.print("Not implemented yet :(");
            }
        }
    }

    @Override
    public void mouseMoved(int xDelta, int yDelta) {
        super.mouseMoved(xDelta, yDelta);
        // TODO implement multiple build-possibilities
    }

    @Override
    public String toString() {
        return "Track Builder (" + type + ")";
    }

}
