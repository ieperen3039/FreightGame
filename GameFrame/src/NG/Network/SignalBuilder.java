package NG.Network;

import NG.Core.Game;
import NG.Entities.Entity;
import NG.GUIMenu.Components.SToggleButton;
import NG.InputHandling.MouseTools.ToggleMouseTool;
import NG.Rendering.MatrixStack.SGL;
import NG.Tracks.RailTools;
import NG.Tracks.TrackPiece;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 24-5-2020.
 */
public class SignalBuilder extends ToggleMouseTool {

    private Signal ghostSignal;

    public SignalBuilder(Game game, SToggleButton source) {
        super(game, () -> source.setActive(false));
    }

    @Override
    public void apply(Entity entity, Vector3fc origin, Vector3fc direction) {
        switch (getMouseAction()) {
            case PRESS_ACTIVATE:
                if (entity instanceof TrackPiece) {
                    TrackPiece trackPiece = (TrackPiece) entity;
                    assert trackPiece.isValid();

                    float fraction = getFraction(trackPiece, origin, direction);
                    RailNode targetNode = getIfExisting(game, trackPiece, fraction);

                    if (targetNode == null) {
                        double gameTime = game.timer().getGameTime();
                        targetNode = RailTools.createSplit(game, trackPiece, fraction, gameTime);
                    }

                    targetNode.addSignal(game);
                }
                break;

            case HOVER:
                if (entity instanceof TrackPiece) {
                    TrackPiece trackPiece = (TrackPiece) entity;

                    float fraction = getFraction(trackPiece, origin, direction);
                    Vector3f closestPoint = trackPiece.getPositionFromFraction(fraction);

                    RailNode ghostNodeTarget;
                    if (game.keyControl().isControlPressed() || trackPiece.isStatic()) {
                        if (fraction < 0.5f) {
                            ghostNodeTarget = trackPiece.getStartNode();
                        } else {
                            ghostNodeTarget = trackPiece.getEndNode();
                        }

                    } else {
                        Vector3f dir = trackPiece.getDirectionFromFraction(fraction);
                        ghostNodeTarget = new RailNode(closestPoint, null, dir, null);
                    }

                    if (ghostSignal == null || ghostSignal.getNode() != ghostNodeTarget) {
                        ghostSignal = new Signal(game, ghostNodeTarget, true, true);
                    }

                } else {
                    ghostSignal = null;
                }
        }
    }

    @Override
    public void apply(Vector3fc position, Vector3fc origin, Vector3fc direction) {
        ghostSignal = null;
    }

    @Override
    public void draw(SGL gl) {
        if (ghostSignal != null) {
            ghostSignal.draw(gl);
        }
    }
}
