package io.github.ieperen3039.trainsinspace.Menu.InGame.Build;

import io.github.ieperen3039.ngn.AssetHandling.Asset;
import io.github.ieperen3039.ngn.AssetHandling.GeneratorAsset;
import io.github.ieperen3039.ngn.DataStructures.Generic.Color4f;
import io.github.ieperen3039.ngn.InputHandling.MouseTools.MouseTarget;
import io.github.ieperen3039.ngn.InputHandling.MouseTools.MouseTool;
import io.github.ieperen3039.ngn.Rendering.Material;
import io.github.ieperen3039.ngn.Rendering.MatrixStack.SGL;
import io.github.ieperen3039.ngn.Rendering.MeshLoading.Mesh;
import io.github.ieperen3039.ngn.Rendering.Shaders.MaterialShader;
import io.github.ieperen3039.ngn.Tools.TickTime;
import io.github.ieperen3039.ngn.Tools.Vectors;
import io.github.ieperen3039.ngn.UserInterface.Components.*;
import io.github.ieperen3039.ngn.UserInterface.LayoutManagers.GridLayoutManager;
import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.Entities.GameEntity;
import io.github.ieperen3039.trainsinspace.Entities.Shapes;
import io.github.ieperen3039.trainsinspace.Entities.StationGhost;
import io.github.ieperen3039.trainsinspace.GameMap.GameMap;
import io.github.ieperen3039.trainsinspace.InputHandling.InputMapping;
import io.github.ieperen3039.trainsinspace.InputHandling.MouseTool.MouseTargetEntity;
import io.github.ieperen3039.trainsinspace.InputHandling.MouseTool.MouseTargetMap;
import io.github.ieperen3039.trainsinspace.InputHandling.MouseTool.MouseToolVisuals;
import io.github.ieperen3039.trainsinspace.Menu.Main.MainMenu;
import io.github.ieperen3039.trainsinspace.Tools.Rays;
import io.github.ieperen3039.trainsinspace.Tracks.TrackType;
import org.joml.*;
import org.joml.Math;
import org.joml.primitives.Rayf;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static io.github.ieperen3039.trainsinspace.Entities.StationImpl.PLATFORM_SIZE;

/**
 * @author Geert van Ieperen created on 30-4-2020.
 */
public class StationBuilder implements MouseTool {
    private static final float EPSILON = 1 / 128f;
    private static final int RING_RADIAL_PARTS = 64;
    private static final float RING_THICKNESS = 0.1f;

    private static final Map<Float, Asset<Mesh>> meshes = new HashMap<>();

    private static final String[] numbers = new String[12];

