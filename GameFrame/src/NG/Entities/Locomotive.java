package NG.Entities;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.InputHandling.MouseTools.AbstractMouseTool;
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

/**
 * @author Geert van Ieperen created on 5-5-2020.
 */
public class Locomotive extends AbstractGameObject implements MovingEntity {
    private final Resource<Mesh> mesh;

    private boolean isDisposed = false;
    private final float spawnTime;
    private float despawnTime = Float.POSITIVE_INFINITY;

    private final Deque<RailNode> plan = new ArrayDeque<>();
    private final RailMovement positionEngine;


    public Locomotive(Game game, TrackPiece startPiece, float fraction) {
        super(game);
        this.spawnTime = game.timer().getGametime();
        this.positionEngine = new RailMovement(game, this, spawnTime, startPiece, fraction, true);

        this.mesh = Mesh.createResource(Directory.meshes, "locos", "LittleRedDiesel.ply");
        positionEngine.setAcceleration(0.1f);
    }

    @Override
    public void update() {
        positionEngine.update();

        if (positionEngine.getSpeed() > 5f) {
            positionEngine.setAcceleration(0);
        }
    }

    @Override
    public Vector3fc getPosition(float time) {
        if (spawnTime > time || despawnTime < time) return null;
        return positionEngine.getPosition(time);
    }

    @Override
    public void draw(SGL gl) {
        float renderTime = game.timer().getRendertime();
        if (spawnTime > renderTime || despawnTime < renderTime) return;

        ShaderProgram shader = gl.getShader();

        if (shader instanceof MaterialShader) {
            ((MaterialShader) shader).setMaterial(Material.METAL, Color4f.WHITE);
        }


        gl.pushMatrix();
        {
            Vector3fc position = positionEngine.getPosition(renderTime);
            gl.translate(position);

            Quaternionf rotation = positionEngine.getRotation(renderTime);
            gl.rotate(rotation);

            gl.render(mesh.get(), this);
        }
        gl.popMatrix();
    }

    public RailNode.Direction pickNextTrack(List<RailNode.Direction> options) {
        assert !options.isEmpty();
        RailNode plannedNext = plan.peek();

        for (RailNode.Direction dir : options) {
            if (dir.railNode.equals(plannedNext)) {
                plan.remove();
                return dir;
            }
        }

        int i = Toolbox.random.nextInt(options.size());
        return options.get(i);
    }

    @Override
    public void onClick(int button) {
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
