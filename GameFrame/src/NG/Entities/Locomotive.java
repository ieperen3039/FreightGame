package NG.Entities;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.InputHandling.MouseTools.AbstractMouseTool;
import NG.InputHandling.MouseTools.AbstractMouseTool.MouseAction;
import NG.Network.RailNode;
import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Resources.Resource;
import NG.Tools.Directory;
import NG.Tools.Toolbox;
import NG.Tools.Vectors;
import NG.Tracks.RailMovement;
import NG.Tracks.TrackPiece;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * @author Geert van Ieperen created on 5-5-2020.
 */
public class Locomotive extends AbstractGameObject implements MovingEntity {
    private final Resource<Mesh> mesh;

    private boolean isDisposed = false;
    private final double spawnTime;
    private double despawnTime = Float.POSITIVE_INFINITY;

    private final Deque<RailNode> plan = new ArrayDeque<>();
    private final RailMovement positionEngine;


    public Locomotive(Game game, TrackPiece startPiece, float fraction) {
        super(game);
        this.spawnTime = game.timer().getGametime();
        this.positionEngine = new RailMovement(game, this, spawnTime, startPiece, fraction, true, 1);

        this.mesh = Mesh.createResource(Directory.meshes, "locos", "LittleRedDiesel.ply");
    }

    @Override
    public void update() {
        if (positionEngine.getSpeed() > 5f) {
            positionEngine.setAcceleration(0);
        }

        if (positionEngine.getSpeed() < 4f) {
            positionEngine.setAcceleration(2f);
        }

        positionEngine.update();
    }

    @Override
    public Vector3fc getPosition(double time) {
        if (spawnTime > time || despawnTime < time) return null;
        return positionEngine.getPosition(time);
    }

    @Override
    public void draw(SGL gl) {
        double renderTime = game.timer().getRendertime();
        if (spawnTime > renderTime || despawnTime < renderTime) return;

        ShaderProgram shader = gl.getShader();

        if (shader instanceof MaterialShader) {
            ((MaterialShader) shader).setMaterial(Material.METAL);
        }

        gl.pushMatrix();
        {
            Vector3fc position = positionEngine.getPosition(renderTime);
            gl.translate(position);

            Quaternionf rotation = positionEngine.getRotation(renderTime);
            gl.rotate(rotation);

            gl.translate(-0.5f, 0, 0); // TODO set standard for loco mesh
            gl.render(mesh.get(), this);
        }
        gl.popMatrix();
    }

    /**
     * queries the next action that this locomotive is going to take based on the given directions. If the plan is
     * 'null', this method prefers dead ends.
     * @param options the directions it may choose from
     * @return the chosen direction
     */
    public RailNode.Direction pickNextTrack(List<RailNode.Direction> options) {
        assert !options.isEmpty();
        RailNode plannedNext = plan.peek();

        RailNode.Direction result = null;
        float shortest = Float.POSITIVE_INFINITY;

        for (RailNode.Direction dir : options) {
            if (Objects.equals(dir.networkNode, plannedNext)) {
                if (dir.distanceToNetworkNode < shortest) {
                    result = dir;
                }
                break;
            }
        }

        if (result != null) return result;

        int i = Toolbox.random.nextInt(options.size());
        result = options.get(i);

        return result;
    }

    @Override
    public void reactMouse(MouseAction action) {
        if (action == MouseAction.PRESS_ACTIVATE) {
            positionEngine.reverse(1f);
        }
    }

    @Override
    public void dispose() {
        isDisposed = true;
        despawnTime = game.timer().getGametime();
    }

    @Override
    public boolean isDisposed() {
        return isDisposed;
    }


    public static class Placer extends AbstractMouseTool {
        public Placer(Game game) {
            super(game);
        }

        @Override
        public void apply(Entity entity, int xSc, int ySc) {
            if (getMouseAction() == MouseAction.PRESS_ACTIVATE) {
                if (entity instanceof TrackPiece) {
                    TrackPiece trackPiece = (TrackPiece) entity;
                    Vector3f origin = new Vector3f();
                    Vector3f direction = new Vector3f();
                    Vectors.windowCoordToRay(game, xSc, ySc, origin, direction);

                    float fraction = trackPiece.getFractionOfClosest(origin, direction);

                    Locomotive locomotive = new Locomotive(game, trackPiece, fraction);
                    game.state().addEntity(locomotive);
                    game.inputHandling().setMouseTool(null);
                }

            } else if (getMouseAction() == MouseAction.PRESS_DEACTIVATE) {
                game.inputHandling().setMouseTool(null);
            }
        }

        @Override
        public void apply(Vector3fc position, int xSc, int ySc) {
            if (getMouseAction() == MouseAction.PRESS_DEACTIVATE) {
                game.inputHandling().setMouseTool(null);
            }
        }
    }
}