    static {
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = String.valueOf(i);
        }
    }

    protected final Runnable deactivation;
    private final Game game;
    private final TrackType trackType;
    private StationGhost station;
    private SizeSelector selector;
    private boolean isPositioned = false;
    private Vector3f cursorPosition = new Vector3f();
    private boolean cursorIsOnMap = false;
    private Vector3fc placementPos;

    public StationBuilder(Game game, SToggleButton source, TrackType trackType) {
        this.game = game;
        this.trackType = trackType;
        this.deactivation = () -> source.setActive(false);
        this.selector = null;
    }

    @Override
    public void onActivate() {
        this.station = new StationGhost(game, 1, 6);
        this.selector = new SizeSelector();
        // TODO move this out of the MouseTool
        game.gui().addFrame(selector);
        game.state().addEntity(new Visuals());
    }

    @Override
    public void onDeactivate() {
        station = null;
        selector.dispose();
        deactivation.run();
    }

    @Override
    public HandleResult onPress(int button, MouseTarget target, int xSc, int ySc) {
        if (button == InputMapping.DeactivateMouseTool.getButton()) {
            return HandleResult.DEACTIVATE;
        }

        if (button != InputMapping.UseMouseTool.getButton()) {
            return HandleResult.CONTINUE;
        }

        if (target instanceof MouseTargetEntity) {
            cursorIsOnMap = false;
            return HandleResult.CONTINUE;

        } else if (target instanceof MouseTargetMap map) {
            placementPos = map.position();
            station.setPosition(map.position());
            cursorPosition.set(map.position());
            isPositioned = true;

            return HandleResult.CAPTURE;
        }

        return HandleResult.CONTINUE;
    }

    @Override
    public void onDrag(int xDelta, int yDelta, float xSc, float ySc) {
        if (isPositioned) {
            Rayf ray = game.camera().unproject((int) xSc, (int) ySc, game.getViewPort());
            // get relative direction that the player points to
            /* The following is an optimized version of:
             * Planef plane = new Planef(placementPos, Vectors.Z);
             * float f = Intersectionf.intersectRayPlane(ray, plane, EPSILON);
             */
            float f = Intersectionf.intersectRayPlane(
                    ray.oX, ray.oY, ray.oZ,
                    ray.dX, ray.dY, ray.dZ,
                    0, 0, 1,
                    -placementPos.z(), EPSILON
            );
            if (f == -1.0f) return;

            Vector3f point = Rays.toPosition(ray, f);
            Vector2f direction2D = new Vector2f(point.x - placementPos.x(), point.y - placementPos.y());
            station.setOrientation(Vectors.arcTan(direction2D));

            // get the minimum elevation of the station
            GameMap map = game.map();
            Vector3f newPos = new Vector3f(placementPos);
            station.forEachCorner(pos -> {
                float height = map.getHeightAt(pos.x(), pos.y());
                if (newPos.z < height) newPos.z = height;
            });
            station.setPosition(newPos);
        }
    }

    @Override
    public HandleResult onHover(MouseTarget target, int xDelta, int yDelta, float xSc, float ySc) {
        if (target instanceof MouseTargetMap map) {
            if (!isPositioned) {
                cursorPosition.set(map.position());
            }
            cursorIsOnMap = true;
        } else {
            cursorIsOnMap = false;
        }
        return HandleResult.STOP;
    }

    private float hypotenuse(float a, float b) {
        return Math.sqrt(a * a + b * b);
    }

    protected Mesh getRingMesh(float radius) {
        Asset<Mesh> meshAsset = meshes.computeIfAbsent(
                radius, (r) -> new GeneratorAsset<>(
                        () -> Shapes.createRing(
                                (r + StationBuilder.RING_THICKNESS), RING_RADIAL_PARTS, RING_THICKNESS),
                        mesh -> game.executeOnRenderThread(mesh::dispose)
                )
        );
        return meshAsset.get();
    }

    @Override
    public HandleResult onRelease(int button, MouseTarget target, int xSc, int ySc) {
        if (!isPositioned) return HandleResult.CONTINUE;
        double gameTime = game.timer().getTime();

        GameEntity newStation = station.solidify(game, trackType, gameTime);
        game.state().addEntity(newStation);
        game.playerStatus().stations.add(station);

        station.despawn(gameTime);
        game.inputHandling().setMouseTool(null);
        return HandleResult.CONTINUE;
    }

    private class Visuals extends MouseToolVisuals {
        public Visuals() {
            super(selector);
        }

        @Override
        public void draw(SGL gl, TickTime renderTime) {
            if (isPositioned) {
                station.draw(gl, renderTime);
            }

            if (cursorIsOnMap) {
                gl.pushMatrix();
                {
                    gl.translate(cursorPosition);

                    MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, Color4f.GREY));
                    float diameter = hypotenuse(station.getLength(), station.getNumberOfPlatforms() * PLATFORM_SIZE);
                    gl.render(getRingMesh(diameter / 2), 0);

                    MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, Color4f.GREEN));
                    gl.render(getRingMesh(game.settings().STATION_RANGE), 0);
                }
                gl.popMatrix();
            }
        }
    }

    private class SizeSelector extends SFrame {
        // TODO remember previous setting
        SizeSelector() {
            super("Station Size");
            GridLayoutManager layout = new GridLayoutManager(2, 2);
            SPanel panel = new SPanel(layout);

            SDropDown platformCapacityChooser = new SDropDown(game.gui(), (int) station.getLength(), numbers);
            SDropDown nrOfPlatormChooser = new SDropDown(game.gui(), station.getNumberOfPlatforms(), numbers);

            Consumer<Integer> changeListener = (i) -> setSize(platformCapacityChooser, nrOfPlatormChooser);

            platformCapacityChooser.addStateChangeListener(changeListener);
            nrOfPlatormChooser.addStateChangeListener(changeListener);

            panel.add(new STextComponent("Wagons per platform", MainMenu.TEXT_PROPERTIES));
            panel.add(platformCapacityChooser);
            panel.add(new STextComponent("Number of platforms", MainMenu.TEXT_PROPERTIES));
            panel.add(nrOfPlatormChooser);

            setMainPanel(panel);
            pack();
        }

        private void setSize(SDropDown platformCapacityChooser, SDropDown nrOfPlatormChooser) {
            station.setSize(
                    nrOfPlatormChooser.getSelectedIndex(), platformCapacityChooser.getSelectedIndex()
            );
        }

        @Override
        public void dispose() {
            if (!isDisposed()) {
                super.dispose();
                StationBuilder.this.onDeactivate();
            }
        }
    }
}
