package io.github.ieperen3039.trainsinspace.Menu.InGame.Build;

import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.Entities.Entity;
import io.github.ieperen3039.trainsinspace.GUIMenu.Components.SToggleButton;
import io.github.ieperen3039.trainsinspace.InputHandling.MouseTool.AbstractMouseTool;
import io.github.ieperen3039.trainsinspace.Network.RailNode;
import io.github.ieperen3039.trainsinspace.Network.SignalEntity;
import io.github.ieperen3039.trainsinspace.Rendering.MatrixStack.SGL;
import io.github.ieperen3039.trainsinspace.Tracks.RailTools;
import io.github.ieperen3039.trainsinspace.Tracks.TrackPiece;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 24-5-2020.
 */
public class SignalBuilder extends AbstractMouseTool {

    protected final Runnable deactivation;
    private SignalEntity ghostSignal;

    public SignalBuilder(Game game, SToggleButton source) {
        super(game);
        this.deactivation = () -> source.setActive(false);
    }

    @Override
    public void apply(Entity entity, Vector3fc origin, Vector3fc direction) {
        switch (getMouseAction()) {
            case PRESS_ACTIVATE:
                if (entity instanceof TrackPiece) {
                    TrackPiece trackPiece = (TrackPiece) entity;
                    assert trackPiece.isValid();

                    float fraction = TrackBuilder.getFraction(trackPiece, origin, direction);
                    RailNode targetNode = TrackBuilder.getIfExisting(game, trackPiece, fraction);

                    if (targetNode == null) {
                        double gameTime = game.timer().getGameTime();
                        targetNode = RailTools.createSplit(game, trackPiece, fraction, gameTime);
                    }

                    targetNode.addSignal(game, true);
                }
                break;

            case HOVER:
                if (entity instanceof TrackPiece) {
                    TrackPiece trackPiece = (TrackPiece) entity;

                    float fraction = TrackBuilder.getFraction(trackPiece, origin, direction);
                    Vector3f closestPoint = trackPiece.getPositionFromFraction(fraction);
                    RailNode ghostNodeTarget = TrackBuilder.getIfExisting(game, trackPiece, fraction);

                    if (ghostNodeTarget == null) {
                        Vector3f dir = trackPiece.getDirectionFromFraction(fraction);
                        ghostNodeTarget = new RailNode(game, closestPoint, trackPiece.getType(), dir);

                    } else {
                        // make it a ghost type
                        ghostNodeTarget = new RailNode(ghostNodeTarget, trackPiece.getType());
                    }

                    if (ghostSignal == null || ghostSignal.getNode() != ghostNodeTarget) {
                        ghostSignal = new SignalEntity(game, ghostNodeTarget, true, true);
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

    @Override
    public void dispose() {
        deactivation.run();
    }
}
